package com.haven.admin.service;

import com.haven.admin.client.HealthProbeClient;
import com.haven.admin.client.MetricsClient;
import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Value;

/**
 * 服务管理服务
 *
 * 重构说明：
 * - 使用统一的 HealthProbeClient 进行健康检查
 * - 使用统一的 MetricsClient 进行指标采集
 * - 移除重复的健康检查和指标查询逻辑
 *
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceManageService {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final ConfigService configService;
    private final HealthProbeClient healthProbeClient;
    private final MetricsClient metricsClient;

    private final Map<String, ServiceInfo> serviceCache = new ConcurrentHashMap<>();

    @Value("${admin.allowRemoteShutdown:false}")
    private boolean allowRemoteShutdown;

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
     *
     * 重构：委托给统一的 HealthProbeClient，自动享受缓存和错误处理
     */
    private boolean checkInstanceHealth(ServiceInstance instance) {
        return healthProbeClient.checkInstanceHealth(instance);
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
     *
     * 重构：使用统一的 MetricsClient 进行指标采集，自动享受缓存和错误处理
     * 跨实例聚合指标，返回服务级别的总体视图
     */
    public ServiceMetrics getServiceMetrics(String serviceName, LocalDateTime startTime, LocalDateTime endTime) {
        ServiceMetrics metrics = new ServiceMetrics();
        metrics.setServiceName(serviceName);

        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            return metrics;
        }

        // 跨实例聚合指标
        double totalCpuUsage = 0.0;
        double totalMemoryUsed = 0.0;
        double totalMemoryMax = 0.0;
        double totalThreads = 0.0;
        double totalRequestCount = 0.0;
        int validInstanceCount = 0;

        for (ServiceInstance instance : instances) {
            try {
                // 使用统一的 MetricsClient 采集实例指标
                Map<String, Double> instanceMetrics = metricsClient.collectInstanceMetrics(instance);

                // 聚合各项指标
                if (instanceMetrics.containsKey("cpu.usage")) {
                    totalCpuUsage += instanceMetrics.get("cpu.usage");
                }
                if (instanceMetrics.containsKey("memory.used")) {
                    totalMemoryUsed += instanceMetrics.get("memory.used") / (1024 * 1024); // 转 MB
                }
                if (instanceMetrics.containsKey("memory.max")) {
                    totalMemoryMax += instanceMetrics.get("memory.max") / (1024 * 1024); // 转 MB
                }
                if (instanceMetrics.containsKey("threads.count")) {
                    totalThreads += instanceMetrics.get("threads.count");
                }
                if (instanceMetrics.containsKey("http.requests.count")) {
                    totalRequestCount += instanceMetrics.get("http.requests.count");
                }

                validInstanceCount++;
            } catch (Exception e) {
                log.warn("获取实例 {} 指标失败: {}", instance.getInstanceId(), e.getMessage());
            }
        }

        // 设置聚合后的指标
        if (validInstanceCount > 0) {
            metrics.setCpuUsage(totalCpuUsage / validInstanceCount); // 平均 CPU 使用率
            metrics.setMemoryUsage(totalMemoryUsed); // 总内存使用量
            metrics.setMemoryMax(totalMemoryMax); // 总内存上限
            metrics.setThreadCount(totalThreads); // 总线程数
            metrics.setRequestCount(totalRequestCount); // 总请求数

            // 计算其他派生指标
            if (totalMemoryMax > 0) {
                metrics.setErrorRate((totalMemoryUsed / totalMemoryMax) * 100);
            }
        }

        return metrics;
    }

    // ============================================================
    // 注意：旧的指标查询方法（queryMetricValue、queryMetricDetail 等）
    // 已移除，统一使用 MetricsClient 进行指标采集
    // ============================================================

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
        if (!allowRemoteShutdown) {
            log.warn("已阻止远程shutdown：请通过容器编排（docker/k8s）执行停止，或设置 admin.allowRemoteShutdown=true 以启用（不推荐生产）。");
            return;
        }
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
        // 使用Nacos集中发布配置，客户端通过 @RefreshScope 自动刷新
        try {
            String dataId = serviceName + ".yml";
            String group = "DEFAULT_GROUP";
            String yaml = toYaml(config);
            boolean ok = configService.publishConfig(dataId, group, yaml, "yaml");
            if (ok) {
                log.info("已发布Nacos配置: dataId={}, group={}", dataId, group);
            } else {
                log.warn("发布Nacos配置返回false: dataId={}, group={}", dataId, group);
            }
        } catch (Exception e) {
            log.error("通过Nacos更新服务配置失败: {}", serviceName, e);
        }
    }

    /**
     * 简单YAML序列化（仅支持一层KV与基本类型）
     */
    private String toYaml(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> e : map.entrySet()) {
            sb.append(e.getKey()).append(": ");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(String.valueOf(v));
            } else {
                sb.append('"').append(String.valueOf(v).replace("\"", "\\\"")).append('"');
            }
            sb.append('\n');
        }
        return sb.toString();
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
