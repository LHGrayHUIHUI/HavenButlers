package com.haven.common.metrics;

import com.haven.common.aspect.RateLimitAspect;
import com.haven.common.mq.MessageSender;
import com.haven.common.redis.DistributedLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Common模块指标收集器 - 基于base-model规范
 * 统一收集分布式锁、限流、消息队列等指标
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model指标规范
 */
@Slf4j
@Component
@Endpoint(id = "common-metrics")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "base-model.metrics", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CommonMetricsCollector {

    private final DistributedLock distributedLock;
    private final RateLimitAspect rateLimitAspect;
    private final MessageSender messageSender;

    /**
     * 读取所有Common模块指标
     */
    @ReadOperation
    public Map<String, Object> collectMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        try {
            // 基础信息
            metrics.put("module", "haven-common");
            metrics.put("version", "2.0.0");
            metrics.put("collectTime", LocalDateTime.now().toString());

            // 分布式锁指标
            Map<String, Long> lockMetrics = distributedLock.getMetrics();
            metrics.put("distributedLock", lockMetrics);

            // 限流指标
            Map<String, Long> rateLimitMetrics = rateLimitAspect.getRateLimitMetrics();
            metrics.put("rateLimit", rateLimitMetrics);

            // 消息队列指标
            Map<String, Long> messageMetrics = messageSender.getMessageMetrics();
            metrics.put("messaging", messageMetrics);

            // 计算综合指标
            Map<String, Object> summary = calculateSummaryMetrics(lockMetrics, rateLimitMetrics, messageMetrics);
            metrics.put("summary", summary);

        } catch (Exception e) {
            log.error("收集Common模块指标失败", e);
            metrics.put("error", e.getMessage());
            metrics.put("errorClass", e.getClass().getSimpleName());
        }

        return metrics;
    }

    /**
     * 计算综合指标
     */
    private Map<String, Object> calculateSummaryMetrics(Map<String, Long> lockMetrics,
                                                       Map<String, Long> rateLimitMetrics,
                                                       Map<String, Long> messageMetrics) {
        Map<String, Object> summary = new HashMap<>();

        // 总体健康状态
        boolean isHealthy = true;

        // 分布式锁健康状态
        long lockFailed = lockMetrics.getOrDefault("lockFailed", 0L);
        long lockTotal = lockMetrics.getOrDefault("lockAcquired", 0L) + lockFailed;
        double lockSuccessRate = lockTotal > 0 ? (double) lockMetrics.getOrDefault("lockAcquired", 0L) / lockTotal * 100 : 100;

        if (lockSuccessRate < 95) {
            isHealthy = false;
        }

        // 限流健康状态
        long rateLimitBlocked = rateLimitMetrics.getOrDefault("rateLimitBlocked", 0L);
        long rateLimitTotal = rateLimitMetrics.getOrDefault("rateLimitHits", 0L);
        double rateLimitBlockRate = rateLimitTotal > 0 ? (double) rateLimitBlocked / rateLimitTotal * 100 : 0;

        if (rateLimitBlockRate > 50) {
            isHealthy = false;
        }

        // 消息队列健康状态
        long messageSuccessRate = messageMetrics.getOrDefault("successRate", 100L);
        if (messageSuccessRate < 95) {
            isHealthy = false;
        }

        summary.put("overallHealth", isHealthy ? "HEALTHY" : "UNHEALTHY");
        summary.put("lockSuccessRate", String.format("%.2f%%", lockSuccessRate));
        summary.put("rateLimitBlockRate", String.format("%.2f%%", rateLimitBlockRate));
        summary.put("messageSuccessRate", messageSuccessRate + "%");

        // 性能指标
        summary.put("lockRenewed", lockMetrics.getOrDefault("lockRenewed", 0L));
        summary.put("rateLimitPassed", rateLimitMetrics.getOrDefault("rateLimitPassed", 0L));
        summary.put("messagesDelayed", messageMetrics.getOrDefault("messagesDelayed", 0L));

        return summary;
    }

    /**
     * 重置所有指标
     */
    public void resetAllMetrics() {
        try {
            rateLimitAspect.resetMetrics();
            messageSender.resetMessageMetrics();
            log.info("Common模块所有指标已重置");
        } catch (Exception e) {
            log.error("重置指标失败", e);
        }
    }
}