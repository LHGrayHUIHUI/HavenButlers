package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 密钥管理器配置属性
 * 提供密钥管理的相关配置选项
 *
 * @author HavenButler
 */
@Data
@ConfigurationProperties(prefix = "base-model.security.key-manager")
public class KeyManagerProperties {

    /**
     * 是否启用密钥管理器
     */
    private boolean enabled = true;

    /**
     * 密钥缓存配置
     */
    private Cache cache = new Cache();

    /**
     * 密钥轮换配置
     */
    private Rotation rotation = new Rotation();

    /**
     * 默认密钥配置
     */
    private Defaults defaults = new Defaults();

    @Data
    public static class Cache {
        /**
         * 是否启用密钥缓存
         */
        private boolean enabled = true;

        /**
         * 缓存过期时间（分钟）
         */
        private int ttlMinutes = 5;

        /**
         * 最大缓存条目数
         */
        private int maxSize = 1000;
    }

    @Data
    public static class Rotation {
        /**
         * 是否启用密钥轮换
         */
        private boolean enabled = false;

        /**
         * 默认密钥过期天数
         */
        private int defaultExpirationDays = 90;

        /**
         * 密钥过期预警天数
         */
        private int warningDays = 7;

        /**
         * 自动轮换密钥（需要配合外部调度器）
         */
        private boolean autoRotate = false;

        /**
         * 轮换检查间隔（小时）
         */
        private int checkIntervalHours = 24;
    }

    @Data
    public static class Defaults {
        /**
         * 默认AES密钥长度
         */
        private int aesKeySize = 256;

        /**
         * 默认RSA密钥长度
         */
        private int rsaKeySize = 2048;

        /**
         * 默认HMAC密钥长度
         */
        private int hmacKeySize = 256;

        /**
         * 密钥强度要求
         */
        private Strength strength = new Strength();
    }

    @Data
    public static class Strength {
        /**
         * 最小密钥长度
         */
        private int minLength = 16;

        /**
         * 要求包含小写字母
         */
        private boolean requireLowercase = true;

        /**
         * 要求包含大写字母
         */
        private boolean requireUppercase = true;

        /**
         * 要求数字
         */
        private boolean requireDigits = true;

        /**
         * 要求特殊字符
         */
        private boolean requireSpecialChars = false;

        /**
         * 最小强度评分
         */
        private int minScore = 60;
    }
}