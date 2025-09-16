package com.haven.admin.service;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 告警服务
 * 处理系统告警和通知
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class AlertService {

    private final ConcurrentMap<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final List<AlertRule> alertRules = new ArrayList<>();

    /**
     * 触发告警
     */
    public void triggerAlert(String alertId, AlertLevel level, String title, String message) {
        Alert alert = Alert.builder()
                .alertId(alertId)
                .level(level)
                .title(title)
                .message(message)
                .timestamp(LocalDateTime.now())
                .status(AlertStatus.ACTIVE)
                .build();

        activeAlerts.put(alertId, alert);
        log.warn("告警触发: [{}] {} - {}", level, title, message);

        // 根据级别处理告警
        handleAlert(alert);
    }

    /**
     * 解除告警
     */
    public void resolveAlert(String alertId) {
        Alert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.setStatus(AlertStatus.RESOLVED);
            alert.setResolvedTime(LocalDateTime.now());
            log.info("告警解除: {}", alertId);
        }
    }

    /**
     * 获取所有活跃告警
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * 添加告警规则
     */
    public void addAlertRule(AlertRule rule) {
        alertRules.add(rule);
        log.info("添加告警规则: {}", rule.getRuleName());
    }

    /**
     * 评估告警规则
     */
    public void evaluateRules(String metricName, double value) {
        for (AlertRule rule : alertRules) {
            if (rule.getMetricName().equals(metricName)) {
                boolean shouldAlert = evaluateCondition(rule, value);

                if (shouldAlert) {
                    triggerAlert(
                        rule.getRuleId(),
                        rule.getLevel(),
                        rule.getRuleName(),
                        String.format(rule.getMessageTemplate(), value)
                    );
                } else {
                    // 如果条件不再满足，解除告警
                    resolveAlert(rule.getRuleId());
                }
            }
        }
    }

    /**
     * 评估告警条件
     */
    private boolean evaluateCondition(AlertRule rule, double value) {
        switch (rule.getOperator()) {
            case GREATER_THAN:
                return value > rule.getThreshold();
            case LESS_THAN:
                return value < rule.getThreshold();
            case EQUALS:
                return Math.abs(value - rule.getThreshold()) < 0.001;
            case GREATER_THAN_OR_EQUALS:
                return value >= rule.getThreshold();
            case LESS_THAN_OR_EQUALS:
                return value <= rule.getThreshold();
            default:
                return false;
        }
    }

    /**
     * 处理告警
     */
    private void handleAlert(Alert alert) {
        switch (alert.getLevel()) {
            case CRITICAL:
                // 严重告警：发送短信、邮件、推送
                sendSmsNotification(alert);
                sendEmailNotification(alert);
                sendPushNotification(alert);
                break;
            case WARNING:
                // 警告：发送邮件、推送
                sendEmailNotification(alert);
                sendPushNotification(alert);
                break;
            case INFO:
                // 信息：仅推送
                sendPushNotification(alert);
                break;
        }
    }

    private void sendSmsNotification(Alert alert) {
        log.info("发送短信通知: {}", alert.getTitle());
        // 实际实现中调用短信服务
    }

    private void sendEmailNotification(Alert alert) {
        log.info("发送邮件通知: {}", alert.getTitle());
        // 实际实现中调用邮件服务
    }

    private void sendPushNotification(Alert alert) {
        log.info("发送推送通知: {}", alert.getTitle());
        // 实际实现中调用推送服务
    }

    /**
     * 告警实体
     */
    @Data
    @Builder
    public static class Alert {
        private String alertId;
        private AlertLevel level;
        private String title;
        private String message;
        private LocalDateTime timestamp;
        private LocalDateTime resolvedTime;
        private AlertStatus status;
    }

    /**
     * 告警规则
     */
    @Data
    @Builder
    public static class AlertRule {
        private String ruleId;
        private String ruleName;
        private String metricName;
        private ComparisonOperator operator;
        private double threshold;
        private AlertLevel level;
        private String messageTemplate;
    }

    /**
     * 告警级别
     */
    public enum AlertLevel {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * 告警状态
     */
    public enum AlertStatus {
        ACTIVE,
        RESOLVED,
        ACKNOWLEDGED
    }

    /**
     * 比较操作符
     */
    public enum ComparisonOperator {
        GREATER_THAN,
        LESS_THAN,
        EQUALS,
        GREATER_THAN_OR_EQUALS,
        LESS_THAN_OR_EQUALS
    }
}