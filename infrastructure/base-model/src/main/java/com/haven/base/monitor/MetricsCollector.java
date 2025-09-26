package com.haven.base.monitor;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 统一指标收集接口
 * 为微服务架构提供标准化的指标收集能力，支持业务指标、技术指标等
 *
 * 功能特性：
 * - 计数器指标（Counter）- 只增不减的累计值
 * - 计量器指标（Gauge）- 可增可减的瞬时值
 * - 直方图指标（Histogram）- 观测值的分布统计
 * - 定时器指标（Timer）- 时间统计和频率统计
 * - 自定义业务指标支持
 *
 * @author HavenButler
 */
public interface MetricsCollector {

    /**
     * 递增计数器指标
     * 用于统计累计性数据，如总请求数、总错误数等
     *
     * @param metricName 指标名称，建议使用点分格式如 "service.request.total"
     * @param increment 增量值，默认为1
     * @param tags 标签信息，用于指标分组，如 {"service":"account", "method":"login"}
     */
    void incrementCounter(String metricName, double increment, Map<String, String> tags);

    /**
     * 递增计数器指标（默认增量1）
     */
    default void incrementCounter(String metricName, Map<String, String> tags) {
        incrementCounter(metricName, 1.0, tags);
    }

    /**
     * 记录计量器指标
     * 用于记录瞬时值，如当前线程数、内存使用量等
     *
     * @param metricName 指标名称
     * @param value 指标值
     * @param tags 标签信息
     */
    void recordGauge(String metricName, double value, Map<String, String> tags);

    /**
     * 记录直方图指标
     * 用于统计数值分布，如响应时间分布、请求大小分布等
     *
     * @param metricName 指标名称
     * @param value 观测值
     * @param tags 标签信息
     */
    void recordHistogram(String metricName, double value, Map<String, String> tags);

    /**
     * 记录定时器指标
     * 用于统计操作耗时，自动计算平均时间、QPS等
     *
     * @param metricName 指标名称
     * @param durationMs 操作耗时（毫秒）
     * @param tags 标签信息
     */
    void recordTimer(String metricName, long durationMs, Map<String, String> tags);

    /**
     * 开始计时
     * 返回计时器句柄，用于后续停止计时
     *
     * @param metricName 指标名称
     * @param tags 标签信息
     * @return 计时器句柄
     */
    TimerHandle startTimer(String metricName, Map<String, String> tags);

    /**
     * 记录业务指标
     * 用于记录业务相关的自定义指标
     *
     * @param event 业务事件名称
     * @param value 指标值
     * @param tags 业务标签
     */
    void recordBusinessMetric(String event, double value, Map<String, String> tags);

    /**
     * 获取指标摘要信息
     * 用于健康检查和监控展示
     *
     * @return 指标摘要
     */
    MetricsSummary getMetricsSummary();

    /**
     * 计时器句柄
     * 用于精确测量代码块执行时间
     */
    interface TimerHandle {
        /**
         * 停止计时并记录结果
         */
        void stop();

        /**
         * 获取已经过的时间（毫秒）
         */
        long getElapsedMs();
    }

    /**
     * 指标摘要信息
     */
    class MetricsSummary {
        private final Map<String, Object> counters = new ConcurrentHashMap<>();
        private final Map<String, Object> gauges = new ConcurrentHashMap<>();
        private final Map<String, Object> timers = new ConcurrentHashMap<>();
        private final LocalDateTime collectTime = LocalDateTime.now();

        public Map<String, Object> getCounters() { return counters; }
        public Map<String, Object> getGauges() { return gauges; }
        public Map<String, Object> getTimers() { return timers; }
        public LocalDateTime getCollectTime() { return collectTime; }

        public void addCounter(String name, Object value) {
            counters.put(name, value);
        }

        public void addGauge(String name, Object value) {
            gauges.put(name, value);
        }

        public void addTimer(String name, Object value) {
            timers.put(name, value);
        }
    }

    /**
     * 常用业务指标常量
     */
    class BusinessMetrics {
        // 用户相关
        public static final String USER_LOGIN_SUCCESS = "user.login.success";
        public static final String USER_LOGIN_FAILED = "user.login.failed";
        public static final String USER_REGISTER = "user.register";

        // 设备相关
        public static final String DEVICE_ONLINE = "device.online";
        public static final String DEVICE_OFFLINE = "device.offline";
        public static final String DEVICE_COMMAND_EXECUTE = "device.command.execute";

        // AI服务相关
        public static final String AI_REQUEST_SUCCESS = "ai.request.success";
        public static final String AI_REQUEST_FAILED = "ai.request.failed";
        public static final String NLP_PARSE_SUCCESS = "nlp.parse.success";
        public static final String NLP_PARSE_FAILED = "nlp.parse.failed";

        // 存储服务相关
        public static final String STORAGE_READ = "storage.read";
        public static final String STORAGE_WRITE = "storage.write";
        public static final String STORAGE_DELETE = "storage.delete";
    }

    /**
     * 常用技术指标常量
     */
    class TechnicalMetrics {
        // HTTP相关
        public static final String HTTP_REQUESTS_TOTAL = "http.requests.total";
        public static final String HTTP_REQUEST_DURATION = "http.request.duration";
        public static final String HTTP_ERRORS_TOTAL = "http.errors.total";

        // 数据库相关
        public static final String DB_CONNECTIONS_ACTIVE = "db.connections.active";
        public static final String DB_QUERY_DURATION = "db.query.duration";
        public static final String DB_ERRORS_TOTAL = "db.errors.total";

        // 缓存相关
        public static final String CACHE_HITS_TOTAL = "cache.hits.total";
        public static final String CACHE_MISSES_TOTAL = "cache.misses.total";
        public static final String CACHE_EVICTIONS_TOTAL = "cache.evictions.total";

        // JVM相关
        public static final String JVM_MEMORY_USED = "jvm.memory.used";
        public static final String JVM_THREADS_ACTIVE = "jvm.threads.active";
        public static final String JVM_GC_DURATION = "jvm.gc.duration";
    }
}