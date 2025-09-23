package com.haven.storage.logging.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.haven.base.logging.model.LogEvent;
import com.haven.storage.logging.entity.OperationLog;
import com.haven.storage.logging.entity.PerformanceLog;
import com.haven.storage.logging.entity.SecurityLog;
import com.haven.storage.logging.repository.OperationLogRepository;
import com.haven.storage.logging.repository.PerformanceLogRepository;
import com.haven.storage.logging.repository.SecurityLogRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 日志事件处理服务
 * 将来自base-model的LogEvent转换为storage-service的实体并存储
 *
 * @author HavenButler
 */
@Slf4j
@Service
public class LogEventService {

    @Autowired
    private OperationLogRepository operationLogRepository;

    @Autowired
    private SecurityLogRepository securityLogRepository;

    @Autowired
    private PerformanceLogRepository performanceLogRepository;

    @Autowired
    private ObjectMapper objectMapper;

    // 处理统计计数器
    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong operationLogCount = new AtomicLong(0);
    private final AtomicLong securityLogCount = new AtomicLong(0);
    private final AtomicLong performanceLogCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    // ================================
    // 单个日志事件处理
    // ================================

    /**
     * 处理操作日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processOperationLog(LogEvent logEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                OperationLog operationLog = convertToOperationLog(logEvent);
                operationLogRepository.save(operationLog);

                operationLogCount.incrementAndGet();
                totalProcessed.incrementAndGet();

                log.debug("操作日志处理成功: {}", logEvent.getOperationType());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("处理操作日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 处理安全日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processSecurityLog(LogEvent logEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                SecurityLog securityLog = convertToSecurityLog(logEvent);
                securityLogRepository.save(securityLog);

                securityLogCount.incrementAndGet();
                totalProcessed.incrementAndGet();

                log.debug("安全日志处理成功: {}", logEvent.getSecurityEventType());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("处理安全日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 处理性能日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processPerformanceLog(LogEvent logEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                PerformanceLog performanceLog = convertToPerformanceLog(logEvent);
                performanceLogRepository.save(performanceLog);

                performanceLogCount.incrementAndGet();
                totalProcessed.incrementAndGet();

                log.debug("性能日志处理成功: {}", logEvent.getMetricName());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("处理性能日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 处理业务日志事件（存储为操作日志）
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processBusinessLog(LogEvent logEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                // 业务日志转换为操作日志，添加业务相关元数据
                OperationLog operationLog = convertToOperationLog(logEvent);

                // 添加业务相关字段到元数据
                Map<String, Object> metadata = parseMetadata(operationLog.getMetadata());
                if (logEvent.getBusinessModule() != null) {
                    metadata.put("businessModule", logEvent.getBusinessModule());
                }
                if (logEvent.getBusinessScenario() != null) {
                    metadata.put("businessScenario", logEvent.getBusinessScenario());
                }
                if (logEvent.getAffectedRecords() != null) {
                    metadata.put("affectedRecords", logEvent.getAffectedRecords());
                }

                operationLog.setMetadata(serializeMetadata(metadata));
                operationLogRepository.save(operationLog);

                operationLogCount.incrementAndGet();
                totalProcessed.incrementAndGet();

                log.debug("业务日志处理成功: {}", logEvent.getOperationType());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("处理业务日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 处理错误日志事件（存储为操作日志）
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processErrorLog(LogEvent logEvent) {
        return CompletableFuture.runAsync(() -> {
            try {
                OperationLog operationLog = convertToOperationLog(logEvent);

                // 确保结果状态为失败
                operationLog.setResultStatus("FAILED");

                operationLogRepository.save(operationLog);

                operationLogCount.incrementAndGet();
                totalProcessed.incrementAndGet();

                log.debug("错误日志处理成功: {}", logEvent.getOperationType());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("处理错误日志失败: {}", e.getMessage(), e);
            }
        });
    }

    // ================================
    // 批量日志事件处理
    // ================================

    /**
     * 批量处理操作日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processBatchOperationLogs(List<LogEvent> logEvents, String traceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<OperationLog> operationLogs = logEvents.stream()
                    .map(this::convertToOperationLog)
                    .peek(log -> {
                        if (log.getTraceId() == null && traceId != null) {
                            log.setTraceId(traceId);
                        }
                    })
                    .collect(Collectors.toList());

                operationLogRepository.saveAll(operationLogs);

                operationLogCount.addAndGet(operationLogs.size());
                totalProcessed.addAndGet(operationLogs.size());

                log.debug("批量操作日志处理成功，数量: {}", operationLogs.size());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("批量处理操作日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 批量处理安全日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processBatchSecurityLogs(List<LogEvent> logEvents, String traceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<SecurityLog> securityLogs = logEvents.stream()
                    .map(this::convertToSecurityLog)
                    .peek(log -> {
                        if (log.getTraceId() == null && traceId != null) {
                            log.setTraceId(traceId);
                        }
                    })
                    .collect(Collectors.toList());

                securityLogRepository.saveAll(securityLogs);

                securityLogCount.addAndGet(securityLogs.size());
                totalProcessed.addAndGet(securityLogs.size());

                log.debug("批量安全日志处理成功，数量: {}", securityLogs.size());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("批量处理安全日志失败: {}", e.getMessage(), e);
            }
        });
    }

    /**
     * 批量处理性能日志事件
     */
    @Async("logExecutor")
    public CompletableFuture<Void> processBatchPerformanceLogs(List<LogEvent> logEvents, String traceId) {
        return CompletableFuture.runAsync(() -> {
            try {
                List<PerformanceLog> performanceLogs = logEvents.stream()
                    .map(this::convertToPerformanceLog)
                    .peek(log -> {
                        if (log.getTraceId() == null && traceId != null) {
                            log.setTraceId(traceId);
                        }
                    })
                    .collect(Collectors.toList());

                performanceLogRepository.saveAll(performanceLogs);

                performanceLogCount.addAndGet(performanceLogs.size());
                totalProcessed.addAndGet(performanceLogs.size());

                log.debug("批量性能日志处理成功，数量: {}", performanceLogs.size());

            } catch (Exception e) {
                errorCount.incrementAndGet();
                log.error("批量处理性能日志失败: {}", e.getMessage(), e);
            }
        });
    }

    // ================================
    // 转换方法
    // ================================

    /**
     * 将LogEvent转换为OperationLog
     */
    private OperationLog convertToOperationLog(LogEvent logEvent) {
        OperationLog operationLog = new OperationLog();

        // 基础字段
        operationLog.setFamilyId(logEvent.getFamilyId());
        operationLog.setServiceType(determineServiceType(logEvent.getServiceName()));
        operationLog.setClientIP(logEvent.getClientIP());
        operationLog.setUserId(logEvent.getUserId());
        operationLog.setOperationType(logEvent.getOperationType());
        operationLog.setOperationContent(logEvent.getOperationDescription());
        operationLog.setExecutionTimeMs(logEvent.getExecutionTimeMs());
        operationLog.setResultStatus(convertResultStatus(logEvent.getResultStatus()));
        operationLog.setErrorMessage(logEvent.getErrorMessage());
        operationLog.setTraceId(logEvent.getTraceId());
        operationLog.setSessionId(logEvent.getSessionId());
        operationLog.setCreatedAt(logEvent.getTimestamp() != null ?
            logEvent.getTimestamp() : LocalDateTime.now());

        // 元数据
        Map<String, Object> metadata = new HashMap<>();
        if (logEvent.getMetadata() != null) {
            metadata.putAll(logEvent.getMetadata());
        }
        if (logEvent.getTags() != null) {
            metadata.put("tags", logEvent.getTags());
        }
        if (logEvent.getRequestParams() != null) {
            metadata.put("requestParams", logEvent.getRequestParams());
        }
        if (logEvent.getResponseData() != null) {
            metadata.put("responseData", logEvent.getResponseData());
        }

        operationLog.setMetadata(serializeMetadata(metadata));

        return operationLog;
    }

    /**
     * 将LogEvent转换为SecurityLog
     */
    private SecurityLog convertToSecurityLog(LogEvent logEvent) {
        SecurityLog securityLog = new SecurityLog();

        securityLog.setFamilyId(logEvent.getFamilyId());
        securityLog.setClientIP(logEvent.getClientIP());
        securityLog.setEventType(logEvent.getSecurityEventType() != null ?
            logEvent.getSecurityEventType() : logEvent.getOperationType());
        securityLog.setServiceType(determineServiceType(logEvent.getServiceName()));
        securityLog.setEventDetails(logEvent.getOperationDescription());
        securityLog.setRiskLevel(convertRiskLevel(logEvent.getRiskLevel()));
        securityLog.setUserId(logEvent.getUserId());
        securityLog.setUserAgent(logEvent.getUserAgent());
        securityLog.setGeoLocation(logEvent.getGeoLocation());
        securityLog.setCreatedAt(logEvent.getTimestamp() != null ?
            logEvent.getTimestamp() : LocalDateTime.now());
        securityLog.setTraceId(logEvent.getTraceId());

        return securityLog;
    }

    /**
     * 将LogEvent转换为PerformanceLog
     */
    private PerformanceLog convertToPerformanceLog(LogEvent logEvent) {
        PerformanceLog performanceLog = new PerformanceLog();

        performanceLog.setServiceType(determineServiceType(logEvent.getServiceName()));
        performanceLog.setMetricName(logEvent.getMetricName());
        performanceLog.setMetricValue(logEvent.getMetricValue());
        performanceLog.setMetricUnit(logEvent.getMetricUnit());
        performanceLog.setIsThresholdExceeded(logEvent.getThresholdExceeded());
        performanceLog.setCreatedAt(logEvent.getTimestamp() != null ?
            logEvent.getTimestamp() : LocalDateTime.now());

        // 从元数据中提取实例信息
        if (logEvent.getMetadata() != null) {
            Object instanceId = logEvent.getMetadata().get("instanceId");
            if (instanceId != null) {
                performanceLog.setInstanceId(instanceId.toString());
            }

            Object hostname = logEvent.getMetadata().get("hostname");
            if (hostname != null) {
                performanceLog.setHostname(hostname.toString());
            }
        }

        // 标签信息
        if (logEvent.getTags() != null) {
            performanceLog.setTags(serializeMetadata(logEvent.getTags()));
        }

        return performanceLog;
    }

    // ================================
    // 辅助方法
    // ================================

    /**
     * 确定服务类型
     */
    private String determineServiceType(String serviceName) {
        if (serviceName == null) return "UNKNOWN";

        // 根据服务名称映射到服务类型
        if (serviceName.contains("account")) return "HTTP_API";
        if (serviceName.contains("storage")) return "STORAGE_API";
        if (serviceName.contains("ai")) return "HTTP_API";
        if (serviceName.contains("nlp")) return "HTTP_API";
        if (serviceName.contains("message")) return "HTTP_API";
        if (serviceName.contains("file")) return "HTTP_API";

        return "HTTP_API";
    }

    /**
     * 转换结果状态
     */
    private String convertResultStatus(LogEvent.ResultStatus resultStatus) {
        if (resultStatus == null) return "SUCCESS";
        return resultStatus.name();
    }

    /**
     * 转换风险级别
     */
    private String convertRiskLevel(LogEvent.RiskLevel riskLevel) {
        if (riskLevel == null) return "LOW";
        return riskLevel.name();
    }

    /**
     * 序列化元数据
     */
    private String serializeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            log.error("序列化元数据失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析元数据
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isEmpty()) {
            return new HashMap<>();
        }

        try {
            return objectMapper.readValue(metadataJson, Map.class);
        } catch (JsonProcessingException e) {
            log.error("解析元数据失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    // ================================
    // 健康检查和统计
    // ================================

    /**
     * 检查日志系统是否健康
     */
    public boolean isHealthy() {
        try {
            // 检查数据库连接
            operationLogRepository.count();
            securityLogRepository.count();
            performanceLogRepository.count();
            return true;
        } catch (Exception e) {
            log.error("日志系统健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取处理统计信息
     */
    public Map<String, Object> getProcessingStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("operationLogCount", operationLogCount.get());
        stats.put("securityLogCount", securityLogCount.get());
        stats.put("performanceLogCount", performanceLogCount.get());
        stats.put("errorCount", errorCount.get());
        stats.put("timestamp", LocalDateTime.now());
        return stats;
    }

    /**
     * 重置统计计数器
     */
    public void resetStats() {
        totalProcessed.set(0);
        operationLogCount.set(0);
        securityLogCount.set(0);
        performanceLogCount.set(0);
        errorCount.set(0);
    }
}