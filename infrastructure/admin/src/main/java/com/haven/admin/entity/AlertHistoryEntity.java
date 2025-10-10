package com.haven.admin.entity;

import com.haven.admin.model.AlertRule;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 告警历史实体
 * 存储告警触发和处理的历史记录
 *
 * @author HavenButler
 */
@Entity
@Table(name = "alert_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertHistoryEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 告警ID（关联到内存中的Alert.id）
     */
    @Column(name = "alert_id")
    private Long alertId;

    /**
     * 规则ID
     */
    @Column(name = "rule_id")
    private Long ruleId;

    /**
     * 规则名称
     */
    @Column(name = "rule_name", length = 200)
    private String ruleName;

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
     * 告警级别
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private AlertRule.AlertLevel level;

    /**
     * 告警消息
     */
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    /**
     * 指标名称
     */
    @Column(name = "metric_name", length = 100)
    private String metricName;

    /**
     * 当前值
     */
    @Column(name = "current_value")
    private Double currentValue;

    /**
     * 阈值
     */
    @Column(name = "threshold")
    private Double threshold;

    /**
     * 告警状态
     */
    @Column(name = "status", length = 20)
    private String status;

    /**
     * 处理人
     */
    @Column(name = "handler", length = 100)
    private String handler;

    /**
     * 处理时间
     */
    @Column(name = "handle_time")
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    @Column(name = "handle_remark", columnDefinition = "TEXT")
    private String handleRemark;

    /**
     * 通知发送状态
     */
    @Column(name = "notification_status", length = 20)
    private String notificationStatus;

    /**
     * 触发时间
     */
    @Column(name = "trigger_time", nullable = false)
    private Instant triggerTime;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (triggerTime == null) {
            triggerTime = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}