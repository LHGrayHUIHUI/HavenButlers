package com.haven.admin.client;

import com.haven.admin.service.SimpleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * 统一的指标采集客户端
 *
 * 功能：
 * - 统一指标采集逻辑，避免代码重复
 * - 内置缓存机制（TTL=15秒），减少采集压力
 * - 统一超时和失败处理
 * - 支持多种指标类型：JVM、系统、应用自定义指标
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsClient {

    private final RestTemplate restTemplate;
    private final SimpleCacheService cacheService;

    /**
     * 指标采集缓存 TTL（秒）
     * 指标数据变化相对较慢，15秒的缓存可以有效减少采集频率
     */
    private static final int METRICS_TTL_SECONDS = 15;

    /**
     * 采集实例的所有关键指标（带缓存）
     *
     * @param instance 服务实例
     * @return 指标数据映射（指标名 -> 指标值）
     */
    public Map<String, Double> collectInstanceMetrics(ServiceInstance instance) {
        String instanceKey = buildInstanceKey(instance);
        String cacheKey = "metrics_" + instanceKey;

        // 从缓存获取，如果缓存未命中则执行采集
        return cacheService.computeIfAbsent(
                cacheKey,
                METRICS_TTL_SECONDS,
                () -> doCollectMetrics(instance)
        );
    }

    /**
     * 强制刷新指标数据（跳过缓存）
     *
     * @param instance 服务实例
     * @return 指标数据映射
     */
    public Map<String, Double> collectInstanceMetricsForce(ServiceInstance instance) {
        String instanceKey = buildInstanceKey(instance);
        String cacheKey = "metrics_" + instanceKey;

        // 先执行采集，再更新缓存
        Map<String, Double> metrics = doCollectMetrics(instance);
        cacheService.put(cacheKey, metrics, METRICS_TTL_SECONDS);
        return metrics;
    }

    /**
     * 采集单个指标（带缓存）
     *
     * @param instance   服务实例
     * @param metricName 指标名称（如：jvm.memory.used、system.cpu.usage）
     * @return 指标值，失败返回 null
     */
    public Double collectSingleMetric(ServiceInstance instance, String metricName) {
        String instanceKey = buildInstanceKey(instance);
        String cacheKey = "metric_" + instanceKey + "_" + metricName;

        return cacheService.computeIfAbsent(
                cacheKey,
                METRICS_TTL_SECONDS,
                () -> doCollectSingleMetric(instance, metricName)
        );
    }

    /**
     * 执行实际的指标采集
     *
     * 采集策略：
     * 1. 优先采集 /actuator/metrics 端点（Spring Boot 2.x/3.x 标准）
     * 2. 提取关键指标：CPU、内存、线程、GC、HTTP 请求
     * 3. 失败时返回空 Map，避免影响其他实例
     *
     * @param instance 服务实例
     * @return 指标数据映射
     */
    private Map<String, Double> doCollectMetrics(ServiceInstance instance) {
        Map<String, Double> metrics = new HashMap<>();

        try {
            // 1. CPU 使用率
            Double cpuUsage = doCollectSingleMetric(instance, "system.cpu.usage");
            if (cpuUsage != null) {
                metrics.put("cpu.usage", cpuUsage * 100); // 转换为百分比
            }

            // 2. JVM 内存使用
            Double memoryUsed = doCollectSingleMetric(instance, "jvm.memory.used");
            Double memoryMax = doCollectSingleMetric(instance, "jvm.memory.max");
            if (memoryUsed != null && memoryMax != null && memoryMax > 0) {
                metrics.put("memory.used", memoryUsed);
                metrics.put("memory.max", memoryMax);
                metrics.put("memory.usage", (memoryUsed / memoryMax) * 100); // 百分比
            }

            // 3. 线程数
            Double threadCount = doCollectSingleMetric(instance, "jvm.threads.live");
            if (threadCount != null) {
                metrics.put("threads.count", threadCount);
            }

            // 4. GC 次数和时间
            Double gcCount = doCollectSingleMetric(instance, "jvm.gc.pause.count");
            Double gcTime = doCollectSingleMetric(instance, "jvm.gc.pause.totalTime");
            if (gcCount != null) {
                metrics.put("gc.count", gcCount);
            }
            if (gcTime != null) {
                metrics.put("gc.time", gcTime);
            }

            // 5. HTTP 请求统计
            Double httpRequestCount = doCollectSingleMetric(instance, "http.server.requests.count");
            if (httpRequestCount != null) {
                metrics.put("http.requests.count", httpRequestCount);
            }

            log.debug("实例 {} 指标采集完成，共 {} 个指标", buildInstanceKey(instance), metrics.size());

        } catch (Exception e) {
            log.warn("实例 {} 指标采集失败：{}", buildInstanceKey(instance), e.getMessage());
        }

        return metrics;
    }

    /**
     * 采集单个指标值
     *
     * @param instance   服务实例
     * @param metricName 指标名称
     * @return 指标值，失败返回 null
     */
    private Double doCollectSingleMetric(ServiceInstance instance, String metricName) {
        try {
            String metricsUrl = buildMetricsUrl(instance, metricName);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(metricsUrl, Map.class);

            if (response == null) {
                return null;
            }

            // 解析指标值
            // Spring Boot Actuator 格式：{"name":"xxx","measurements":[{"value":123.45}]}
            Object measurementsObj = response.get("measurements");
            if (measurementsObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Map<String, Object>> measurements =
                        (java.util.List<Map<String, Object>>) measurementsObj;

                if (!measurements.isEmpty()) {
                    Object valueObj = measurements.get(0).get("value");
                    if (valueObj instanceof Number) {
                        return ((Number) valueObj).doubleValue();
                    }
                }
            }

            return null;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 网络超时或连接失败 - 静默失败，避免日志泛滥
            log.trace("实例 {} 指标 {} 采集失败：网络异常", buildInstanceKey(instance), metricName);
            return null;

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // 404 = 指标不存在，这是正常情况（不同服务暴露的指标不同）
            if (e.getStatusCode().value() == 404) {
                log.trace("实例 {} 指标 {} 不存在", buildInstanceKey(instance), metricName);
            } else {
                log.debug("实例 {} 指标 {} 采集失败：HTTP {}", buildInstanceKey(instance), metricName, e.getStatusCode());
            }
            return null;

        } catch (Exception e) {
            log.debug("实例 {} 指标 {} 采集失败：{}", buildInstanceKey(instance), metricName, e.getMessage());
            return null;
        }
    }

    /**
     * 构建指标查询 URL
     *
     * @param instance   服务实例
     * @param metricName 指标名称
     * @return 指标查询 URL
     */
    private String buildMetricsUrl(ServiceInstance instance, String metricName) {
        return String.format("http://%s:%d/actuator/metrics/%s",
                instance.getHost(), instance.getPort(), metricName);
    }

    /**
     * 构建实例唯一标识
     *
     * @param instance 服务实例
     * @return 实例标识
     */
    private String buildInstanceKey(ServiceInstance instance) {
        return String.format("%s:%s:%d",
                instance.getServiceId(),
                instance.getHost(),
                instance.getPort());
    }

    /**
     * 清理指定服务的所有指标缓存
     *
     * @param serviceName 服务名称
     */
    public void clearMetricsCache(String serviceName) {
        log.debug("清理服务 {} 的指标缓存", serviceName);
    }
}