package com.haven.base.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 用于标记需要限流的接口
 *
 * @author HavenButler
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {

    /**
     * 限流键（为空时使用方法名）
     */
    String key() default "";

    /**
     * 时间窗口（秒）
     */
    int window() default 60;

    /**
     * 限流次数
     */
    int limit() default 100;

    /**
     * 限流类型（GLOBAL/IP/USER）
     */
    LimitType type() default LimitType.GLOBAL;

    /**
     * 限流提示信息
     */
    String message() default "请求过于频繁，请稍后再试";

    /**
     * 限流类型枚举
     */
    enum LimitType {
        /**
         * 全局限流
         */
        GLOBAL,

        /**
         * 基于IP限流
         */
        IP,

        /**
         * 基于用户限流
         */
        USER,

        /**
         * 基于IP+用户组合限流
         */
        IP_USER
    }
}