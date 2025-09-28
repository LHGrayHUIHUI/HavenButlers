package com.haven.admin.controller;

import com.haven.admin.model.Alert;
import com.haven.admin.model.AlertRule;
import com.haven.admin.service.AlertService;
import com.haven.admin.common.AdminResponse;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 告警管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/alert")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    /**
     * 获取告警列表
     */
    @GetMapping("/list")
    public AdminResponse<PageResponse<Alert>> getAlertList(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) AlertRule.AlertLevel level,
            @RequestParam(required = false) Alert.AlertStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {

        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);

        PageResponse<Alert> alerts = alertService.getAlerts(
                serviceName, level, status, startTime, endTime, pageRequest);
        return AdminResponse.success(alerts);
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/{alertId}")
    public AdminResponse<Alert> getAlertDetail(@PathVariable Long alertId) {
        Alert alert = alertService.getAlertDetail(alertId);
        return AdminResponse.success(alert);
    }

    /**
     * 处理告警
     */
    @PostMapping("/{alertId}/handle")
    public AdminResponse<Void> handleAlert(
            @PathVariable Long alertId,
            @RequestParam String handler,
            @RequestParam(required = false) String remark) {
        alertService.handleAlert(alertId, handler, remark);
        log.info("处理告警: {}, 处理人: {}", alertId, handler);
        return AdminResponse.success();
    }

    /**
     * 忽略告警
     */
    @PostMapping("/{alertId}/ignore")
    public AdminResponse<Void> ignoreAlert(
            @PathVariable Long alertId,
            @RequestParam String reason) {
        alertService.ignoreAlert(alertId, reason);
        log.info("忽略告警: {}, 原因: {}", alertId, reason);
        return AdminResponse.success();
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    public AdminResponse<List<AlertRule>> getAlertRules(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) Boolean enabled) {
        List<AlertRule> rules = alertService.getAlertRules(serviceName, enabled);
        return AdminResponse.success(rules);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rule")
    public AdminResponse<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        AlertRule createdRule = alertService.createAlertRule(rule);
        log.info("创建告警规则: {}", createdRule.getName());
        return AdminResponse.success(createdRule);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rule/{ruleId}")
    public AdminResponse<Void> updateAlertRule(
            @PathVariable Long ruleId,
            @RequestBody AlertRule rule) {
        rule.setId(ruleId);
        alertService.updateAlertRule(rule);
        log.info("更新告警规则: {}", ruleId);
        return AdminResponse.success();
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rule/{ruleId}")
    public AdminResponse<Void> deleteAlertRule(@PathVariable Long ruleId) {
        alertService.deleteAlertRule(ruleId);
        log.info("删除告警规则: {}", ruleId);
        return AdminResponse.success();
    }

    /**
     * 启用/禁用告警规则
     */
    @PutMapping("/rule/{ruleId}/enable")
    public AdminResponse<Void> enableAlertRule(
            @PathVariable Long ruleId,
            @RequestParam Boolean enabled) {
        alertService.enableAlertRule(ruleId, enabled);
        log.info("{}告警规则: {}", enabled ? "启用" : "禁用", ruleId);
        return AdminResponse.success();
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    public AdminResponse<Map<String, Object>> getAlertStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Map<String, Object> statistics = alertService.getAlertStatistics(startTime, endTime);
        return AdminResponse.success(statistics);
    }

    /**
     * 测试告警规则
     */
    @PostMapping("/rule/test")
    public AdminResponse<Boolean> testAlertRule(@RequestBody AlertRule rule) {
        boolean result = alertService.testAlertRule(rule);
        return AdminResponse.success(result);
    }

    /**
     * 批量处理告警
     */
    @PostMapping("/batch/handle")
    public AdminResponse<Void> batchHandleAlerts(
            @RequestBody List<Long> alertIds,
            @RequestParam String handler,
            @RequestParam(required = false) String remark) {
        alertService.batchHandleAlerts(alertIds, handler, remark);
        log.info("批量处理告警: {}, 处理人: {}", alertIds, handler);
        return AdminResponse.success();
    }
}