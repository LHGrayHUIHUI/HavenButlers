package com.haven.base.monitor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * 默认指标收集器实现
 * 基于内存的简单实现，适合开发和测试环境
 * 生产环境建议集成Micrometer + Prometheus等专业监控方案
 *
 * @author HavenButler
 */
@Slf4j
@Component
@ConditionalOnMissingBean(MetricsCollector.class)
public class DefaultMetricsCollector implements MetricsCollector {

    private final Map<String, DoubleAdder> counters = new ConcurrentHashMap<>();
    private final Map<String, Double> gauges = new ConcurrentHashMap<>();
    private final Map<String, TimerStats> timers = new ConcurrentHashMap<>();
    private final Map<String, HistogramStats> histograms = new ConcurrentHashMap<>();

    @Override
    public void incrementCounter(String metricName, double increment, Map<String, String> tags) {
        String key = buildKey(metricName, tags);
        counters.computeIfAbsent(key, k -> new DoubleAdder()).add(increment);
        log.debug("计数器指标 [{}] 增加: {}", key, increment);
    }

    @Override
    public void recordGauge(String metricName, double value, Map<String, String> tags) {
        String key = buildKey(metricName, tags);
        gauges.put(key, value);
        log.debug("计量器指标 [{}] 记录: {}", key, value);
    }

    @Override
    public void recordHistogram(String metricName, double value, Map<String, String> tags) {
        String key = buildKey(metricName, tags);
        histograms.computeIfAbsent(key, k -> new HistogramStats()).record(value);
        log.debug("直方图指标 [{}] 记录: {}", key, value);
    }

    @Override
    public void recordTimer(String metricName, long durationMs, Map<String, String> tags) {
        String key = buildKey(metricName, tags);
        timers.computeIfAbsent(key, k -> new TimerStats()).record(durationMs);
        log.debug("定时器指标 [{}] 记录: {}ms", key, durationMs);
    }

    @Override
    public TimerHandle startTimer(String metricName, Map<String, String> tags) {
        String key = buildKey(metricName, tags);
        return new DefaultTimerHandle(key, System.currentTimeMillis());
    }

    @Override
    public void recordBusinessMetric(String event, double value, Map<String, String> tags) {
        String key = "business." + buildKey(event, tags);
        counters.computeIfAbsent(key, k -> new DoubleAdder()).add(value);
        log.info("业务指标 [{}] 记录: {}", event, value);
    }

    @Override
    public MetricsSummary getMetricsSummary() {
        MetricsSummary summary = new MetricsSummary();

        // 收集计数器数据
        counters.forEach((key, adder) -> summary.addCounter(key, adder.sum()));

        // 收集计量器数据
        gauges.forEach(summary::addGauge);

        // 收集定时器数据
        timers.forEach((key, stats) -> {
            Map<String, Object> timerData = Map.of(
                    "count", stats.getCount(),
                    "totalMs", stats.getTotalMs(),
                    "avgMs", stats.getAverageMs(),
                    "maxMs", stats.getMaxMs(),
                    "minMs", stats.getMinMs()
            );
            summary.addTimer(key, timerData);
        });

        log.debug("生成指标摘要，计数器: {}, 计量器: {}, 定时器: {}",
                counters.size(), gauges.size(), timers.size());

        return summary;
    }

    /**
     * 构建指标键名
     */
    private String buildKey(String metricName, Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return metricName;
        }

        StringBuilder keyBuilder = new StringBuilder(metricName);
        tags.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> keyBuilder.append(",")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue()));

        return keyBuilder.toString();
    }

    /**
     * 默认计时器句柄实现
     */
    private class DefaultTimerHandle implements TimerHandle {
        private final String key;
        private final long startTime;

        public DefaultTimerHandle(String key, long startTime) {
            this.key = key;
            this.startTime = startTime;
        }

        @Override
        public void stop() {
            long duration = System.currentTimeMillis() - startTime;
            timers.computeIfAbsent(key, k -> new TimerStats()).record(duration);
            log.debug("定时器 [{}] 停止，耗时: {}ms", key, duration);
        }

        @Override
        public long getElapsedMs() {
            return System.currentTimeMillis() - startTime;
        }
    }

    /**
     * 定时器统计信息
     */
    private static class TimerStats {
        private final AtomicLong count = new AtomicLong(0);
        private final AtomicLong totalMs = new AtomicLong(0);
        private volatile long maxMs = 0;
        private volatile long minMs = Long.MAX_VALUE;

        public synchronized void record(long durationMs) {
            count.incrementAndGet();
            totalMs.addAndGet(durationMs);
            maxMs = Math.max(maxMs, durationMs);
            minMs = Math.min(minMs, durationMs);
        }

        public long getCount() { return count.get(); }
        public long getTotalMs() { return totalMs.get(); }
        public double getAverageMs() {
            long c = count.get();
            return c == 0 ? 0.0 : (double) totalMs.get() / c;
        }
        public long getMaxMs() { return maxMs; }
        public long getMinMs() { return minMs == Long.MAX_VALUE ? 0 : minMs; }
    }

    /**
     * 直方图统计信息
     */
    private static class HistogramStats {
        private final AtomicLong count = new AtomicLong(0);
        private final DoubleAdder sum = new DoubleAdder();
        private volatile double max = Double.NEGATIVE_INFINITY;
        private volatile double min = Double.POSITIVE_INFINITY;

        public synchronized void record(double value) {
            count.incrementAndGet();
            sum.add(value);
            max = Math.max(max, value);
            min = Math.min(min, value);
        }

        public long getCount() { return count.get(); }
        public double getSum() { return sum.sum(); }
        public double getAverage() {
            long c = count.get();
            return c == 0 ? 0.0 : sum.sum() / c;
        }
        public double getMax() { return max == Double.NEGATIVE_INFINITY ? 0.0 : max; }
        public double getMin() { return min == Double.POSITIVE_INFINITY ? 0.0 : min; }
    }

    /**
     * 清理指标数据
     * 用于测试环境或定期清理
     */
    public void clearMetrics() {
        counters.clear();
        gauges.clear();
        timers.clear();
        histograms.clear();
        log.info("指标数据已清理");
    }
}