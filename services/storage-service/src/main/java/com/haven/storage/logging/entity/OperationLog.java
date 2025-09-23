package com.haven.storage.logging.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志实体类
 * 记录所有数据库操作和API调用的详细信息
 *
 * @author HavenButler
 */
@Data
@Entity
@Table(name = "operation_logs", indexes = {
    @Index(name = "idx_family_created", columnList = "family_id,created_at"),
    @Index(name = "idx_service_created", columnList = "service_type,created_at"),
    @Index(name = "idx_client_ip", columnList = "client_ip"),
    @Index(name = "idx_result_status", columnList = "result_status")
})
public class OperationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 家庭ID - 数据隔离的关键字段
     */
    @Column(name = "family_id", length = 64, nullable = false)
    private String familyId;

    /**
     * 服务类型：POSTGRESQL、MONGODB、REDIS、HTTP_API
     */
    @Column(name = "service_type", length = 20, nullable = false)
    private String serviceType;

    /**
     * 客户端IP地址
     */
    @Column(name = "client_ip", length = 45, nullable = false)
    private String clientIP;

    /**
     * 用户ID
     */
    @Column(name = "user_id", length = 64)
    private String userId;

    /**
     * 操作类型：SELECT、INSERT、UPDATE、DELETE、CREATE、DROP等
     */
    @Column(name = "operation_type", length = 50, nullable = false)
    private String operationType;

    /**
     * 数据库名称
     */
    @Column(name = "database_name", length = 100)
    private String databaseName;

    /**
     * 表名或集合名
     */
    @Column(name = "table_or_collection", length = 100)
    private String tableOrCollection;

    /**
     * 操作内容(脱敏后的SQL语句、MongoDB命令、Redis命令)
     */
    @Column(name = "operation_content", columnDefinition = "TEXT")
    private String operationContent;

    /**
     * 执行时间(毫秒)
     */
    @Column(name = "execution_time_ms")
    private Integer executionTimeMs;

    /**
     * 结果状态：SUCCESS、FAILED、BLOCKED、IN_PROGRESS
     */
    @Column(name = "result_status", length = 20, nullable = false)
    private String resultStatus;

    /**
     * 错误信息(如果操作失败)
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 请求追踪ID(用于链路追踪)
     */
    @Column(name = "trace_id", length = 64)
    private String traceId;

    /**
     * 会话ID(用于关联同一会话的多个操作)
     */
    @Column(name = "session_id", length = 64)
    private String sessionId;

    /**
     * 返回记录数(对于查询操作)
     */
    @Column(name = "affected_rows")
    private Integer affectedRows;

    /**
     * 额外的元数据信息(JSON格式)
     */
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}