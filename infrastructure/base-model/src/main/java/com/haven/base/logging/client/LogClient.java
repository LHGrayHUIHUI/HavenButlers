package com.haven.base.logging.client;

import com.haven.base.config.DynamicServiceConfig;
import com.haven.base.logging.model.LogEvent;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * 统一日志客户端
 * 供所有微服务使用，负责将日志发送到storage-service
 *
 * 使用方式：
 * 1. 注入LogClient
 * 2. 调用相应的日志记录方法
 * 3. 自动异步发送到storage-service
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class LogClient {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private DynamicServiceConfig serviceConfig;

    @Value("${spring.application.name:unknown-service}")
    private String currentServiceName;

    // ================================
    // 操作日志记录方法
    // ================================

    /**
     * 记录用户操作日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logOperation(String familyId, String userId,
                                              String operationType, String description) {
        LogEvent logEvent = LogEvent.createOperationLog(
            currentServiceName, familyId, userId, operationType, description
        );
        return sendLogToStorage(logEvent, "operation");
    }

    /**
     * 记录操作日志（含执行时间）
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logOperationWithTiming(String familyId, String userId,
                                                         String operationType, String description,
                                                         long startTime) {
        LogEvent logEvent = LogEvent.createOperationLog(
            currentServiceName, familyId, userId, operationType, description
        ).withExecutionTime(startTime);

        return sendLogToStorage(logEvent, "operation");
    }

    /**
     * 记录操作结果
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logOperationResult(String familyId, String userId,
                                                     String operationType, String description,
                                                     LogEvent.ResultStatus status, String errorMessage) {
        LogEvent logEvent = LogEvent.createOperationLog(
            currentServiceName, familyId, userId, operationType, description
        ).withResult(status, errorMessage);

        return sendLogToStorage(logEvent, "operation");
    }

    // ================================
    // 安全日志记录方法
    // ================================

    /**
     * 记录安全事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logSecurityEvent(String familyId, String clientIP,
                                                   String eventType, LogEvent.RiskLevel riskLevel,
                                                   String description) {
        LogEvent logEvent = LogEvent.createSecurityLog(
            currentServiceName, familyId, clientIP, eventType, riskLevel, description
        );
        return sendLogToStorage(logEvent, "security");
    }

    /**
     * 记录认证失败事件
     */
    public CompletableFuture<Void> logAuthFailure(String familyId, String clientIP, String userId, String reason) {
        return logSecurityEvent(familyId, clientIP, "AUTH_FAILED", LogEvent.RiskLevel.MEDIUM,
                              "用户认证失败: " + reason)
                .thenCompose(v -> logOperation(familyId, userId, "AUTH_FAILED", reason));
    }

    /**
     * 记录访问拒绝事件
     */
    public CompletableFuture<Void> logAccessDenied(String familyId, String clientIP, String userId, String resource) {
        return logSecurityEvent(familyId, clientIP, "ACCESS_DENIED", LogEvent.RiskLevel.HIGH,
                              "访问被拒绝: " + resource)
                .thenCompose(v -> logOperation(familyId, userId, "ACCESS_DENIED", "访问资源: " + resource));
    }

    /**
     * 记录危险操作被拦截
     */
    public CompletableFuture<Void> logDangerousOperationBlocked(String familyId, String clientIP,
                                                               String userId, String operation) {
        return logSecurityEvent(familyId, clientIP, "DANGEROUS_OPERATION_BLOCKED", LogEvent.RiskLevel.CRITICAL,
                              "危险操作被拦截: " + operation)
                .thenCompose(v -> logOperation(familyId, userId, "OPERATION_BLOCKED", operation));
    }

    // ================================
    // 性能日志记录方法
    // ================================

    /**
     * 记录性能指标
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logPerformanceMetric(String metricName, Double metricValue, String metricUnit) {
        LogEvent logEvent = LogEvent.createPerformanceLog(
            currentServiceName, metricName, metricValue, metricUnit
        );
        return sendLogToStorage(logEvent, "performance");
    }

    /**
     * 记录响应时间
     */
    public CompletableFuture<Void> logResponseTime(String operationType, long responseTimeMs) {
        return logPerformanceMetric("response_time_" + operationType, (double) responseTimeMs, "ms");
    }

    /**
     * 记录QPS
     */
    public CompletableFuture<Void> logQPS(String operationType, int requestCount) {
        return logPerformanceMetric("qps_" + operationType, (double) requestCount, "requests/sec");
    }

    /**
     * 记录错误率
     */
    public CompletableFuture<Void> logErrorRate(String operationType, double errorRate) {
        return logPerformanceMetric("error_rate_" + operationType, errorRate, "percent");
    }

    // ================================
    // 业务日志记录方法
    // ================================

    /**
     * 记录业务日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logBusiness(String familyId, String userId, String businessModule,
                                             String scenario, String description) {
        LogEvent logEvent = LogEvent.createBusinessLog(
            currentServiceName, familyId, userId, businessModule, scenario, description
        );
        return sendLogToStorage(logEvent, "business");
    }

    /**
     * 记录设备操作
     */
    public CompletableFuture<Void> logDeviceOperation(String familyId, String userId, String deviceId,
                                                     String operation, String result) {
        return logBusiness(familyId, userId, "DEVICE_CONTROL", operation,
                         "设备ID: " + deviceId + ", 操作: " + operation + ", 结果: " + result);
    }

    /**
     * 记录文件操作
     */
    public CompletableFuture<Void> logFileOperation(String familyId, String userId, String fileName,
                                                   String operation, String result) {
        return logBusiness(familyId, userId, "FILE_MANAGEMENT", operation,
                         "文件: " + fileName + ", 操作: " + operation + ", 结果: " + result);
    }

    // ================================
    // 错误日志记录方法
    // ================================

    /**
     * 记录错误日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> logError(String familyId, String userId, String operationType,
                                          String errorCode, String errorMessage) {
        LogEvent logEvent = LogEvent.createErrorLog(
            currentServiceName, familyId, userId, operationType, errorCode, errorMessage
        );
        return sendLogToStorage(logEvent, "error");
    }

    /**
     * 记录异常日志
     */
    public CompletableFuture<Void> logException(String familyId, String userId, String operationType,
                                              Exception exception) {
        String errorMessage = exception.getMessage();
        String errorCode = exception.getClass().getSimpleName();

        return logError(familyId, userId, operationType, errorCode, errorMessage);
    }

    // ================================
    // 便捷方法
    // ================================

    /**
     * 记录简单操作（自动获取traceId）
     */
    public CompletableFuture<Void> log(String familyId, String userId, String operation, String description) {
        return logOperation(familyId, userId, operation, description);
    }

    /**
     * 记录带链路追踪的操作
     */
    public CompletableFuture<Void> logWithTrace(String familyId, String userId, String operation,
                                               String description, String customTraceId) {
        LogEvent logEvent = LogEvent.createOperationLog(
            currentServiceName, familyId, userId, operation, description
        );
        logEvent.setTraceId(customTraceId);

        return sendLogToStorage(logEvent, "operation");
    }

    // ================================
    // 核心发送方法
    // ================================

    /**
     * 发送自定义日志事件到storage-service（公开方法）
     */
    public CompletableFuture<Void> sendCustomLog(LogEvent logEvent, String logType) {
        return sendLogToStorage(logEvent, logType);
    }

    /**
     * 发送日志到storage-service
     */
    private CompletableFuture<Void> sendLogToStorage(LogEvent logEvent, String logType) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 确保有traceId
                if (logEvent.getTraceId() == null) {
                    logEvent.setTraceId(TraceIdUtil.getCurrentOrGenerate());
                }

                // 确保有时间戳
                if (logEvent.getTimestamp() == null) {
                    logEvent.setTimestamp(LocalDateTime.now());
                }

                // 从动态配置获取storage-service地址
                String storageServiceUrl = serviceConfig.getStorage().getUrl();
                String url = storageServiceUrl + "/api/v1/logs/" + logType;
                restTemplate.postForObject(url, logEvent, String.class);

                log.debug("日志发送成功: {} - {}", logType, logEvent.getOperationType());

            } catch (Exception e) {
                log.error("发送日志到storage-service失败: {}", e.getMessage(), e);
                // 不抛出异常，避免影响业务流程
            }
        });
    }

    /**
     * 批量发送日志
     */
    @Async("logExecutor")
    public CompletableFuture<Void> batchSendLogs(java.util.List<LogEvent> logEvents, String logType) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 批量处理日志
                for (LogEvent logEvent : logEvents) {
                    if (logEvent.getTraceId() == null) {
                        logEvent.setTraceId(TraceIdUtil.getCurrentOrGenerate());
                    }
                    if (logEvent.getTimestamp() == null) {
                        logEvent.setTimestamp(LocalDateTime.now());
                    }
                }

                // 从动态配置获取storage-service地址
                String storageServiceUrl = serviceConfig.getStorage().getUrl();
                String url = storageServiceUrl + "/api/v1/logs/" + logType + "/batch";
                restTemplate.postForObject(url, logEvents, String.class);

                log.debug("批量日志发送成功: {} 条记录", logEvents.size());

            } catch (Exception e) {
                log.error("批量发送日志失败: {}", e.getMessage(), e);
            }
        });
    }

    // ================================
    // 工具方法
    // ================================

    /**
     * 获取当前服务名称
     */
    public String getCurrentServiceName() {
        return currentServiceName;
    }

    /**
     * 设置storage-service地址（用于测试）
     */
    public void setStorageServiceUrl(String url) {
        serviceConfig.getStorage().setUrl(url);
    }
}