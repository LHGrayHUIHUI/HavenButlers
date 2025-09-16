package com.haven.base.annotation;

import java.lang.annotation.*;

/**
 * 日志追踪注解
 * 用于标记需要记录日志的方法
 *
 * @author HavenButler
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TraceLog {

    /**
     * 操作描述
     */
    String value() default "";

    /**
     * 操作模块
     */
    String module() default "";

    /**
     * 操作类型（CREATE/UPDATE/DELETE/QUERY/OTHER）
     */
    String type() default "OTHER";

    /**
     * 是否记录参数
     */
    boolean logParams() default true;

    /**
     * 是否记录结果
     */
    boolean logResult() default true;

    /**
     * 是否记录执行时间
     */
    boolean logTime() default true;

    /**
     * 忽略的参数索引（用于敏感信息）
     */
    int[] ignoreParamIndexes() default {};
}