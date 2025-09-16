package com.haven.admin.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警信息
 */
@Data
@Builder
public class Alert {

    /**
     * 告警ID
     */
    private Long id;

    /**
     * 规则ID
     */
    private Long ruleId;

    /**
     * 规则名称
     */
    private String ruleName;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 告警级别
     */
    private AlertRule.AlertLevel level;

    /**
     * 告警消息
     */
    private String message;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 当前值
     */
    private Double value;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 告警时间
     */
    private Instant timestamp;

    /**
     * 告警状态
     */
    private AlertStatus status;

    /**
     * 处理人
     */
    private String handler;

    /**
     * 处理时间
     */
    private LocalDateTime handleTime;

    /**
     * 处理备注
     */
    private String handleRemark;

    /**
     * 附加信息
     */
    private Map<String, Object> metadata;

    /**
     * 告警状态
     */
    public enum AlertStatus {
        PENDING("待处理"),
        PROCESSING("处理中"),
        RESOLVED("已解决"),
        IGNORED("已忽略");

        private final String description;

        AlertStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}