package com.haven.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 监控数据实体
 * 存储各服务的监控指标数据
 *
 * @author HavenButler
 */
@Entity
@Table(name = "monitoring_data")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonitoringDataEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 服务名称
     */
    @Column(name = "service_name", nullable = false, length = 100)
    private String serviceName;

    /**
     * 实例ID
     */
    @Column(name = "instance_id", length = 100)
    private String instanceId;

    /**
     * 指标名称
     */
    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    /**
     * 指标值
     */
    @Column(name = "metric_value")
    private Double metricValue;

    /**
     * 指标类型
     */
    @Column(name = "metric_type", length = 50)
    private String metricType;

    /**
     * 单位
     */
    @Column(name = "unit", length = 20)
    private String unit;

    /**
     * 标签（JSON格式）
     */
    @Column(name = "tags", columnDefinition = "TEXT")
    private String tags;

    /**
     * 时间戳
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}