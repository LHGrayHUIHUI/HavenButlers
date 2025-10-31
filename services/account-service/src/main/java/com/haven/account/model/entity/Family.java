package com.haven.account.model.entity;

import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 家庭实体类
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Entity
@Table(name = "families")
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class Family extends BaseEntity {

  
    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;

    /**
     * 家庭标识符
     * 业务上使用的家庭ID，可能与数据库主键不同
     * 用于对外API和业务逻辑中的家庭标识
     */
    @Column(name = "family_id", unique = true, nullable = false, length = 50)
    private String familyId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "owner_id", nullable = false)
    private Long ownerId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @Column(name = "background_image_url", length = 500)
    private String backgroundImageUrl;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 家庭状态枚举
     */
    public enum Status {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        DELETED("DELETED");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 获取家庭状态
     */
    public Status getFamilyStatus() {
        return Status.valueOf(status);
    }

    /**
     * 设置家庭状态
     */
    public void setFamilyStatus(Status status) {
        this.status = status.getValue();
    }
}