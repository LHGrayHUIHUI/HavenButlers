package com.haven.base.logging.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 统一日志事件基础类
 * 所有微服务的日志都基于此模型
 *
 * 设计原则：
 * 1. 统一的数据格式，便于后续分析
 * 2. 可扩展的元数据字段
 * 3. 自动的链路追踪集成
 * 4. 敏感信息保护
 *
 * @author HavenButler
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LogEvent {

    /**
     * 日志类型枚举
     */
    public enum LogType {
        OPERATION,    // 操作日志
        SECURITY,     // 安全日志
        PERFORMANCE,  // 性能日志
        BUSINESS,     // 业务日志
        SYSTEM,       // 系统日志
        ERROR         // 错误日志
    }

    /**
     * 风险级别枚举
     */
    public enum RiskLevel {
        LOW,          // 低风险
        MEDIUM,       // 中风险
        HIGH,         // 高风险
        CRITICAL      // 严重风险
    }

    /**
     * 操作结果枚举
     */
    public enum ResultStatus {
        SUCCESS,      // 成功
        FAILED,       // 失败
        BLOCKED,      // 被拦截
        IN_PROGRESS,  // 进行中
        TIMEOUT,      // 超时
        CANCELLED     // 取消
    }

    // ================================
    // 基础字段 (所有日志类型必需)
    // ================================

    /**
     * 日志类型
     */
    private LogType logType;

    /**
     * 服务名称 (如: account-service, ai-service)
     */
    private String serviceName;

    /**
     * 家庭ID (数据隔离的核心字段)
     */
    private String familyId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 客户端IP地址
     */
    private String clientIP;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 创建时间
     */
    private LocalDateTime timestamp;

    // ================================
    // 操作相关字段
    // ================================

    /**
     * 操作类型 (如: USER_LOGIN, FILE_UPLOAD, DATABASE_QUERY)
     */
    private String operationType;

    /**
     * 操作描述
     */
    private String operationDescription;

    /**
     * 操作内容 (脱敏后的详细信息)
     */
    private String operationContent;

    /**
     * 操作目标 (如: 数据库名、文件名、接口名)
     */
    private String operationTarget;

    /**
     * 执行时间 (毫秒)
     */
    private Long executionTimeMs;

    /**
     * 操作结果状态
     */
    private ResultStatus resultStatus;

    /**
     * 错误消息
     */
    private String errorMessage;

    /**
     * 错误码
     */
    private String errorCode;

    // ================================
    // 安全相关字段
    // ================================

    /**
     * 风险级别
     */
    private RiskLevel riskLevel;

    /**
     * 安全事件类型
     */
    private String securityEventType;

    /**
     * 用户代理信息
     */
    private String userAgent;

    /**
     * 地理位置信息
     */
    private String geoLocation;

    // ================================
    // 性能相关字段
    // ================================

    /**
     * 性能指标名称
     */
    private String metricName;

    /**
     * 性能指标值
     */
    private Double metricValue;

    /**
     * 性能指标单位
     */
    private String metricUnit;

    /**
     * 是否超过阈值
     */
    private Boolean thresholdExceeded;

    // ================================
    // 业务相关字段
    // ================================

    /**
     * 业务模块
     */
    private String businessModule;

    /**
     * 业务场景
     */
    private String businessScenario;

    /**
     * 影响的记录数
     */
    private Integer affectedRecords;

    // ================================
    // 扩展字段
    // ================================

    /**
     * 扩展元数据 (JSON格式，用于存储特定业务的额外信息)
     */
    private Map<String, Object> metadata;

    /**
     * 标签信息 (用于分类和过滤)
     */
    private Map<String, String> tags;

    /**
     * 请求参数 (脱敏后)
     */
    private String requestParams;

    /**
     * 响应数据 (脱敏后)
     */
    private String responseData;

    // ================================
    // 便捷的静态构建方法
    // ================================

    /**
     * 创建操作日志
     */
    public static LogEvent createOperationLog(String serviceName, String familyId, String userId,
                                            String operationType, String description) {
        return LogEvent.builder()
            .logType(LogType.OPERATION)
            .serviceName(serviceName)
            .familyId(familyId)
            .userId(userId)
            .operationType(operationType)
            .operationDescription(description)
            .timestamp(LocalDateTime.now())
            .traceId(com.haven.base.utils.TraceIdUtil.getCurrentOrGenerate())
            .build();
    }

    /**
     * 创建安全日志
     */
    public static LogEvent createSecurityLog(String serviceName, String familyId, String clientIP,
                                           String eventType, RiskLevel riskLevel, String description) {
        return LogEvent.builder()
            .logType(LogType.SECURITY)
            .serviceName(serviceName)
            .familyId(familyId)
            .clientIP(clientIP)
            .securityEventType(eventType)
            .riskLevel(riskLevel)
            .operationDescription(description)
            .timestamp(LocalDateTime.now())
            .traceId(com.haven.base.utils.TraceIdUtil.getCurrentOrGenerate())
            .build();
    }

    /**
     * 创建性能日志
     */
    public static LogEvent createPerformanceLog(String serviceName, String metricName,
                                              Double metricValue, String metricUnit) {
        return LogEvent.builder()
            .logType(LogType.PERFORMANCE)
            .serviceName(serviceName)
            .metricName(metricName)
            .metricValue(metricValue)
            .metricUnit(metricUnit)
            .timestamp(LocalDateTime.now())
            .traceId(com.haven.base.utils.TraceIdUtil.getCurrentOrGenerate())
            .build();
    }

    /**
     * 创建业务日志
     */
    public static LogEvent createBusinessLog(String serviceName, String familyId, String userId,
                                           String businessModule, String scenario, String description) {
        return LogEvent.builder()
            .logType(LogType.BUSINESS)
            .serviceName(serviceName)
            .familyId(familyId)
            .userId(userId)
            .businessModule(businessModule)
            .businessScenario(scenario)
            .operationDescription(description)
            .timestamp(LocalDateTime.now())
            .traceId(com.haven.base.utils.TraceIdUtil.getCurrentOrGenerate())
            .build();
    }

    /**
     * 创建错误日志
     */
    public static LogEvent createErrorLog(String serviceName, String familyId, String userId,
                                        String operationType, String errorCode, String errorMessage) {
        return LogEvent.builder()
            .logType(LogType.ERROR)
            .serviceName(serviceName)
            .familyId(familyId)
            .userId(userId)
            .operationType(operationType)
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .resultStatus(ResultStatus.FAILED)
            .timestamp(LocalDateTime.now())
            .traceId(com.haven.base.utils.TraceIdUtil.getCurrentOrGenerate())
            .build();
    }

    /**
     * 设置执行时间并返回自身 (链式调用)
     */
    public LogEvent withExecutionTime(long startTime) {
        this.executionTimeMs = System.currentTimeMillis() - startTime;
        return this;
    }

    /**
     * 设置操作结果并返回自身 (链式调用)
     */
    public LogEvent withResult(ResultStatus status, String message) {
        this.resultStatus = status;
        if (status == ResultStatus.FAILED && message != null) {
            this.errorMessage = message;
        }
        return this;
    }

    /**
     * 添加元数据并返回自身 (链式调用)
     */
    public LogEvent addMetadata(String key, Object value) {
        if (this.metadata == null) {
            this.metadata = new java.util.HashMap<>();
        }
        this.metadata.put(key, value);
        return this;
    }

    /**
     * 添加标签并返回自身 (链式调用)
     */
    public LogEvent addTag(String key, String value) {
        if (this.tags == null) {
            this.tags = new java.util.HashMap<>();
        }
        this.tags.put(key, value);
        return this;
    }

    /**
     * 判断是否为高风险日志
     */
    public boolean isHighRisk() {
        return riskLevel == RiskLevel.HIGH || riskLevel == RiskLevel.CRITICAL;
    }

    /**
     * 判断是否为失败操作
     */
    public boolean isFailedOperation() {
        return resultStatus == ResultStatus.FAILED ||
               resultStatus == ResultStatus.BLOCKED ||
               resultStatus == ResultStatus.TIMEOUT;
    }

    /**
     * 获取格式化的时间戳
     */
    public String getFormattedTimestamp() {
        return timestamp != null ? timestamp.toString() : LocalDateTime.now().toString();
    }
}