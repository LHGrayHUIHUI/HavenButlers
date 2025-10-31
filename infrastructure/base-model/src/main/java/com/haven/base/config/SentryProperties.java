package com.haven.base.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sentry 异常监控配置
 * 支持异常追踪、性能监控、用户反馈等功能
 *
 * @author HavenButler
 */
@Data
@Component
@ConfigurationProperties(prefix = "base-model.sentry")
public class SentryProperties {

    /**
     * 是否启用Sentry
     */
    private boolean enabled = false;

    /**
     * Sentry DSN
     */
    private String dsn = "";

    /**
     * 环境名称
     */
    private String environment = "development";

    /**
     * 采样率 (0.0 - 1.0)
     */
    private double tracesSampleRate = 0.1;

    /**
     * 异常采样率
     */
    private double sampleRate = 1.0;

    /**
     * 项目名称
     */
    private String projectName = "base-model";

    /**
     * 版本号
     */
    private String release = "";

    /**
     * 服务器名称
     */
    private String serverName = "";

    /**
     * 标签
     */
    private TagsConfig tags = new TagsConfig();

    /**
     * 异常过滤器配置
     */
    private ExceptionFilterConfig exceptionFilter = new ExceptionFilterConfig();

    /**
     * 性能监控配置
     */
    private PerformanceConfig performance = new PerformanceConfig();

    @Data
    public static class TagsConfig {
        /**
         * 应用标签
         */
        private String application = "havenbutler";

        /**
         * 模块标签
         */
        private String module = "base-model";

        /**
         * 团队标签
         */
        private String team = "backend";

        /**
         * 自定义标签
         */
        private java.util.Map<String, String> custom = new java.util.HashMap<>();
    }

    @Data
    public static class ExceptionFilterConfig {
        /**
         * 是否过滤404异常
         */
        private boolean filter404 = true;

        /**
         * 是否过滤客户端异常
         */
        private boolean filterClientErrors = true;

        /**
         * 排除的异常类型
         */
        private String[] excludedExceptions = {
            "java.lang.InterruptedException",
            "org.springframework.web.servlet.NoHandlerFoundException"
        };

        /**
         * 包含的异常类型（仅监控这些异常）
         */
        private String[] includedExceptions = {};
    }

    @Data
    public static class PerformanceConfig {
        /**
         * 是否启用性能监控
         */
        private boolean enabled = true;

        /**
         * 是否启用数据库监控
         */
        private boolean enableDatabase = true;

        /**
         * 是否启用HTTP监控
         */
        private boolean enableHttp = true;

        /**
         * 慢查询阈值（毫秒）
         */
        private long slowQueryThreshold = 1000;

        /**
         * 慢请求阈值（毫秒）
         */
        private long slowRequestThreshold = 2000;
    }
}