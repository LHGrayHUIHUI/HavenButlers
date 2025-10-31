package com.haven.base.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 安全配置属性
 * 包含JWT、认证、授权等安全相关配置
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "base-model.security")
public class SecurityProperties {

    /**
     * 是否启用安全功能
     */
    private boolean enabled = true;

    /**
     * JWT配置
     */
    private Jwt jwt = new Jwt();

    /**
     * 认证配置
     */
    private Authentication authentication = new Authentication();

    /**
     * 加密配置
     */
    private Encryption encryption = new Encryption();

    /**
     * JWT配置
     */
    @Setter
    @Getter
    public static class Jwt {

        /**
         * JWT签名密钥
         */
        private String secret = "fJmSbo7pBY8teyzVDyv7YxW1aXPKHSx0uFWxsomMJ4I=";

        /**
         * JWT令牌有效期
         */
        private Duration expiration = Duration.ofHours(24);

        /**
         * JWT刷新令牌有效期
         */
        private Duration refreshExpiration = Duration.ofDays(7);

        /**
         * JWT签发者
         */
        private String issuer = "HavenButler";

        /**
         * JWT受众
         */
        private String audience = "havenbutler-users";

        /**
         * 令牌前缀
         */
        private String tokenPrefix = "Bearer ";

        /**
         * 是否验证签发者
         */
        private boolean validateIssuer = true;

        /**
         * 是否验证受众
         */
        private boolean validateAudience = false;

        /**
         * 是否验证过期时间
         */
        private boolean validateExpiration = true;

        /**
         * 令牌刷新窗口期（在过期前多长时间内允许刷新）
         */
        private Duration refreshWindow = Duration.ofHours(1);

        /**
         * 是否启用刷新令牌
         */
        private boolean enableRefreshToken = true;

        /**
         * 最大并发令牌数（每个用户）
         */
        private int maxConcurrentTokens = 5;

        /**
         * 令牌黑名单缓存时间
         */
        private Duration blacklistCacheTime = Duration.ofDays(7);
    }

    /**
     * 认证配置
     */
    @Setter
    @Getter
    public static class Authentication {

        /**
         * 是否启用认证
         */
        private boolean enabled = true;

        /**
         * 认证类型（jwt、oauth2、basic）
         */
        private String type = "jwt";

        /**
         * 登录失败锁定次数
         */
        private int maxFailedAttempts = 5;

        /**
         * 账户锁定时间
         */
        private Duration lockoutDuration = Duration.ofMinutes(30);

        /**
         * 密码最小长度
         */
        private int minPasswordLength = 8;

        /**
         * 密码最大长度
         */
        private int maxPasswordLength = 128;

        /**
         * 是否启用密码强度验证
         */
        private boolean enablePasswordStrength = true;

        /**
         * 会话超时时间
         */
        private Duration sessionTimeout = Duration.ofHours(8);

        /**
         * 是否启用记住我功能
         */
        private boolean enableRememberMe = true;

        /**
         * 记住我有效期
         */
        private Duration rememberMeDuration = Duration.ofDays(30);
    }

    /**
     * 加密配置
     */
    @Setter
    @Getter
    public static class Encryption {

        /**
         * 是否启用加密
         */
        private boolean enabled = true;

        /**
         * 默认加密算法
         */
        private String algorithm = "AES";

        /**
         * 密钥长度
         */
        private int keySize = 256;

        /**
         * 加密模式
         */
        private String mode = "GCM";

        /**
         * 填充方式
         */
        private String padding = "NoPadding";

        /**
         * 密钥迭代次数
         */
        private int keyIterations = 10000;

        /**
         * 是否启用密钥轮换
         */
        private boolean enableKeyRotation = false;

        /**
         * 密钥轮换周期
         */
        private Duration keyRotationPeriod = Duration.ofDays(90);
    }
}