package com.haven.storage.logging.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全日志实体类
 * 记录系统中的安全事件和异常行为
 *
 * @author HavenButler
 */
@Data
@Entity
@Table(name = "security_logs", indexes = {
    @Index(name = "idx_family_risk", columnList = "family_id,risk_level,created_at"),
    @Index(name = "idx_client_ip", columnList = "client_ip,created_at"),
    @Index(name = "idx_event_type", columnList = "event_type,created_at"),
    @Index(name = "idx_risk_level", columnList = "risk_level,created_at")
})
public class SecurityLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 家庭ID(可为空，某些系统级事件不关联特定家庭)
     */
    @Column(name = "family_id", length = 64)
    private String familyId;

    /**
     * 客户端IP地址
     */
    @Column(name = "client_ip", length = 45, nullable = false)
    private String clientIP;

    /**
     * 事件类型：
     * AUTH_FAILED - 认证失败
     * ACCESS_DENIED - 访问拒绝
     * DANGEROUS_OPERATION_BLOCKED - 危险操作被拦截
     * SUSPICIOUS_OPERATION - 可疑操作
     * CONNECTION_ERROR - 连接异常
     * RATE_LIMIT_EXCEEDED - 频率限制超出
     * UNAUTHORIZED_ACCESS - 未授权访问
     */
    @Column(name = "event_type", length = 50, nullable = false)
    private String eventType;

    /**
     * 服务类型：POSTGRESQL、MONGODB、REDIS、HTTP_API、SYSTEM
     */
    @Column(name = "service_type", length = 20)
    private String serviceType;

    /**
     * 事件详细信息(JSON格式或纯文本)
     */
    @Column(name = "event_details", columnDefinition = "TEXT")
    private String eventDetails;

    /**
     * 风险等级：LOW、MEDIUM、HIGH、CRITICAL
     */
    @Column(name = "risk_level", length = 20, nullable = false)
    private String riskLevel;

    /**
     * 用户ID(如果能确定)
     */
    @Column(name = "user_id", length = 64)
    private String userId;

    /**
     * 用户代理信息
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * 请求URL或操作标识
     */
    @Column(name = "request_path", length = 200)
    private String requestPath;

    /**
     * 响应状态码
     */
    @Column(name = "response_code")
    private Integer responseCode;

    /**
     * 事件发生的地理位置(基于IP解析)
     */
    @Column(name = "geo_location", length = 100)
    private String geoLocation;

    /**
     * 是否已处理
     */
    @Column(name = "is_handled", nullable = false)
    private Boolean isHandled = false;

    /**
     * 处理人员
     */
    @Column(name = "handled_by", length = 64)
    private String handledBy;

    /**
     * 处理时间
     */
    @Column(name = "handled_at")
    private LocalDateTime handledAt;

    /**
     * 处理备注
     */
    @Column(name = "handling_notes", columnDefinition = "TEXT")
    private String handlingNotes;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 请求追踪ID
     */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (isHandled == null) {
            isHandled = false;
        }
    }
}