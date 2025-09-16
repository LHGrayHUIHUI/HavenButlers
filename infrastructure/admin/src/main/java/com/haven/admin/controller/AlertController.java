package com.haven.admin.controller;

import com.haven.admin.model.Alert;
import com.haven.admin.model.AlertRule;
import com.haven.admin.service.AlertService;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.model.dto.PageRequest;
import com.haven.base.model.dto.PageResponse;
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
    public ResponseWrapper<PageResponse<Alert>> getAlertList(
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
        return ResponseWrapper.success(alerts);
    }

    /**
     * 获取告警详情
     */
    @GetMapping("/{alertId}")
    public ResponseWrapper<Alert> getAlertDetail(@PathVariable Long alertId) {
        Alert alert = alertService.getAlertDetail(alertId);
        return ResponseWrapper.success(alert);
    }

    /**
     * 处理告警
     */
    @PostMapping("/{alertId}/handle")
    public ResponseWrapper<Void> handleAlert(
            @PathVariable Long alertId,
            @RequestParam String handler,
            @RequestParam(required = false) String remark) {
        alertService.handleAlert(alertId, handler, remark);
        log.info("处理告警: {}, 处理人: {}", alertId, handler);
        return ResponseWrapper.success();
    }

    /**
     * 忽略告警
     */
    @PostMapping("/{alertId}/ignore")
    public ResponseWrapper<Void> ignoreAlert(
            @PathVariable Long alertId,
            @RequestParam String reason) {
        alertService.ignoreAlert(alertId, reason);
        log.info("忽略告警: {}, 原因: {}", alertId, reason);
        return ResponseWrapper.success();
    }

    /**
     * 获取告警规则列表
     */
    @GetMapping("/rules")
    public ResponseWrapper<List<AlertRule>> getAlertRules(
            @RequestParam(required = false) String serviceName,
            @RequestParam(required = false) Boolean enabled) {
        List<AlertRule> rules = alertService.getAlertRules(serviceName, enabled);
        return ResponseWrapper.success(rules);
    }

    /**
     * 创建告警规则
     */
    @PostMapping("/rule")
    public ResponseWrapper<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        AlertRule createdRule = alertService.createAlertRule(rule);
        log.info("创建告警规则: {}", createdRule.getName());
        return ResponseWrapper.success(createdRule);
    }

    /**
     * 更新告警规则
     */
    @PutMapping("/rule/{ruleId}")
    public ResponseWrapper<Void> updateAlertRule(
            @PathVariable Long ruleId,
            @RequestBody AlertRule rule) {
        rule.setId(ruleId);
        alertService.updateAlertRule(rule);
        log.info("更新告警规则: {}", ruleId);
        return ResponseWrapper.success();
    }

    /**
     * 删除告警规则
     */
    @DeleteMapping("/rule/{ruleId}")
    public ResponseWrapper<Void> deleteAlertRule(@PathVariable Long ruleId) {
        alertService.deleteAlertRule(ruleId);
        log.info("删除告警规则: {}", ruleId);
        return ResponseWrapper.success();
    }

    /**
     * 启用/禁用告警规则
     */
    @PutMapping("/rule/{ruleId}/enable")
    public ResponseWrapper<Void> enableAlertRule(
            @PathVariable Long ruleId,
            @RequestParam Boolean enabled) {
        alertService.enableAlertRule(ruleId, enabled);
        log.info("{}告警规则: {}", enabled ? "启用" : "禁用", ruleId);
        return ResponseWrapper.success();
    }

    /**
     * 获取告警统计
     */
    @GetMapping("/statistics")
    public ResponseWrapper<Map<String, Object>> getAlertStatistics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        Map<String, Object> statistics = alertService.getAlertStatistics(startTime, endTime);
        return ResponseWrapper.success(statistics);
    }

    /**
     * 测试告警规则
     */
    @PostMapping("/rule/test")
    public ResponseWrapper<Boolean> testAlertRule(@RequestBody AlertRule rule) {
        boolean result = alertService.testAlertRule(rule);
        return ResponseWrapper.success(result);
    }

    /**
     * 批量处理告警
     */
    @PostMapping("/batch/handle")
    public ResponseWrapper<Void> batchHandleAlerts(
            @RequestBody List<Long> alertIds,
            @RequestParam String handler,
            @RequestParam(required = false) String remark) {
        alertService.batchHandleAlerts(alertIds, handler, remark);
        log.info("批量处理告警: {}, 处理人: {}", alertIds, handler);
        return ResponseWrapper.success();
    }
}