package com.haven.storage.logging.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 性能日志实体类
 * 记录系统性能指标和监控数据
 *
 * @author HavenButler
 */
@Data
@Entity
@Table(name = "performance_logs", indexes = {
    @Index(name = "idx_service_metric", columnList = "service_type,metric_name,created_at"),
    @Index(name = "idx_instance_created", columnList = "instance_id,created_at"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
public class PerformanceLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 服务类型：POSTGRESQL、MONGODB、REDIS、HTTP_API、SYSTEM
     */
    @Column(name = "service_type", length = 20, nullable = false)
    private String serviceType;

    /**
     * 指标名称：
     * connection_pool_active - 活跃连接数
     * connection_pool_idle - 空闲连接数
     * response_time - 响应时间
     * qps - 每秒查询数
     * error_rate - 错误率
     * memory_usage - 内存使用率
     * cpu_usage - CPU使用率
     * disk_usage - 磁盘使用率
     */
    @Column(name = "metric_name", length = 50, nullable = false)
    private String metricName;

    /**
     * 指标值
     */
    @Column(name = "metric_value", precision = 10, scale = 2, nullable = false)
    private Double metricValue;

    /**
     * 指标单位：ms、count、percentage、bytes等
     */
    @Column(name = "metric_unit", length = 20)
    private String metricUnit;

    /**
     * 实例ID(用于区分多实例部署)
     */
    @Column(name = "instance_id", length = 100)
    private String instanceId;

    /**
     * 主机名或容器名
     */
    @Column(name = "hostname", length = 100)
    private String hostname;

    /**
     * 标签信息(JSON格式，用于多维度查询)
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /**
     * 阈值配置
     */
    @Column(name = "threshold_warning")
    private Double thresholdWarning;

    @Column(name = "threshold_critical")
    private Double thresholdCritical;

    /**
     * 是否超过阈值
     */
    @Column(name = "is_threshold_exceeded")
    private Boolean isThresholdExceeded = false;

    /**
     * 采集时间间隔(秒)
     */
    @Column(name = "collection_interval")
    private Integer collectionInterval;

    /**
     * 数据来源：MICROMETER、JMX、CUSTOM
     */
    @Column(name = "data_source", length = 20)
    private String dataSource;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 额外的元数据信息
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isThresholdExceeded == null) {
            isThresholdExceeded = false;
        }

        // 检查是否超过阈值
        checkThreshold();
    }

    @PreUpdate
    protected void onUpdate() {
        checkThreshold();
    }

    /**
     * 检查是否超过阈值
     */
    private void checkThreshold() {
        if (metricValue != null) {
            if (thresholdCritical != null && metricValue >= thresholdCritical) {
                isThresholdExceeded = true;
            } else if (thresholdWarning != null && metricValue >= thresholdWarning) {
                isThresholdExceeded = true;
            } else {
                isThresholdExceeded = false;
            }
        }
    }

    /**
     * 获取告警级别
     */
    @Transient
    public String getAlertLevel() {
        if (metricValue == null) {
            return "NORMAL";
        }

        if (thresholdCritical != null && metricValue >= thresholdCritical) {
            return "CRITICAL";
        } else if (thresholdWarning != null && metricValue >= thresholdWarning) {
            return "WARNING";
        } else {
            return "NORMAL";
        }
    }
}