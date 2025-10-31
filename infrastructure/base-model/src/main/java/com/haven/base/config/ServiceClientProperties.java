package com.haven.base.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * ServiceClient配置属性
 * 支持超时、重试、连接池等配置
 *
 * @author HavenButler
 */
@Setter
@Getter
@ConfigurationProperties(prefix = "base-model.service-client")
public class ServiceClientProperties {

    // Getters and Setters
    /**
     * 是否启用ServiceClient
     */
    private boolean enabled = true;

    /**
     * 连接超时时间（毫秒）
     */
    private int connectTimeout = 5000;

    /**
     * 读取超时时间（毫秒）
     */
    private int readTimeout = 30000;

    /**
     * 写入超时时间（毫秒）
     */
    private int writeTimeout = 30000;

    /**
     * 重试配置
     */
    private Retry retry = new Retry();

    /**
     * 连接池配置
     */
    private ConnectionPool connectionPool = new ConnectionPool();

    /**
     * 请求头配置
     */
    private Headers headers = new Headers();

    /**
     * 日志配置
     */
    private Logging logging = new Logging();

    /**
     * 重试配置
     */
    @Setter
    @Getter
    public static class Retry {
        // Getters and Setters
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
        private long interval = 1000;

        /**
         * 重试间隔递增因子
         */
        private double multiplier = 2.0;

        /**
         * 最大重试间隔（毫秒）
         */
        private long maxInterval = 10000;

        /**
         * 需要重试的HTTP状态码
         */
        private int[] retryableStatusCodes = {500, 502, 503, 504};

        /**
         * 需要重试的异常类型
         */
        private String[] retryableExceptions = {
            "java.net.SocketTimeoutException",
            "java.net.ConnectException",
            "org.springframework.web.client.ResourceAccessException"
        };

    }

    /**
     * 连接池配置
     */
    @Setter
    @Getter
    public static class ConnectionPool {
        // Getters and Setters
        /**
         * 最大连接数
         */
        private int maxTotal = 200;

        /**
         * 每个路由的最大连接数
         */
        private int maxPerRoute = 50;

        /**
         * 连接存活时间（秒）
         */
        private int timeToLive = 60;

        /**
         * 连接空闲超时时间（秒）
         */
        private int idleTimeout = 30;

        /**
         * 验证连接间隔（毫秒）
         */
        private int validateAfterInactivity = 2000;

    }

    /**
     * 请求头配置
     */
    @Setter
    @Getter
    public static class Headers {
        // Getters and Setters
        /**
         * 默认User-Agent
         */
        private String userAgent = "HavenButler-ServiceClient/1.0";

        /**
         * TraceID请求头名称
         */
        private String traceIdName = "X-Trace-ID";

        /**
         * 用户ID请求头名称
         */
        private String userIdName = "X-User-ID";

        /**
         * 自定义请求头
         */
        private java.util.Map<String, String> custom = new java.util.HashMap<>();

    }

    /**
     * 日志配置
     */
    @Setter
    @Getter
    public static class Logging {
        // Getters and Setters
        /**
         * 是否启用请求日志
         */
        private boolean requestEnabled = true;

        /**
         * 是否启用响应日志
         */
        private boolean responseEnabled = true;

        /**
         * 是否记录请求体
         */
        private boolean includeRequestBody = false;

        /**
         * 是否记录响应体
         */
        private boolean includeResponseBody = false;

        /**
         * 最大日志体大小（字节）
         */
        private int maxBodySize = 1024;

        /**
         * 慢请求阈值（毫秒）
         */
        private long slowRequestThreshold = 3000;

    }
}