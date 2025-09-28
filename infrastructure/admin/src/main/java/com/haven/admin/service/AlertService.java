package com.haven.admin.service;

import com.haven.admin.model.Alert;
import com.haven.admin.model.AlertRule;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import com.haven.admin.web.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 告警服务
 * 处理系统告警和通知
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class AlertService {

    // 使用Long作为key，存储Alert对象
    private final ConcurrentMap<Long, Alert> alertStorage = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, AlertRule> ruleStorage = new ConcurrentHashMap<>();
    private final AtomicLong alertIdGenerator = new AtomicLong(1);
    private final AtomicLong ruleIdGenerator = new AtomicLong(1);

    /**
     * 获取告警列表（分页）
     */
    public PageResponse<Alert> getAlerts(String serviceName,
                                       AlertRule.AlertLevel level,
                                       Alert.AlertStatus status,
                                       LocalDateTime startTime,
                                       LocalDateTime endTime,
                                       PageRequest pageRequest) {
        // 模拟数据，实际应该从数据库查询
        List<Alert> allAlerts = new ArrayList<>(alertStorage.values());

        // 过滤条件
        List<Alert> filteredAlerts = allAlerts.stream()
                .filter(alert -> serviceName == null || serviceName.equals(alert.getServiceName()))
                .filter(alert -> level == null || level.equals(alert.getLevel()))
                .filter(alert -> status == null || status.equals(alert.getStatus()))
                .filter(alert -> startTime == null || alert.getTimestamp().isAfter(startTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .filter(alert -> endTime == null || alert.getTimestamp().isBefore(endTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .sorted((a1, a2) -> a2.getTimestamp().compareTo(a1.getTimestamp()))
                .collect(Collectors.toList());

        // 分页处理
        int start = (pageRequest.getPage() - 1) * pageRequest.getSize();
        int end = Math.min(start + pageRequest.getSize(), filteredAlerts.size());
        List<Alert> pageData = start < filteredAlerts.size() ? filteredAlerts.subList(start, end) : new ArrayList<>();

        return PageResponse.of(pageData, (long) filteredAlerts.size(), pageRequest);
    }

    /**
     * 获取告警详情
     */
    public Alert getAlertDetail(Long alertId) {
        Alert alert = alertStorage.get(alertId);
        if (alert == null) {
            throw new BusinessException(40404, "告警不存在: " + alertId);
        }
        return alert;
    }

    /**
     * 处理告警
     */
    public void handleAlert(Long alertId, String handler, String remark) {
        Alert alert = alertStorage.get(alertId);
        if (alert == null) {
            throw new BusinessException(40404, "告警不存在: " + alertId);
        }

        // 更新告警状态
        alert.setStatus(Alert.AlertStatus.RESOLVED);
        alert.setHandler(handler);
        alert.setHandleTime(LocalDateTime.now());
        alert.setHandleRemark(remark);

        log.info("告警已处理: alertId={}, handler={}, remark={}", alertId, handler, remark);
    }

    /**
     * 忽略告警
     */
    public void ignoreAlert(Long alertId, String reason) {
        Alert alert = alertStorage.get(alertId);
        if (alert == null) {
            throw new BusinessException(40404, "告警不存在: " + alertId);
        }

        // 更新告警状态
        alert.setStatus(Alert.AlertStatus.IGNORED);
        alert.setHandleRemark(reason);
        alert.setHandleTime(LocalDateTime.now());

        log.info("告警已忽略: alertId={}, reason={}", alertId, reason);
    }

    /**
     * 获取告警规则列表
     */
    public List<AlertRule> getAlertRules(String serviceName, Boolean enabled) {
        return ruleStorage.values().stream()
                .filter(rule -> serviceName == null || serviceName.equals(rule.getServiceName()))
                .filter(rule -> enabled == null || enabled.equals(rule.getEnabled()))
                .sorted((r1, r2) -> r2.getCreateTime().compareTo(r1.getCreateTime()))
                .collect(Collectors.toList());
    }

    /**
     * 创建告警规则
     */
    public AlertRule createAlertRule(AlertRule rule) {
        Long ruleId = ruleIdGenerator.getAndIncrement();
        rule.setId(ruleId);
        rule.setEnabled(true);
        rule.setCreateTime(LocalDateTime.now());
        rule.setUpdateTime(LocalDateTime.now());
        rule.setTriggerCount(0);

        ruleStorage.put(ruleId, rule);

        log.info("创建告警规则: id={}, name={}", ruleId, rule.getName());
        return rule;
    }

    /**
     * 更新告警规则
     */
    public void updateAlertRule(AlertRule rule) {
        AlertRule existingRule = ruleStorage.get(rule.getId());
        if (existingRule == null) {
            throw new BusinessException(40404, "告警规则不存在: " + rule.getId());
        }

        // 更新规则信息
        rule.setUpdateTime(LocalDateTime.now());
        rule.setCreateTime(existingRule.getCreateTime());
        rule.setTriggerCount(existingRule.getTriggerCount());

        ruleStorage.put(rule.getId(), rule);

        log.info("更新告警规则: id={}, name={}", rule.getId(), rule.getName());
    }

    /**
     * 删除告警规则
     */
    public void deleteAlertRule(Long ruleId) {
        AlertRule rule = ruleStorage.remove(ruleId);
        if (rule == null) {
            throw new BusinessException(40404, "告警规则不存在: " + ruleId);
        }

        log.info("删除告警规则: id={}, name={}", ruleId, rule.getName());
    }

    /**
     * 启用/禁用告警规则
     */
    public void enableAlertRule(Long ruleId, Boolean enabled) {
        AlertRule rule = ruleStorage.get(ruleId);
        if (rule == null) {
            throw new BusinessException(40404, "告警规则不存在: " + ruleId);
        }

        rule.setEnabled(enabled);
        rule.setUpdateTime(LocalDateTime.now());

        log.info("{}告警规则: id={}, name={}", enabled ? "启用" : "禁用", ruleId, rule.getName());
    }

    /**
     * 获取告警统计
     */
    public Map<String, Object> getAlertStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        Map<String, Object> statistics = new HashMap<>();

        List<Alert> alerts = alertStorage.values().stream()
                .filter(alert -> startTime == null || alert.getTimestamp().isAfter(startTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .filter(alert -> endTime == null || alert.getTimestamp().isBefore(endTime.atZone(java.time.ZoneId.systemDefault()).toInstant()))
                .collect(Collectors.toList());

        // 按级别统计
        Map<AlertRule.AlertLevel, Long> levelCounts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getLevel, Collectors.counting()));

        // 按状态统计
        Map<Alert.AlertStatus, Long> statusCounts = alerts.stream()
                .collect(Collectors.groupingBy(Alert::getStatus, Collectors.counting()));

        // 按服务统计
        Map<String, Long> serviceCounts = alerts.stream()
                .filter(alert -> alert.getServiceName() != null)
                .collect(Collectors.groupingBy(Alert::getServiceName, Collectors.counting()));

        statistics.put("total", alerts.size());
        statistics.put("levelCounts", levelCounts);
        statistics.put("statusCounts", statusCounts);
        statistics.put("serviceCounts", serviceCounts);

        return statistics;
    }

    /**
     * 测试告警规则
     */
    public boolean testAlertRule(AlertRule rule) {
        try {
            // 模拟规则测试逻辑
            log.info("测试告警规则: name={}, metric={}", rule.getName(), rule.getMetricName());

            // 这里可以实际执行指标查询和条件判断
            // 暂时返回随机结果
            return Math.random() > 0.5;
        } catch (Exception e) {
            log.error("测试告警规则失败: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 批量处理告警
     */
    public void batchHandleAlerts(List<Long> alertIds, String handler, String remark) {
        for (Long alertId : alertIds) {
            try {
                handleAlert(alertId, handler, remark);
            } catch (Exception e) {
                log.error("批量处理告警失败: alertId={}, error={}", alertId, e.getMessage());
            }
        }
    }

    /**
     * 触发新告警（内部使用）
     */
    public void triggerAlert(String serviceName, String instanceId, AlertRule rule, Double value) {
        Long alertId = alertIdGenerator.getAndIncrement();

        Alert alert = Alert.builder()
                .id(alertId)
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .serviceName(serviceName)
                .instanceId(instanceId)
                .level(rule.getLevel())
                .message(String.format(rule.getMessageTemplate(), value))
                .metricName(rule.getMetricName())
                .value(value)
                .threshold(rule.getThreshold())
                .timestamp(Instant.now())
                .status(Alert.AlertStatus.PENDING)
                .build();

        alertStorage.put(alertId, alert);

        // 更新规则触发次数
        rule.setTriggerCount(rule.getTriggerCount() + 1);
        rule.setLastTriggerTime(LocalDateTime.now());

        log.warn("触发告警: service={}, level={}, message={}", serviceName, rule.getLevel(), alert.getMessage());

        // 发送通知
        sendNotification(alert, rule);
    }

    /**
     * 发送告警通知
     */
    private void sendNotification(Alert alert, AlertRule rule) {
        if (rule.getNotifyType() != null) {
            switch (rule.getNotifyType()) {
                case EMAIL:
                    sendEmailNotification(alert);
                    break;
                case SMS:
                    sendSmsNotification(alert);
                    break;
                case WEBHOOK:
                    sendWebhookNotification(alert);
                    break;
                case WECHAT:
                    sendWechatNotification(alert);
                    break;
                case DINGTALK:
                    sendDingtalkNotification(alert);
                    break;
            }
        }
    }

    private void sendEmailNotification(Alert alert) {
        log.info("发送邮件通知: alertId={}, message={}", alert.getId(), alert.getMessage());
        // 实际实现中调用邮件服务
    }

    private void sendSmsNotification(Alert alert) {
        log.info("发送短信通知: alertId={}, message={}", alert.getId(), alert.getMessage());
        // 实际实现中调用短信服务
    }

    private void sendWebhookNotification(Alert alert) {
        log.info("发送Webhook通知: alertId={}, message={}", alert.getId(), alert.getMessage());
        // 实际实现中调用Webhook服务
    }

    private void sendWechatNotification(Alert alert) {
        log.info("发送微信通知: alertId={}, message={}", alert.getId(), alert.getMessage());
        // 实际实现中调用微信服务
    }

    private void sendDingtalkNotification(Alert alert) {
        log.info("发送钉钉通知: alertId={}, message={}", alert.getId(), alert.getMessage());
        // 实际实现中调用钉钉服务
    }
}