package com.haven.base.annotation;

import java.lang.annotation.*;

/**
 * 加密注解
 * 用于标记需要加密的字段或参数
 *
 * @author HavenButler
 */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Encrypt {

    /**
     * 加密算法类型
     */
    Algorithm algorithm() default Algorithm.AES;

    /**
     * 是否为敏感信息（用于日志脱敏）
     */
    boolean sensitive() default true;

    /**
     * 加密类型
     */
    Type type() default Type.FIELD;

    /**
     * 需要加密的字段名（仅在FIELD类型时生效）
     */
    String[] fields() default {};

    /**
     * 是否脱敏处理（替代加密）
     */
    boolean mask() default false;

    /**
     * 加密密钥（Base64或普通字符串）
     */
    String key() default "";

    /**
     * 加密失败时是否抛出异常
     * true: 抛出异常
     * false: 返回原始数据或脱敏数据
     */
    boolean failOnError() default false;

    /**
     * 加密类型枚举
     */
    enum Type {
        /**
         * 全量加密：整个响应体加密
         */
        FULL,

        /**
         * 字段级加密：对特定字段进行加密或脱敏
         */
        FIELD
    }

    /**
     * 加密算法枚举
     */
    enum Algorithm {
        /**
         * AES加密
         */
        AES,

        /**
         * RSA加密
         */
        RSA,

        /**
         * MD5（不可逆）
         */
        MD5,

        /**
         * SHA256（不可逆）
         */
        SHA256,

        /**
         * BCrypt（密码专用）
         */
        BCRYPT
    }
}