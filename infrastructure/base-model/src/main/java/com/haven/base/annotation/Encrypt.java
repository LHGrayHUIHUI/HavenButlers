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