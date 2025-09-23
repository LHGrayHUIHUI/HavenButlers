package com.haven.storage.logging.controller;

import com.haven.base.logging.model.LogEvent;
import com.haven.storage.logging.service.LogEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 日志事件控制器
 * 接收来自各微服务的LogEvent日志事件，统一存储到数据库
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/logs")
@Tag(name = "日志事件API", description = "统一日志事件接收和存储")
public class LogEventController {

    @Autowired
    private LogEventService logEventService;

    // ================================
    // 单个日志事件接收
    // ================================

    /**
     * 接收操作日志事件
     */
    @PostMapping("/operation")
    @Operation(summary = "记录操作日志", description = "接收并存储操作日志事件")
    public ResponseEntity<String> logOperation(
            @Valid @RequestBody LogEvent logEvent,
            @Parameter(description = "请求追踪ID") @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        // 设置traceId（如果没有）
        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        // 异步处理
        logEventService.processOperationLog(logEvent);

        return ResponseEntity.ok("操作日志接收成功");
    }

    /**
     * 接收安全日志事件
     */
    @PostMapping("/security")
    @Operation(summary = "记录安全日志", description = "接收并存储安全日志事件")
    public ResponseEntity<String> logSecurity(
            @Valid @RequestBody LogEvent logEvent,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        logEventService.processSecurityLog(logEvent);
        return ResponseEntity.ok("安全日志接收成功");
    }

    /**
     * 接收性能日志事件
     */
    @PostMapping("/performance")
    @Operation(summary = "记录性能日志", description = "接收并存储性能日志事件")
    public ResponseEntity<String> logPerformance(
            @Valid @RequestBody LogEvent logEvent,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        logEventService.processPerformanceLog(logEvent);
        return ResponseEntity.ok("性能日志接收成功");
    }

    /**
     * 接收业务日志事件
     */
    @PostMapping("/business")
    @Operation(summary = "记录业务日志", description = "接收并存储业务日志事件")
    public ResponseEntity<String> logBusiness(
            @Valid @RequestBody LogEvent logEvent,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        logEventService.processBusinessLog(logEvent);
        return ResponseEntity.ok("业务日志接收成功");
    }

    /**
     * 接收错误日志事件
     */
    @PostMapping("/error")
    @Operation(summary = "记录错误日志", description = "接收并存储错误日志事件")
    public ResponseEntity<String> logError(
            @Valid @RequestBody LogEvent logEvent,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        logEventService.processErrorLog(logEvent);
        return ResponseEntity.ok("错误日志接收成功");
    }

    // ================================
    // 批量日志事件接收
    // ================================

    /**
     * 批量接收操作日志事件
     */
    @PostMapping("/operation/batch")
    @Operation(summary = "批量记录操作日志", description = "批量接收并存储操作日志事件")
    public ResponseEntity<String> batchLogOperation(
            @Valid @RequestBody List<LogEvent> logEvents,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        // 批量处理
        logEventService.processBatchOperationLogs(logEvents, traceId);
        return ResponseEntity.ok("批量操作日志接收成功，数量: " + logEvents.size());
    }

    /**
     * 批量接收安全日志事件
     */
    @PostMapping("/security/batch")
    @Operation(summary = "批量记录安全日志", description = "批量接收并存储安全日志事件")
    public ResponseEntity<String> batchLogSecurity(
            @Valid @RequestBody List<LogEvent> logEvents,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        logEventService.processBatchSecurityLogs(logEvents, traceId);
        return ResponseEntity.ok("批量安全日志接收成功，数量: " + logEvents.size());
    }

    /**
     * 批量接收性能日志事件
     */
    @PostMapping("/performance/batch")
    @Operation(summary = "批量记录性能日志", description = "批量接收并存储性能日志事件")
    public ResponseEntity<String> batchLogPerformance(
            @Valid @RequestBody List<LogEvent> logEvents,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        logEventService.processBatchPerformanceLogs(logEvents, traceId);
        return ResponseEntity.ok("批量性能日志接收成功，数量: " + logEvents.size());
    }

    // ================================
    // 统一日志事件接收
    // ================================

    /**
     * 统一日志事件接收接口
     * 根据logType自动分发到对应的处理器
     */
    @PostMapping("/unified")
    @Operation(summary = "统一日志接收", description = "根据日志类型自动分发处理")
    public ResponseEntity<String> logUnified(
            @Valid @RequestBody LogEvent logEvent,
            @RequestHeader(value = "Trace-ID", required = false) String traceId) {

        if (logEvent.getTraceId() == null && traceId != null) {
            logEvent.setTraceId(traceId);
        }

        // 根据日志类型分发
        switch (logEvent.getLogType()) {
            case OPERATION:
                logEventService.processOperationLog(logEvent);
                break;
            case SECURITY:
                logEventService.processSecurityLog(logEvent);
                break;
            case PERFORMANCE:
                logEventService.processPerformanceLog(logEvent);
                break;
            case BUSINESS:
                logEventService.processBusinessLog(logEvent);
                break;
            case ERROR:
                logEventService.processErrorLog(logEvent);
                break;
            case SYSTEM:
                // 系统日志当作操作日志处理
                logEventService.processOperationLog(logEvent);
                break;
            default:
                log.warn("未知的日志类型: {}", logEvent.getLogType());
                return ResponseEntity.badRequest().body("未知的日志类型");
        }

        return ResponseEntity.ok("统一日志接收成功");
    }

    // ================================
    // 健康检查和状态
    // ================================

    /**
     * 日志系统健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "日志系统健康检查", description = "检查日志系统是否正常运行")
    public ResponseEntity<String> health() {
        try {
            // 检查日志处理队列状态
            boolean isHealthy = logEventService.isHealthy();
            if (isHealthy) {
                return ResponseEntity.ok("日志系统运行正常");
            } else {
                return ResponseEntity.status(503).body("日志系统存在问题");
            }
        } catch (Exception e) {
            log.error("日志系统健康检查失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("日志系统健康检查失败");
        }
    }

    /**
     * 获取日志处理统计
     */
    @GetMapping("/stats")
    @Operation(summary = "获取日志处理统计", description = "获取日志处理的实时统计信息")
    public ResponseEntity<Object> getStats() {
        try {
            Object stats = logEventService.getProcessingStats();
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("获取日志统计失败: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body("获取统计信息失败");
        }
    }
}