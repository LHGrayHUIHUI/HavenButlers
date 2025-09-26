package com.haven.admin.service;

import com.haven.admin.model.ServiceOverview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 健康状态快照服务
 *
 * 定时汇总各服务实例的健康状态和关键指标，维护内存快照
 * 提供快速的健康状态查询，避免实时探测造成的性能问题
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthSnapshotService {

    private final DiscoveryClient discoveryClient;
    private final RestTemplate restTemplate;
    private final SimpleCacheService cacheService;

    /**
     * 服务健康快照缓存
     * Key: serviceName, Value: ServiceOverview
     * TTL: 30秒
     */
    private final Map<String, ServiceOverview> healthSnapshots = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> snapshotTimestamps = new ConcurrentHashMap<>();

    // 快照过期时间（秒）
    private static final int SNAPSHOT_TTL_SECONDS = 30;

    /**
     * 定时刷新健康快照（每10秒执行一次）
     */
    @Scheduled(fixedDelay = 10000)
    public void refreshHealthSnapshots() {
        try {
            List<String> services = discoveryClient.getServices();
            log.debug("开始刷新健康快照，发现 {} 个服务", services.size());

            for (String serviceName : services) {
                try {
                    refreshServiceSnapshot(serviceName);
                } catch (Exception e) {
                    log.warn("刷新服务 {} 健康快照失败: {}", serviceName, e.getMessage());
                }
            }

            // 清理过期快照
            cleanupExpiredSnapshots();
        } catch (Exception e) {
            log.error("刷新健康快照任务执行失败", e);
        }
    }

    /**
     * 定时清理缓存中的过期条目（每5分钟执行一次）
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void cleanupCache() {
        try {
            cacheService.cleanupExpiredEntries();
            log.debug("定时清理缓存过期条目完成");
        } catch (Exception e) {
            log.warn("定时清理缓存失败", e);
        }
    }

    /**
     * 刷新单个服务的健康快照
     */
    private void refreshServiceSnapshot(String serviceName) {
        List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);
        if (instances.isEmpty()) {
            log.debug("服务 {} 无可用实例", serviceName);
            return;
        }

        ServiceOverview overview = new ServiceOverview();
        overview.setServiceName(serviceName);
        overview.setInstanceCount(instances.size());

        // 统计健康实例和收集指标
        int healthyCount = 0;
        double totalCpuUsage = 0.0;
        double totalMemoryUsage = 0.0;
        double totalRequestRate = 0.0;
        double totalErrorRate = 0.0;
        int validMetricsCount = 0;

        for (ServiceInstance instance : instances) {
            try {
                // 检查实例健康状态
                boolean isHealthy = checkInstanceHealth(instance);
                if (isHealthy) {
                    healthyCount++;
                }

                // 收集实例指标
                InstanceMetrics metrics = collectInstanceMetrics(instance);
                if (metrics != null) {
                    totalCpuUsage += metrics.cpuUsage;
                    totalMemoryUsage += metrics.memoryUsage;
                    totalRequestRate += metrics.requestRate;
                    totalErrorRate += metrics.errorRate;
                    validMetricsCount++;
                }
            } catch (Exception e) {
                log.debug("处理实例 {} 失败: {}", instance.getInstanceId(), e.getMessage());
            }
        }

        // 设置聚合结果
        overview.setHealthyCount(healthyCount);
        overview.setStatus(determineServiceStatus(healthyCount, instances.size()));

        if (validMetricsCount > 0) {
            overview.setCpuUsageAvg(totalCpuUsage / validMetricsCount);
            overview.setMemoryUsageAvg(totalMemoryUsage / validMetricsCount);
            overview.setRequestRate(totalRequestRate); // 求和，不是平均
            overview.setErrorRate(totalErrorRate / validMetricsCount); // 平均错误率
        } else {
            overview.setCpuUsageAvg(0.0);
            overview.setMemoryUsageAvg(0.0);
            overview.setRequestRate(0.0);
            overview.setErrorRate(0.0);
        }

        // 更新快照
        healthSnapshots.put(serviceName, overview);
        snapshotTimestamps.put(serviceName, LocalDateTime.now());

        String cpuStr = String.format("%.1f", overview.getCpuUsageAvg());
        String memStr = String.format("%.1f", overview.getMemoryUsageAvg());
        log.debug("更新服务 {} 快照: 健康实例 {}/{}, CPU: {}%, 内存: {}MB",
                serviceName, healthyCount, instances.size(), cpuStr, memStr);
    }

    /**
     * 检查实例健康状态（使用缓存优化）
     */
    private boolean checkInstanceHealth(ServiceInstance instance) {
        String cacheKey = String.format("health_%s_%s_%d",
                instance.getServiceId(), instance.getHost(), instance.getPort());

        // 尝试从缓存获取结果（缓存15秒，减少重复请求）
        Boolean cachedResult = cacheService.computeIfAbsent(
                cacheKey,
                15,
                () -> performHealthCheck(instance)
        );

        return cachedResult != null ? cachedResult : false;
    }

    /**
     * 执行实际的健康检查
     */
    private Boolean performHealthCheck(ServiceInstance instance) {
        try {
            String healthUrl = String.format("http://%s:%d/actuator/health",
                    instance.getHost(), instance.getPort());

            // 使用 Map 解析，避免直接反序列化 Health 对象
            @SuppressWarnings("unchecked")
            Map<String, Object> healthResponse = restTemplate.getForObject(healthUrl, Map.class);

            if (healthResponse == null) {
                return false;
            }

            String status = (String) healthResponse.get("status");
            return "UP".equals(status);
        } catch (Exception e) {
            log.debug("检查实例 {} 健康状态失败: {}", instance.getInstanceId(), e.getMessage());
            return false;
        }
    }

    /**
     * 收集实例指标（使用缓存优化）
     */
    private InstanceMetrics collectInstanceMetrics(ServiceInstance instance) {
        String cacheKey = String.format("metrics_%s_%s_%d",
                instance.getServiceId(), instance.getHost(), instance.getPort());

        // 缓存20秒，因为指标变化较慢
        return cacheService.computeIfAbsent(
                cacheKey,
                20,
                () -> performMetricsCollection(instance)
        );
    }

    /**
     * 执行实际的指标收集
     */
    private InstanceMetrics performMetricsCollection(ServiceInstance instance) {
        try {
            InstanceMetrics metrics = new InstanceMetrics();

            // CPU 使用率
            Double cpuUsage = queryMetricValue(instance, "system.cpu.usage");
            if (cpuUsage != null) {
                metrics.cpuUsage = cpuUsage * 100.0; // 转换为百分比
            }

            // 内存使用（堆内存）
            Double memoryUsed = queryMetricValue(instance, "jvm.memory.used", "area", "heap");
            if (memoryUsed != null) {
                metrics.memoryUsage = memoryUsed / (1024 * 1024); // 转换为 MB
            }

            // HTTP 请求指标
            collectHttpMetrics(instance, metrics);

            return metrics;
        } catch (Exception e) {
            log.debug("收集实例 {} 指标失败: {}", instance.getInstanceId(), e.getMessage());
            return null;
        }
    }

    /**
     * 收集HTTP请求相关指标
     */
    private void collectHttpMetrics(ServiceInstance instance, InstanceMetrics metrics) {
        try {
            // 总请求数
            Double totalRequests = queryMetricValue(instance, "http.server.requests");
            if (totalRequests != null) {
                // 进程运行时间（秒）
                Double uptime = queryMetricValue(instance, "process.uptime");
                if (uptime != null && uptime > 0) {
                    metrics.requestRate = totalRequests / uptime; // 请求/秒
                }

                // 错误请求数（4xx + 5xx）
                Double clientErrors = queryMetricValueWithTag(instance, "http.server.requests", "status", "4xx");
                Double serverErrors = queryMetricValueWithTag(instance, "http.server.requests", "status", "5xx");
                double totalErrors = (clientErrors != null ? clientErrors : 0.0) +
                                   (serverErrors != null ? serverErrors : 0.0);

                if (totalRequests > 0) {
                    metrics.errorRate = (totalErrors / totalRequests) * 100.0; // 错误率百分比
                }
            }
        } catch (Exception e) {
            log.debug("收集HTTP指标失败: {}", e.getMessage());
        }
    }

    /**
     * 查询指标值
     */
    private Double queryMetricValue(ServiceInstance instance, String metricName) {
        return queryMetricValue(instance, metricName, null, null);
    }

    /**
     * 查询带标签的指标值
     */
    private Double queryMetricValue(ServiceInstance instance, String metricName, String tagKey, String tagValue) {
        try {
            String url = String.format("http://%s:%d/actuator/metrics/%s",
                    instance.getHost(), instance.getPort(), metricName);

            if (tagKey != null && tagValue != null) {
                url += "?tag=" + tagKey + ":" + tagValue;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.get("measurements");
            if (measurements == null || measurements.isEmpty()) {
                return null;
            }

            // 查找 VALUE 或 COUNT 统计值
            for (Map<String, Object> measurement : measurements) {
                String statistic = (String) measurement.get("statistic");
                if ("VALUE".equals(statistic) || "COUNT".equals(statistic)) {
                    Object value = measurement.get("value");
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                }
            }

            // 如果没找到，返回第一个测量值
            Object value = measurements.get(0).get("value");
            return value instanceof Number ? ((Number) value).doubleValue() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 查询带特定标签的指标值
     */
    private Double queryMetricValueWithTag(ServiceInstance instance, String metricName, String tagKey, String tagValue) {
        try {
            String url = String.format("http://%s:%d/actuator/metrics/%s?tag=%s:%s",
                    instance.getHost(), instance.getPort(), metricName, tagKey, tagValue);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> measurements = (List<Map<String, Object>>) response.get("measurements");
            if (measurements == null || measurements.isEmpty()) {
                return null;
            }

            for (Map<String, Object> measurement : measurements) {
                if ("COUNT".equals(measurement.get("statistic"))) {
                    Object value = measurement.get("value");
                    if (value instanceof Number) {
                        return ((Number) value).doubleValue();
                    }
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 确定服务整体状态
     */
    private String determineServiceStatus(int healthyCount, int totalCount) {
        if (healthyCount == 0) {
            return "DOWN";
        } else if (healthyCount < totalCount) {
            return "DEGRADED"; // 部分实例不健康
        } else {
            return "UP";
        }
    }

    /**
     * 清理过期快照
     */
    private void cleanupExpiredSnapshots() {
        LocalDateTime cutoff = LocalDateTime.now().minusSeconds(SNAPSHOT_TTL_SECONDS);

        Iterator<Map.Entry<String, LocalDateTime>> iterator = snapshotTimestamps.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, LocalDateTime> entry = iterator.next();
            if (entry.getValue().isBefore(cutoff)) {
                String serviceName = entry.getKey();
                iterator.remove();
                healthSnapshots.remove(serviceName);
                log.debug("清理过期快照: {}", serviceName);
            }
        }
    }

    /**
     * 获取所有服务健康概览
     */
    public List<ServiceOverview> getAllServiceOverview() {
        return new ArrayList<>(healthSnapshots.values());
    }

    /**
     * 获取指定服务的健康概览
     */
    public ServiceOverview getServiceOverview(String serviceName) {
        return healthSnapshots.get(serviceName);
    }

    /**
     * 检查快照是否新鲜
     */
    public boolean isSnapshotFresh(String serviceName) {
        LocalDateTime timestamp = snapshotTimestamps.get(serviceName);
        if (timestamp == null) {
            return false;
        }
        return timestamp.isAfter(LocalDateTime.now().minusSeconds(SNAPSHOT_TTL_SECONDS));
    }

    /**
     * 实例指标内部类
     */
    private static class InstanceMetrics {
        double cpuUsage = 0.0;      // CPU使用率百分比
        double memoryUsage = 0.0;   // 内存使用量MB
        double requestRate = 0.0;   // 请求速率（req/s）
        double errorRate = 0.0;     // 错误率百分比
    }
}
