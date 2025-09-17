package com.haven.admin.service;

import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.base.model.dto.PageRequest;
import com.haven.base.model.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 服务管理服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceManageService {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final Map<String, ServiceInfo> serviceCache = new ConcurrentHashMap<>();

    /**
     * 获取所有服务
     */
    public List<ServiceInfo> getAllServices() {
        List<String> services = discoveryClient.getServices();
        return services.stream()
                .map(this::buildServiceInfo)
                .collect(Collectors.toList());
    }

    /**
     * 获取服务详情
     */
    public ServiceInfo getServiceDetail(String serviceName) {
        return serviceCache.computeIfAbsent(serviceName, this::buildServiceInfo);
    }

    /**
     * 构建服务信息
     */
    private ServiceInfo buildServiceInfo(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

        ServiceInfo serviceInfo = new ServiceInfo();
        serviceInfo.setServiceName(serviceName);
        serviceInfo.setInstanceCount(instances.size());
        serviceInfo.setInstances(new ArrayList<>());

        for (ServiceInstance instance : instances) {
            ServiceInfo.Instance inst = new ServiceInfo.Instance();
            inst.setInstanceId(instance.getInstanceId());
            inst.setHost(instance.getHost());
            inst.setPort(instance.getPort());
            inst.setHealthy(checkInstanceHealth(instance));
            inst.setMetadata(instance.getMetadata());
            serviceInfo.getInstances().add(inst);
        }

        int healthyCount = (int) serviceInfo.getInstances().stream()
                .filter(ServiceInfo.Instance::isHealthy)
                .count();
        serviceInfo.setHealthyCount(healthyCount);
        serviceInfo.setStatus(healthyCount > 0 ? "UP" : "DOWN");

        return serviceInfo;
    }

    /**
     * 检查实例健康状态
     */
    private boolean checkInstanceHealth(ServiceInstance instance) {
        try {
            String healthUrl = String.format("http://%s:%d/actuator/health",
                    instance.getHost(), instance.getPort());
            Health health = restTemplate.getForObject(healthUrl, Health.class);
            return health != null && "UP".equals(health.getStatus().getCode());
        } catch (Exception e) {
            log.warn("检查实例健康状态失败: {}", instance.getInstanceId(), e);
            return false;
        }
    }

    /**
     * 获取服务健康状态
     */
    public Map<String, Object> getServiceHealth(String serviceName) {
        ServiceInfo serviceInfo = getServiceDetail(serviceName);
        Map<String, Object> health = new HashMap<>();
        health.put("serviceName", serviceName);
        health.put("status", serviceInfo.getStatus());
        health.put("totalInstances", serviceInfo.getInstanceCount());
        health.put("healthyInstances", serviceInfo.getHealthyCount());
        health.put("instances", serviceInfo.getInstances());
        return health;
    }

    /**
     * 获取服务指标
     */
    public ServiceMetrics getServiceMetrics(String serviceName, LocalDateTime startTime, LocalDateTime endTime) {
        ServiceMetrics metrics = new ServiceMetrics();
        metrics.setServiceName(serviceName);

        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (!instances.isEmpty()) {
            ServiceInstance instance = instances.get(0);
            try {
                String metricsUrl = String.format("http://%s:%d/actuator/metrics",
                        instance.getHost(), instance.getPort());
                Map<String, Object> metricsData = restTemplate.getForObject(metricsUrl, Map.class);

                metrics.setCpuUsage(getMetricValue(metricsData, "system.cpu.usage"));
                metrics.setMemoryUsage(getMetricValue(metricsData, "jvm.memory.used"));
                metrics.setMemoryMax(getMetricValue(metricsData, "jvm.memory.max"));
                metrics.setThreadCount(getMetricValue(metricsData, "jvm.threads.live"));
                metrics.setRequestCount(getMetricValue(metricsData, "http.server.requests.count"));
                metrics.setRequestRate(calculateRequestRate(metricsData));
                metrics.setErrorRate(calculateErrorRate(metricsData));
                metrics.setResponseTime(getMetricValue(metricsData, "http.server.requests.mean"));
            } catch (Exception e) {
                log.error("获取服务指标失败: {}", serviceName, e);
            }
        }

        return metrics;
    }

    /**
     * 获取指标值
     */
    private double getMetricValue(Map<String, Object> metricsData, String metricName) {
        if (metricsData != null && metricsData.containsKey(metricName)) {
            Object value = metricsData.get(metricName);
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            }
        }
        return 0.0;
    }

    /**
     * 计算请求速率
     */
    private double calculateRequestRate(Map<String, Object> metricsData) {
        double totalRequests = getMetricValue(metricsData, "http.server.requests.count");
        double uptime = getMetricValue(metricsData, "process.uptime");
        return uptime > 0 ? totalRequests / uptime : 0;
    }

    /**
     * 计算错误率
     */
    private double calculateErrorRate(Map<String, Object> metricsData) {
        double totalRequests = getMetricValue(metricsData, "http.server.requests.count");
        double errorRequests = getMetricValue(metricsData, "http.server.requests.error.count");
        return totalRequests > 0 ? errorRequests / totalRequests : 0;
    }

    /**
     * 重启服务
     */
    public void restartService(String serviceName) {
        stopService(serviceName);
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        startService(serviceName);
    }

    /**
     * 停止服务
     */
    public void stopService(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        for (ServiceInstance instance : instances) {
            try {
                String shutdownUrl = String.format("http://%s:%d/actuator/shutdown",
                        instance.getHost(), instance.getPort());
                restTemplate.postForObject(shutdownUrl, null, Void.class);
                log.info("停止服务实例: {}", instance.getInstanceId());
            } catch (Exception e) {
                log.error("停止服务实例失败: {}", instance.getInstanceId(), e);
            }
        }
    }

    /**
     * 启动服务
     */
    public void startService(String serviceName) {
        log.info("启动服务: {} (需要通过容器编排工具实现)", serviceName);
    }

    /**
     * 获取服务日志
     */
    public PageResponse<String> getServiceLogs(String serviceName, String level, PageRequest pageRequest) {
        List<String> logs = new ArrayList<>();

        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (!instances.isEmpty()) {
            ServiceInstance instance = instances.get(0);
            try {
                String logsUrl = String.format("http://%s:%d/actuator/logfile",
                        instance.getHost(), instance.getPort());
                String logContent = restTemplate.getForObject(logsUrl, String.class);

                if (logContent != null) {
                    String[] lines = logContent.split("\n");
                    for (String line : lines) {
                        if (line.contains(level)) {
                            logs.add(line);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("获取服务日志失败: {}", serviceName, e);
            }
        }

        int start = (pageRequest.getPage() - 1) * pageRequest.getSize();
        int end = Math.min(start + pageRequest.getSize(), logs.size());
        List<String> pageLogs = logs.subList(start, end);

        return PageResponse.of(pageLogs, (long) logs.size(), pageRequest);
    }

    /**
     * 更新服务配置
     */
    public void updateServiceConfig(String serviceName, Map<String, Object> config) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        for (ServiceInstance instance : instances) {
            try {
                String configUrl = String.format("http://%s:%d/actuator/env",
                        instance.getHost(), instance.getPort());
                restTemplate.postForObject(configUrl, config, Void.class);

                String refreshUrl = String.format("http://%s:%d/actuator/refresh",
                        instance.getHost(), instance.getPort());
                restTemplate.postForObject(refreshUrl, null, Void.class);

                log.info("更新服务配置成功: {}", instance.getInstanceId());
            } catch (Exception e) {
                log.error("更新服务配置失败: {}", instance.getInstanceId(), e);
            }
        }
    }

    /**
     * 获取服务依赖关系
     */
    public Map<String, List<String>> getServiceDependencies() {
        Map<String, List<String>> dependencies = new HashMap<>();

        dependencies.put("gateway-service", Arrays.asList("account-service", "nlp-service", "ai-service"));
        dependencies.put("account-service", Arrays.asList("storage-service", "common"));
        dependencies.put("nlp-service", Arrays.asList("storage-service", "ai-service", "common"));
        dependencies.put("ai-service", Arrays.asList("storage-service", "common"));
        dependencies.put("storage-service", Arrays.asList("common"));
        dependencies.put("message-service", Arrays.asList("storage-service", "common"));

        return dependencies;
    }

    /**
     * 执行健康检查
     */
    public Map<String, String> performHealthCheck() {
        Map<String, String> healthStatus = new HashMap<>();
        List<String> services = discoveryClient.getServices();

        for (String service : services) {
            ServiceInfo serviceInfo = getServiceDetail(service);
            healthStatus.put(service, serviceInfo.getStatus());
        }

        return healthStatus;
    }
}