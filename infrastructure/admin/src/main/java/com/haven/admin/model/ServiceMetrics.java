package com.haven.admin.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 服务指标
 */
@Data
public class ServiceMetrics {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * CPU使用率
     */
    private Double cpuUsage;

    /**
     * 内存使用量(MB)
     */
    private Double memoryUsage;

    /**
     * 最大内存(MB)
     */
    private Double memoryMax;

    /**
     * 线程数
     */
    private Double threadCount;

    /**
     * 请求总数
     */
    private Double requestCount;

    /**
     * 请求速率(req/s)
     */
    private Double requestRate;

    /**
     * 错误率
     */
    private Double errorRate;

    /**
     * 平均响应时间(ms)
     */
    private Double responseTime;

    /**
     * P95响应时间(ms)
     */
    private Double responseTimeP95;

    /**
     * P99响应时间(ms)
     */
    private Double responseTimeP99;

    /**
     * GC次数
     */
    private Long gcCount;

    /**
     * GC总时间(ms)
     */
    private Long gcTime;

    /**
     * 活跃连接数
     */
    private Integer activeConnections;

    /**
     * 数据库连接池使用率
     */
    private Double dbPoolUsage;

    /**
     * Redis连接池使用率
     */
    private Double redisPoolUsage;

    /**
     * 自定义指标
     */
    private Map<String, Object> customMetrics;

    /**
     * 采集时间
     */
    private LocalDateTime collectionTime;
}