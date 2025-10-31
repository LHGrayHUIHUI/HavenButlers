package com.haven.base.common.model;

import lombok.Data;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 基础实体类
 * 提供通用的审计字段和软删除支持
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Data
@MappedSuperclass
public abstract class BaseEntity {

    /**
     * 主键ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 创建时间
     */
    @Column(name = "created_time", nullable = false, updatable = false)
    private LocalDateTime createdTime;

    /**
     * 更新时间
     */
    @Column(name = "updated_time")
    private LocalDateTime updatedTime;

    /**
     * 创建者ID
     */
    @Column(name = "created_by", length = 64, updatable = false)
    private String createdBy;

    /**
     * 更新者ID
     */
    @Column(name = "updated_by", length = 64)
    private String updatedBy;

    /**
     * 版本号（乐观锁）
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * 是否已删除（软删除）
     */
    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    /**
     * 删除时间
     */
    @Column(name = "deleted_time")
    private LocalDateTime deletedTime;

    /**
     * 删除者ID
     */
    @Column(name = "deleted_by", length = 64)
    private String deletedBy;

    /**
     * 备注
     */
    @Column(name = "remark", length = 500)
    private String remark;

    /**
     * 状态（通用状态字段）
     */
    @Column(name = "status", length = 20)
    private String status;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdTime == null) {
            createdTime = now;
        }
        updatedTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedTime = LocalDateTime.now();
    }

    /**
     * 软删除
     */
    public void softDelete(String deletedBy) {
        this.deleted = true;
        this.deletedTime = LocalDateTime.now();
        this.deletedBy = deletedBy;
    }

    /**
     * 恢复删除
     */
    public void restore() {
        this.deleted = false;
        this.deletedTime = null;
        this.deletedBy = null;
    }

    /**
     * 检查是否已删除
     */
    public boolean isDeleted() {
        return Boolean.TRUE.equals(deleted);
    }
}