package com.haven.admin.service;

import com.haven.admin.client.HealthProbeClient;
import com.haven.admin.client.MetricsClient;
import com.haven.admin.model.ServiceOverview;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

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
 * 重构说明：
 * - 使用统一的 HealthProbeClient 进行健康检查
 * - 使用统一的 MetricsClient 进行指标采集
 * - 移除重复的健康检查和指标查询逻辑
 *
 * @author HavenButler
 * @version 2.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HealthSnapshotService {

    private final DiscoveryClient discoveryClient;
    private final HealthProbeClient healthProbeClient;
    private final MetricsClient metricsClient;

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
     * 定时清理过期快照（每5分钟执行一次）
     */
    @Scheduled(fixedDelay = 300000) // 5分钟
    public void cleanupCache() {
        try {
            cleanupExpiredSnapshots();
            log.debug("定时清理过期快照完成");
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
                // 使用统一的 HealthProbeClient 检查健康状态
                boolean isHealthy = healthProbeClient.checkInstanceHealth(instance);
                if (isHealthy) {
                    healthyCount++;
                }

                // 使用统一的 MetricsClient 收集指标
                Map<String, Double> metrics = metricsClient.collectInstanceMetrics(instance);
                if (metrics != null && !metrics.isEmpty()) {
                    // 提取关键指标
                    totalCpuUsage += metrics.getOrDefault("cpu.usage", 0.0);
                    totalMemoryUsage += metrics.getOrDefault("memory.usage", 0.0);
                    totalRequestRate += metrics.getOrDefault("http.requests.count", 0.0);
                    // 简单的错误率估算（可以根据实际需求调整）
                    totalErrorRate += 0.0; // TODO: 需要从 MetricsClient 获取错误率
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

    // ============================================================
    // 注意：旧的健康检查和指标采集方法已移除
    // 统一使用 HealthProbeClient 和 MetricsClient
    // ============================================================

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
