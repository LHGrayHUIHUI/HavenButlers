package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Resilience4j 容错配置
 * 支持熔断器、重试、舱壁隔离、限流、超时控制等配置
 *
 * @author HavenButler
 */
@Data
@Component
@ConfigurationProperties(prefix = "base-model.resilience")
public class ResilienceProperties {

    /**
     * 是否启用容错机制
     */
    private boolean enabled = true;

    /**
     * 熔断器配置
     */
    private CircuitBreakerConfig circuitBreaker = new CircuitBreakerConfig();

    /**
     * 重试配置
     */
    private RetryConfig retry = new RetryConfig();

    /**
     * 舱壁隔离配置
     */
    private BulkheadConfig bulkhead = new BulkheadConfig();

    /**
     * 限流配置
     */
    private RateLimiterConfig rateLimiter = new RateLimiterConfig();

    /**
     * 时间限制配置
     */
    private TimeLimiterConfig timeLimiter = new TimeLimiterConfig();

    @Data
    public static class CircuitBreakerConfig {
        /**
         * 是否启用熔断器
         */
        private boolean enabled = true;

        /**
         * 失败率阈值（百分比）
         */
        private float failureRateThreshold = 50f;

        /**
         * 慢调用阈值（毫秒）
         */
        private long slowCallDurationThreshold = 2000;

        /**
         * 慢调用失败率阈值（百分比）
         */
        private float slowCallFailureRateThreshold = 100f;

        /**
         * 熔断器打开后的等待时间（秒）
         */
        private int waitDurationInOpenState = 60;

        /**
         * 半开状态允许的调用次数
         */
        private int permittedNumberOfCallsInHalfOpenState = 10;

        /**
         * 最小调用次数阈值
         */
        private int slidingWindowSize = 100;

        /**
         * 滑动窗口类型（COUNT_BASED 或 TIME_BASED）
         */
        private String slidingWindowType = "COUNT_BASED";

        /**
         * 自动从打开到半开的转换
         */
        private boolean automaticTransitionFromOpenToHalfOpenEnabled = false;
    }

    @Data
    public static class RetryConfig {
        /**
         * 是否启用重试
         */
        private boolean enabled = true;

        /**
         * 最大重试次数
         */
        private int maxAttempts = 3;

        /**
         * 重试间隔（毫秒）
         */
        private long waitDuration = 1000;

        /**
         * 重试间隔策略（FIXED 或 EXPONENTIAL）
         */
        private String intervalFunction = "EXPONENTIAL";

        /**
         * 指数退避乘数
         */
        private double exponentialBackoffMultiplier = 2.0;

        /**
         * 重试间隔随机因子
         */
        private double randomizationFactor = 0.5;

        /**
         * 异常列表（这些异常会触发重试）
         */
        private String[] retryExceptionNames = {
            "java.io.IOException",
            "java.net.SocketTimeoutException",
            "org.springframework.web.client.ResourceAccessException"
        };

        /**
         * 忽略异常列表（这些异常不会触发重试）
         */
        private String[] ignoreExceptionNames = {
            "java.lang.IllegalArgumentException",
            "org.springframework.web.client.HttpClientErrorException"
        };
    }

    @Data
    public static class BulkheadConfig {
        /**
         * 是否启用舱壁隔离
         */
        private boolean enabled = true;

        /**
         * 最大并发调用数
         */
        private int maxConcurrentCalls = 25;

        /**
         * 最大等待时间（毫秒）
         */
        private long maxWaitDuration = 5000;

        /**
         * 舱壁隔离类型（SEMAPHORE 或 THREADPOOL）
         */
        private String bulkheadType = "SEMAPHORE";
    }

    @Data
    public static class RateLimiterConfig {
        /**
         * 是否启用限流
         */
        private boolean enabled = true;

        /**
         * 限流周期（秒）
         */
        private int limitForPeriod = 50;

        /**
         * 限流刷新周期（纳秒）
         */
        private long limitRefreshPeriod = 1000000000L; // 1秒

        /**
         * 超时等待时间（毫秒）
         */
        private long timeoutDuration = 5000;
    }

    @Data
    public static class TimeLimiterConfig {
        /**
         * 是否启用时间限制
         */
        private boolean enabled = true;

        /**
         * 超时时间（秒）
         */
        private long timeoutDuration = 10;

        /**
         * 是否取消运行中的Future
         */
        private boolean cancelRunningFuture = true;
    }
}