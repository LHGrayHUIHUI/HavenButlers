package com.haven.base.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 容错保护注解
 * 用于标记需要容错保护的方法
 *
 * @author HavenButler
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Resilient {

    /**
     * 服务名称，用于识别不同的容错策略
     */
    String value() default "";

    /**
     * 是否启用熔断器
     */
    boolean enableCircuitBreaker() default true;

    /**
     * 是否启用重试
     */
    boolean enableRetry() default true;

    /**
     * 是否启用舱壁隔离
     */
    boolean enableBulkhead() default true;

    /**
     * 是否启用限流
     */
    boolean enableRateLimiter() default true;

    /**
     * 是否启用时间限制
     */
    boolean enableTimeLimiter() default false;

    /**
     * 熔断器配置名称
     */
    String circuitBreakerConfig() default "";

    /**
     * 重试配置名称
     */
    String retryConfig() default "";

    /**
     * 舱壁隔离配置名称
     */
    String bulkheadConfig() default "";

    /**
     * 限流配置名称
     */
    String rateLimiterConfig() default "";

    /**
     * 时间限制配置名称
     */
    String timeLimiterConfig() default "";

    /**
     * 是否异步执行
     */
    boolean async() default false;
}