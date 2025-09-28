package com.haven.admin.service;

import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
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
import com.alibaba.nacos.api.config.ConfigService;
import org.springframework.beans.factory.annotation.Value;

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
    private final ConfigService configService;

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
     * 改为使用Map解析，避免直接反序列化Health对象的兼容性风险
     */
    private boolean checkInstanceHealth(ServiceInstance instance) {
        try {
            String healthUrl = String.format("http://%s:%d/actuator/health",
                    instance.getHost(), instance.getPort());

            // 使用Map解析，避免直接反序列化Health对象
            @SuppressWarnings("unchecked")
            Map<String, Object> healthResponse = restTemplate.getForObject(healthUrl, Map.class);

            if (healthResponse == null) {
                return false;
            }

            String status = (String) healthResponse.get("status");
            return "UP".equals(status);
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
     * 改为对所有实例进行聚合，而不是只取第一个实例
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
        double totalThreadCount = 0.0;
        double totalMemoryUsed = 0.0;
        double totalMemoryMax = 0.0;
        double totalRequestCount = 0.0;
        double totalResponseTime = 0.0;
        double totalErrorRate = 0.0;
        double totalRequestRate = 0.0;
        int validInstanceCount = 0;

        for (ServiceInstance instance : instances) {
            try {
                // CPU使用率（0-1）转百分比
                Double cpuUsage = queryMetricValue(instance, "system.cpu.usage");
                if (cpuUsage != null) {
                    totalCpuUsage += cpuUsage * 100.0;
                }

                // 线程数
                Double threadsLive = queryMetricValue(instance, "jvm.threads.live");
                if (threadsLive != null) {
                    totalThreadCount += threadsLive;
                }

                // 堆内存使用与上限（按 id 聚合）
                double heapUsed = sumJvmMemory(instance, "jvm.memory.used");
                double heapMax = sumJvmMemory(instance, "jvm.memory.max");
                if (heapUsed > 0) {
                    totalMemoryUsed += heapUsed / (1024 * 1024); // 转MB
                }
                if (heapMax > 0) {
                    totalMemoryMax += heapMax / (1024 * 1024); // 转MB
                }

                // HTTP请求统计
                MetricDetail httpMetrics = queryMetricDetail(instance, "http.server.requests");
                double instanceRequestCount = httpMetrics.count;
                double instanceTotalTime = httpMetrics.totalTime; // 秒
                if (instanceRequestCount > 0) {
                    totalRequestCount += instanceRequestCount;
                    totalResponseTime += instanceTotalTime;
                }

                // 错误率（客户端+服务端错误）
                double clientErr = queryMetricCountWithTag(instance, "http.server.requests", "outcome", "CLIENT_ERROR");
                double serverErr = queryMetricCountWithTag(instance, "http.server.requests", "outcome", "SERVER_ERROR");
                double instanceErrorCount = clientErr + serverErr;
                if (instanceRequestCount > 0) {
                    totalErrorRate += (instanceErrorCount / instanceRequestCount);
                }

                // 简单请求速率：按进程启动时间估算
                Double uptime = queryMetricValue(instance, "process.uptime");
                if (uptime != null && uptime > 0 && instanceRequestCount > 0) {
                    totalRequestRate += (instanceRequestCount / uptime);
                }

                validInstanceCount++;
            } catch (Exception e) {
                log.error("获取实例 {} 指标失败: {}", instance.getInstanceId(), e);
            }
        }

        // 设置聚合后的指标
        if (validInstanceCount > 0) {
            metrics.setCpuUsage(totalCpuUsage / validInstanceCount); // 平均CPU使用率
            metrics.setThreadCount(totalThreadCount); // 线程总数
            metrics.setMemoryUsage(totalMemoryUsed); // 总内存使用量
            metrics.setMemoryMax(totalMemoryMax); // 总内存上限
            metrics.setRequestCount(totalRequestCount); // 总请求数

            // 加权平均响应时间
            if (totalRequestCount > 0) {
                metrics.setResponseTime((totalResponseTime / totalRequestCount) * 1000.0); // 转毫秒
            }

            metrics.setErrorRate(totalErrorRate / validInstanceCount); // 平均错误率
            metrics.setRequestRate(totalRequestRate); // 总请求速率
        }

        return metrics;
    }

    /**
     * 调用 /actuator/metrics/{name} 并返回首个测量值（VALUE/COUNT/TOTAL_TIME优先）
     */
    private Double queryMetricValue(ServiceInstance instance, String metricName) {
        try {
            String url = String.format("http://%s:%d/actuator/metrics/%s",
                    instance.getHost(), instance.getPort(), metricName);
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body == null) return null;
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) body.get("measurements");
            if (measurements == null || measurements.isEmpty()) return null;
            for (String stat : List.of("VALUE", "COUNT", "TOTAL_TIME", "MAX")) {
                for (Map<String, Object> m : measurements) {
                    if (stat.equals(String.valueOf(m.get("statistic")))) {
                        Object v = m.get("value");
                        if (v instanceof Number) return ((Number) v).doubleValue();
                    }
                }
            }
            Object v = measurements.get(0).get("value");
            return v instanceof Number ? ((Number) v).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取 http.server.requests 的 COUNT 和 TOTAL_TIME（秒）
     */
    private MetricDetail queryMetricDetail(ServiceInstance instance, String metricName) {
        MetricDetail d = new MetricDetail();
        try {
            String url = String.format("http://%s:%d/actuator/metrics/%s",
                    instance.getHost(), instance.getPort(), metricName);
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body != null) {
                List<Map<String, Object>> measurements = (List<Map<String, Object>>) body.get("measurements");
                if (measurements != null) {
                    for (Map<String, Object> m : measurements) {
                        String stat = String.valueOf(m.get("statistic"));
                        double val = m.get("value") instanceof Number ? ((Number) m.get("value")).doubleValue() : 0.0;
                        if ("COUNT".equals(stat)) d.count = val;
                        if ("TOTAL_TIME".equals(stat)) d.totalTime = val;
                    }
                }
            }
        } catch (Exception ignore) {}
        return d;
    }

    /**
     * 指定单个tag筛选获得 COUNT 值
     */
    private double queryMetricCountWithTag(ServiceInstance instance, String metricName, String tagKey, String tagValue) {
        try {
            String url = String.format("http://%s:%d/actuator/metrics/%s?tag=%s:%s",
                    instance.getHost(), instance.getPort(), metricName, tagKey, tagValue);
            Map<String, Object> body = restTemplate.getForObject(url, Map.class);
            if (body != null) {
                List<Map<String, Object>> measurements = (List<Map<String, Object>>) body.get("measurements");
                if (measurements != null) {
                    for (Map<String, Object> m : measurements) {
                        if ("COUNT".equals(String.valueOf(m.get("statistic")))) {
                            Object v = m.get("value");
                            return v instanceof Number ? ((Number) v).doubleValue() : 0.0;
                        }
                    }
                }
            }
        } catch (Exception ignore) {}
        return 0.0;
    }

    /**
     * 汇总堆内存指标（按所有 id 求和）
     */
    private double sumJvmMemory(ServiceInstance instance, String metricName) {
        try {
            String base = String.format("http://%s:%d/actuator/metrics/%s",
                    instance.getHost(), instance.getPort(), metricName);
            Map<String, Object> body = restTemplate.getForObject(base, Map.class);
            if (body == null) return 0.0;
            List<Map<String, Object>> availableTags = (List<Map<String, Object>>) body.get("availableTags");
            if (availableTags == null) return 0.0;
            List<String> ids = null;
            for (Map<String, Object> tag : availableTags) {
                if ("id".equals(tag.get("tag"))) {
                    ids = (List<String>) tag.get("values");
                    break;
                }
            }
            if (ids == null) return 0.0;
            double sum = 0.0;
            for (String id : ids) {
                String url = base + "?tag=area:heap&tag=id:" + id;
                Map<String, Object> detail = restTemplate.getForObject(url, Map.class);
                if (detail != null) {
                    List<Map<String, Object>> measurements = (List<Map<String, Object>>) detail.get("measurements");
                    if (measurements != null) {
                        for (Map<String, Object> m : measurements) {
                            if ("VALUE".equals(String.valueOf(m.get("statistic")))) {
                                Object v = m.get("value");
                                if (v instanceof Number) sum += ((Number) v).doubleValue();
                            }
                        }
                    }
                }
            }
            return sum;
        } catch (Exception e) {
            return 0.0;
        }
    }

    private static class MetricDetail {
        double count = 0.0;
        double totalTime = 0.0; // seconds
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
