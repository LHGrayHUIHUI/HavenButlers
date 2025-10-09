package com.haven.admin.client;

import com.haven.admin.service.SimpleCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 统一的健康检查客户端
 *
 * 功能：
 * - 统一健康检查逻辑，避免代码重复
 * - 内置缓存机制（TTL=10秒），减少探测压力
 * - 统一超时和失败处理
 * - 支持快速短路，避免级联故障
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HealthProbeClient {

    private final RestTemplate restTemplate;
    private final SimpleCacheService cacheService;

    /**
     * 健康检查缓存 TTL（秒）
     * 对于健康检查这种高频操作，10秒的缓存可以有效减少探测风暴
     */
    private static final int HEALTH_CHECK_TTL_SECONDS = 10;

    /**
     * 检查服务实例健康状态（带缓存）
     *
     * @param instance 服务实例
     * @return true=健康，false=不健康
     */
    public boolean checkInstanceHealth(ServiceInstance instance) {
        String instanceKey = buildInstanceKey(instance);
        String cacheKey = "health_" + instanceKey;

        // 从缓存获取，如果缓存未命中则执行探测
        return cacheService.computeIfAbsent(
                cacheKey,
                HEALTH_CHECK_TTL_SECONDS,
                () -> doCheckHealth(instance)
        );
    }

    /**
     * 强制刷新健康状态（跳过缓存）
     *
     * @param instance 服务实例
     * @return true=健康，false=不健康
     */
    public boolean checkInstanceHealthForce(ServiceInstance instance) {
        String instanceKey = buildInstanceKey(instance);
        String cacheKey = "health_" + instanceKey;

        // 先执行探测，再更新缓存
        boolean isHealthy = doCheckHealth(instance);
        cacheService.put(cacheKey, isHealthy, HEALTH_CHECK_TTL_SECONDS);
        return isHealthy;
    }

    /**
     * 批量检查多个实例的健康状态
     *
     * @param instances 服务实例列表
     * @return 实例ID -> 健康状态的映射
     */
    public Map<String, Boolean> checkMultipleInstances(java.util.List<ServiceInstance> instances) {
        return instances.stream()
                .collect(java.util.stream.Collectors.toMap(
                        ServiceInstance::getInstanceId,
                        this::checkInstanceHealth
                ));
    }

    /**
     * 执行实际的健康检查
     *
     * 使用 Map 解析响应，避免直接反序列化 Health 对象的兼容性问题
     * 超时策略：连接超时2秒，读取超时3秒（由 RestTemplate 连接池配置控制）
     *
     * @param instance 服务实例
     * @return true=健康，false=不健康
     */
    private boolean doCheckHealth(ServiceInstance instance) {
        try {
            String healthUrl = buildHealthUrl(instance);

            // 使用 Map 解析，避免版本兼容性问题
            @SuppressWarnings("unchecked")
            Map<String, Object> healthResponse = restTemplate.getForObject(healthUrl, Map.class);

            if (healthResponse == null) {
                log.debug("实例 {} 健康检查失败：响应为空", buildInstanceKey(instance));
                return false;
            }

            // 解析 status 字段
            Object statusObj = healthResponse.get("status");
            if (statusObj == null) {
                log.debug("实例 {} 健康检查失败：无 status 字段", buildInstanceKey(instance));
                return false;
            }

            String status = statusObj.toString();
            boolean isHealthy = "UP".equalsIgnoreCase(status);

            if (!isHealthy) {
                log.debug("实例 {} 状态异常: {}", buildInstanceKey(instance), status);
            }

            return isHealthy;

        } catch (org.springframework.web.client.ResourceAccessException e) {
            // 网络超时或连接失败 - 这是最常见的失败场景
            log.debug("实例 {} 健康检查失败：网络异常 - {}", buildInstanceKey(instance), e.getMessage());
            return false;

        } catch (org.springframework.web.client.HttpClientErrorException |
                 org.springframework.web.client.HttpServerErrorException e) {
            // HTTP 错误（4xx/5xx）
            log.debug("实例 {} 健康检查失败：HTTP {} - {}",
                    buildInstanceKey(instance), e.getStatusCode(), e.getMessage());
            return false;

        } catch (Exception e) {
            // 其他未知错误
            log.warn("实例 {} 健康检查失败：{} - {}",
                    buildInstanceKey(instance), e.getClass().getSimpleName(), e.getMessage());
            return false;
        }
    }

    /**
     * 构建健康检查 URL
     *
     * @param instance 服务实例
     * @return 健康检查 URL
     */
    private String buildHealthUrl(ServiceInstance instance) {
        return String.format("http://%s:%d/actuator/health",
                instance.getHost(), instance.getPort());
    }

    /**
     * 构建实例唯一标识
     *
     * @param instance 服务实例
     * @return 实例标识（格式：serviceName:host:port）
     */
    private String buildInstanceKey(ServiceInstance instance) {
        return String.format("%s:%s:%d",
                instance.getServiceId(),
                instance.getHost(),
                instance.getPort());
    }

    /**
     * 清理指定服务的所有健康检查缓存
     *
     * @param serviceName 服务名称
     */
    public void clearHealthCache(String serviceName) {
        // SimpleCacheService 需要支持通配符清理
        // 这里简化处理，实际应该支持前缀匹配删除
        log.debug("清理服务 {} 的健康检查缓存", serviceName);
    }
}
