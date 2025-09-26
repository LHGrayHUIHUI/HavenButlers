package com.haven.base.annotation;

import java.lang.annotation.*;

/**
 * 权限校验注解
 * 用于标记需要权限校验的方法
 *
 * @author HavenButler
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Permission {

    /**
     * 需要的权限值
     */
    String[] value() default {};

    /**
     * 需要的角色
     */
    String[] roles() default {};

    /**
     * 权限逻辑（AND/OR）
     */
    Logic logic() default Logic.AND;

    /**
     * 是否需要登录
     */
    boolean requireLogin() default true;

    /**
     * 是否需要家庭成员身份
     */
    boolean requireFamily() default false;

    /**
     * 权限不足时的提示信息
     */
    String message() default "您没有权限执行此操作";

    /**
     * 严格模式：权限系统异常时是否拒绝访问
     * true: 权限系统异常时拒绝访问（推荐生产环境）
     * false: 权限系统异常时允许访问并记录警告
     */
    boolean strictMode() default false;

    /**
     * 权限逻辑枚举
     */
    enum Logic {
        /**
         * 需要所有权限
         */
        AND,

        /**
         * 需要任一权限
         */
        OR
    }
}