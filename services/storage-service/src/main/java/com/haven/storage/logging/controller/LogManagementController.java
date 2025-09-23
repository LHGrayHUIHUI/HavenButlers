package com.haven.storage.logging.controller;

import com.haven.storage.logging.OperationLogService;
import com.haven.storage.logging.entity.OperationLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 * 为其他微服务和管理界面提供日志查询接口
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
@RequiredArgsConstructor
public class LogManagementController {

    private final OperationLogService operationLogService;

    /**
     * 查询操作日志
     * GET /api/v1/logs/operations?familyId=xxx&serviceType=POSTGRESQL&page=0&size=20
     */
    @GetMapping("/operations")
    public ResponseEntity<Page<OperationLog>> queryOperationLogs(
            @RequestParam(required = false) String familyId,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String clientIP,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<OperationLog> logs = operationLogService.queryOperationLogs(
            familyId, serviceType, clientIP, startTime, endTime, page, size);

        return ResponseEntity.ok(logs);
    }

    /**
     * 获取性能统计数据
     * GET /api/v1/logs/performance/stats?serviceType=POSTGRESQL&metricName=response_time
     */
    @GetMapping("/performance/stats")
    public ResponseEntity<List<Map<String, Object>>> getPerformanceStats(
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) String metricName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        // 如果没有指定时间范围，默认查询最近24小时
        if (startTime == null) {
            startTime = LocalDateTime.now().minusHours(24);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<Map<String, Object>> stats = operationLogService.getPerformanceStats(
            serviceType, metricName, startTime, endTime);

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取安全事件统计
     * GET /api/v1/logs/security/stats?familyId=xxx
     */
    @GetMapping("/security/stats")
    public ResponseEntity<List<Map<String, Object>>> getSecurityStats(
            @RequestParam(required = false) String familyId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        // 默认查询最近7天的安全事件
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(7);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        List<Map<String, Object>> stats = operationLogService.getSecurityStats(
            familyId, startTime, endTime);

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取实时监控指标
     * GET /api/v1/logs/metrics/realtime
     */
    @GetMapping("/metrics/realtime")
    public ResponseEntity<Map<String, Object>> getRealTimeMetrics() {
        Map<String, Object> metrics = operationLogService.getRealTimeMetrics();
        return ResponseEntity.ok(metrics);
    }

    /**
     * 获取日志概览统计
     * GET /api/v1/logs/overview?familyId=xxx
     */
    @GetMapping("/overview")
    public ResponseEntity<Map<String, Object>> getLogOverview(
            @RequestParam String familyId) {

        Map<String, Object> overview = operationLogService.getLogOverview(familyId);
        return ResponseEntity.ok(overview);
    }

    /**
     * 手动触发日志清理
     * POST /api/v1/logs/cleanup?retentionDays=30
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, String>> cleanupLogs(
            @RequestParam(defaultValue = "30") int retentionDays) {

        try {
            operationLogService.cleanupExpiredLogs(retentionDays);
            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "日志清理任务已启动，保留 " + retentionDays + " 天内的日志"
            ));
        } catch (Exception e) {
            log.error("手动清理日志失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "日志清理失败: " + e.getMessage()
                ));
        }
    }

    /**
     * 导出日志数据
     * GET /api/v1/logs/export?familyId=xxx&format=csv
     */
    @GetMapping("/export")
    public ResponseEntity<String> exportLogs(
            @RequestParam String familyId,
            @RequestParam(required = false) String serviceType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "csv") String format) {

        // 默认导出最近30天的数据
        if (startTime == null) {
            startTime = LocalDateTime.now().minusDays(30);
        }
        if (endTime == null) {
            endTime = LocalDateTime.now();
        }

        String exportData = operationLogService.exportLogs(
            familyId, serviceType, startTime, endTime, format);

        return ResponseEntity.ok(exportData);
    }

    /**
     * 记录API调用日志
     * POST /api/v1/logs/api-call
     */
    @PostMapping("/api-call")
    public ResponseEntity<Map<String, String>> logApiCall(
            @RequestBody Map<String, Object> logData) {

        try {
            // 从请求体中提取日志信息
            String familyId = (String) logData.get("familyId");
            String serviceType = (String) logData.getOrDefault("serviceType", "HTTP_API");
            String clientIP = (String) logData.get("clientIP");
            String userId = (String) logData.get("userId");
            String operationType = (String) logData.get("operationType");
            String operationContent = (String) logData.get("operationContent");
            Integer executionTimeMs = (Integer) logData.get("executionTimeMs");
            String resultStatus = (String) logData.getOrDefault("resultStatus", "SUCCESS");
            String errorMessage = (String) logData.get("errorMessage");

            // 异步记录日志
            operationLogService.logOperation(
                familyId, serviceType, clientIP, userId, operationType,
                null, null, operationContent, executionTimeMs, resultStatus, errorMessage
            );

            return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "日志记录成功"
            ));

        } catch (Exception e) {
            log.error("记录API调用日志失败", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "error",
                    "message", "日志记录失败: " + e.getMessage()
                ));
        }
    }

    /**
     * 获取服务健康状态
     * GET /api/v1/logs/health
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getLogServiceHealth() {
        try {
            Map<String, Object> health = Map.of(
                "status", "UP",
                "service", "log-management",
                "timestamp", LocalDateTime.now().toString(),
                "features", List.of(
                    "操作日志记录", "性能监控", "安全审计",
                    "实时查询", "日志导出", "自动清理"
                )
            );

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "status", "DOWN",
                    "error", e.getMessage()
                ));
        }
    }
}