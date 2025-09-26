package com.haven.base.monitor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 统一健康检查指示器接口
 * 为微服务架构提供标准化的健康检查能力，支持依赖服务检查、资源状态检查等
 *
 * 功能特性：
 * - 服务自身健康检查
 * - 依赖服务健康检查
 * - 数据库连接健康检查
 * - 缓存服务健康检查
 * - 外部API健康检查
 * - 异步健康检查支持
 *
 * @author HavenButler
 */
public interface HealthIndicator {

    /**
     * 执行健康检查
     *
     * @return 健康检查结果
     */
    HealthCheckResult checkHealth();

    /**
     * 异步执行健康检查
     * 适用于检查耗时较长的场景
     *
     * @return 健康检查结果的Future
     */
    default CompletableFuture<HealthCheckResult> checkHealthAsync() {
        return CompletableFuture.supplyAsync(this::checkHealth);
    }

    /**
     * 获取健康检查器名称
     * 用于标识不同的检查器
     *
     * @return 检查器名称
     */
    String getName();

    /**
     * 获取健康检查器类型
     *
     * @return 检查器类型
     */
    HealthCheckType getType();

    /**
     * 获取检查超时时间（毫秒）
     * 超过此时间将标记为检查失败
     *
     * @return 超时时间
     */
    default long getTimeoutMs() {
        return 5000; // 默认5秒超时
    }

    /**
     * 是否为关键检查器
     * 关键检查器失败将导致整体服务标记为不健康
     *
     * @return 是否关键
     */
    default boolean isCritical() {
        return true;
    }

    /**
     * 健康检查结果
     */
    class HealthCheckResult {
        private final String name;
        private final HealthStatus status;
        private final String message;
        private final Map<String, Object> details;
        private final long responseTimeMs;
        private final LocalDateTime checkTime;
        private final Throwable error;

        public HealthCheckResult(String name, HealthStatus status, String message,
                               Map<String, Object> details, long responseTimeMs, Throwable error) {
            this.name = name;
            this.status = status;
            this.message = message;
            this.details = details;
            this.responseTimeMs = responseTimeMs;
            this.checkTime = LocalDateTime.now();
            this.error = error;
        }

        // 成功结果构造器
        public static HealthCheckResult success(String name, String message, Map<String, Object> details, long responseTimeMs) {
            return new HealthCheckResult(name, HealthStatus.UP, message, details, responseTimeMs, null);
        }

        // 失败结果构造器
        public static HealthCheckResult failure(String name, String message, Throwable error, long responseTimeMs) {
            return new HealthCheckResult(name, HealthStatus.DOWN, message, null, responseTimeMs, error);
        }

        // 警告结果构造器
        public static HealthCheckResult warning(String name, String message, Map<String, Object> details, long responseTimeMs) {
            return new HealthCheckResult(name, HealthStatus.WARNING, message, details, responseTimeMs, null);
        }

        // Getters
        public String getName() { return name; }
        public HealthStatus getStatus() { return status; }
        public String getMessage() { return message; }
        public Map<String, Object> details() { return details; }
        public long getResponseTimeMs() { return responseTimeMs; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public Throwable getError() { return error; }

        /**
         * 是否健康
         */
        public boolean isHealthy() {
            return status == HealthStatus.UP;
        }

        /**
         * 是否不健康
         */
        public boolean isUnhealthy() {
            return status == HealthStatus.DOWN;
        }

        /**
         * 是否有警告
         */
        public boolean hasWarning() {
            return status == HealthStatus.WARNING;
        }
    }

    /**
     * 健康状态枚举
     */
    enum HealthStatus {
        /**
         * 健康状态
         */
        UP("UP", "服务健康"),

        /**
         * 不健康状态
         */
        DOWN("DOWN", "服务不健康"),

        /**
         * 警告状态
         */
        WARNING("WARNING", "服务有警告");

        private final String code;
        private final String description;

        HealthStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * 健康检查器类型
     */
    enum HealthCheckType {
        /**
         * 数据库检查
         */
        DATABASE("数据库连接检查"),

        /**
         * 缓存检查
         */
        CACHE("缓存服务检查"),

        /**
         * 外部服务检查
         */
        EXTERNAL_SERVICE("外部服务检查"),

        /**
         * 消息队列检查
         */
        MESSAGE_QUEUE("消息队列检查"),

        /**
         * 文件系统检查
         */
        FILE_SYSTEM("文件系统检查"),

        /**
         * 自定义检查
         */
        CUSTOM("自定义检查");

        private final String description;

        HealthCheckType(String description) {
            this.description = description;
        }

        public String getDescription() { return description; }
    }

    /**
     * 聚合健康检查结果
     */
    class AggregatedHealthResult {
        private final HealthStatus overallStatus;
        private final List<HealthCheckResult> results;
        private final LocalDateTime checkTime;
        private final long totalResponseTimeMs;

        public AggregatedHealthResult(HealthStatus overallStatus, List<HealthCheckResult> results, long totalResponseTimeMs) {
            this.overallStatus = overallStatus;
            this.results = results;
            this.totalResponseTimeMs = totalResponseTimeMs;
            this.checkTime = LocalDateTime.now();
        }

        // Getters
        public HealthStatus getOverallStatus() { return overallStatus; }
        public List<HealthCheckResult> getResults() { return results; }
        public LocalDateTime getCheckTime() { return checkTime; }
        public long getTotalResponseTimeMs() { return totalResponseTimeMs; }

        /**
         * 获取失败的检查结果
         */
        public List<HealthCheckResult> getFailedChecks() {
            return results.stream()
                    .filter(HealthCheckResult::isUnhealthy)
                    .toList();
        }

        /**
         * 获取警告的检查结果
         */
        public List<HealthCheckResult> getWarningChecks() {
            return results.stream()
                    .filter(HealthCheckResult::hasWarning)
                    .toList();
        }

        /**
         * 获取成功的检查结果
         */
        public List<HealthCheckResult> getSuccessfulChecks() {
            return results.stream()
                    .filter(HealthCheckResult::isHealthy)
                    .toList();
        }

        /**
         * 是否整体健康
         */
        public boolean isHealthy() {
            return overallStatus == HealthStatus.UP;
        }
    }

    /**
     * 常用健康检查器名称常量
     */
    class HealthCheckNames {
        // 数据库相关
        public static final String MYSQL_HEALTH = "mysql";
        public static final String MONGODB_HEALTH = "mongodb";
        public static final String REDIS_HEALTH = "redis";

        // 微服务相关
        public static final String STORAGE_SERVICE_HEALTH = "storage-service";
        public static final String ACCOUNT_SERVICE_HEALTH = "account-service";
        public static final String AI_SERVICE_HEALTH = "ai-service";
        public static final String NLP_SERVICE_HEALTH = "nlp-service";

        // 外部服务相关
        public static final String NACOS_HEALTH = "nacos";
        public static final String GATEWAY_HEALTH = "gateway";

        // 系统资源相关
        public static final String DISK_SPACE_HEALTH = "diskSpace";
        public static final String MEMORY_HEALTH = "memory";
        public static final String CPU_HEALTH = "cpu";
    }
}