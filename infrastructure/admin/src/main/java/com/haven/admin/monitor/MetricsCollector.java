package com.haven.admin.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * 指标收集器
 * 收集和记录系统运行指标
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class MetricsCollector {

    @Autowired
    private MeterRegistry meterRegistry;

    private Counter httpRequestCounter;
    private Counter errorCounter;
    private Timer responseTimer;

    @PostConstruct
    public void init() {
        // 初始化计数器
        httpRequestCounter = Counter.builder("http.requests.total")
                .description("HTTP请求总数")
                .register(meterRegistry);

        errorCounter = Counter.builder("http.requests.errors")
                .description("HTTP请求错误数")
                .register(meterRegistry);

        responseTimer = Timer.builder("http.requests.duration")
                .description("HTTP请求响应时间")
                .register(meterRegistry);

        log.info("指标收集器初始化完成");
    }

    /**
     * 记录HTTP请求
     */
    public void recordHttpRequest(String method, String uri, int status, long duration) {
        httpRequestCounter.increment();

        if (status >= 400) {
            errorCounter.increment();
        }

        responseTimer.record(duration, TimeUnit.MILLISECONDS);

        // 记录详细指标
        meterRegistry.counter("http.requests",
                "method", method,
                "uri", uri,
                "status", String.valueOf(status)
        ).increment();

        meterRegistry.timer("http.response.time",
                "method", method,
                "uri", uri
        ).record(duration, TimeUnit.MILLISECONDS);
    }

    /**
     * 记录业务指标
     */
    public void recordBusinessMetric(String name, double value, String... tags) {
        if (tags.length % 2 != 0) {
            log.warn("标签数量必须为偶数");
            return;
        }

        meterRegistry.gauge(name, io.micrometer.core.instrument.Tags.of(tags), value);
    }

    /**
     * 记录设备在线数量
     */
    public void recordOnlineDevices(int count) {
        meterRegistry.gauge("devices.online", count);
    }

    /**
     * 记录活跃用户数
     */
    public void recordActiveUsers(int count) {
        meterRegistry.gauge("users.active", count);
    }

    /**
     * 记录消息队列深度
     */
    public void recordQueueDepth(String queueName, int depth) {
        meterRegistry.gauge("queue.depth",
                io.micrometer.core.instrument.Tags.of("queue", queueName),
                depth);
    }

    /**
     * 记录缓存命中率
     */
    public void recordCacheHitRate(double rate) {
        meterRegistry.gauge("cache.hit.rate", rate);
    }

    /**
     * 记录数据库连接池状态
     */
    public void recordConnectionPoolStatus(int active, int idle, int total) {
        meterRegistry.gauge("db.connections.active", active);
        meterRegistry.gauge("db.connections.idle", idle);
        meterRegistry.gauge("db.connections.total", total);
    }

    /**
     * 记录API调用次数
     */
    public void incrementApiCall(String api, boolean success) {
        meterRegistry.counter("api.calls",
                "api", api,
                "status", success ? "success" : "failure"
        ).increment();
    }

    /**
     * 记录任务执行时间
     */
    public void recordTaskDuration(String taskName, long duration) {
        meterRegistry.timer("task.duration",
                "task", taskName
        ).record(duration, TimeUnit.MILLISECONDS);
    }
}