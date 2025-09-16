package com.haven.admin.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 告警规则
 */
@Data
public class AlertRule {

    /**
     * 规则ID
     */
    private Long id;

    /**
     * 规则名称
     */
    private String name;

    /**
     * 规则描述
     */
    private String description;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 指标名称
     */
    private String metricName;

    /**
     * 比较操作符
     */
    private Operator operator;

    /**
     * 阈值
     */
    private Double threshold;

    /**
     * 时间窗口(秒)
     */
    private Integer window;

    /**
     * 告警级别
     */
    private AlertLevel level;

    /**
     * 告警消息模板
     */
    private String messageTemplate;

    /**
     * 是否启用
     */
    private Boolean enabled;

    /**
     * 通知方式
     */
    private NotifyType notifyType;

    /**
     * 通知配置
     */
    private Map<String, String> notifyConfig;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 最后触发时间
     */
    private LocalDateTime lastTriggerTime;

    /**
     * 触发次数
     */
    private Integer triggerCount;

    /**
     * 比较操作符
     */
    public enum Operator {
        GREATER_THAN(">"),
        GREATER_THAN_OR_EQUAL(">="),
        LESS_THAN("<"),
        LESS_THAN_OR_EQUAL("<="),
        EQUALS("=="),
        NOT_EQUALS("!=");

        private final String symbol;

        Operator(String symbol) {
            this.symbol = symbol;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        CRITICAL("严重"),
        WARNING("警告"),
        INFO("信息");

        private final String description;

        AlertLevel(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 通知方式
     */
    public enum NotifyType {
        EMAIL("邮件"),
        SMS("短信"),
        WEBHOOK("Webhook"),
        WECHAT("微信"),
        DINGTALK("钉钉");

        private final String description;

        NotifyType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}