package com.haven.account.entity;

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
 * 家庭成员实体类
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Entity
@Table(name = "family_members",
       uniqueConstraints = @UniqueConstraint(columnNames = {"family_id", "user_id"}))
@Data
@EqualsAndHashCode(callSuper = false)
@EntityListeners(AuditingEntityListener.class)
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "uuid", unique = true, nullable = false, updatable = false)
    private UUID uuid;

    @Column(name = "family_id", nullable = false)
    private Long familyId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (uuid == null) {
            uuid = UUID.randomUUID();
        }
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 成员状态枚举
     */
    public enum Status {
        ACTIVE("ACTIVE"),
        INACTIVE("INACTIVE"),
        REMOVED("REMOVED");

        private final String value;

        Status(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    /**
     * 家庭角色枚举
     */
    public enum Role {
        ADMIN("ADMIN", "家庭管理员", 100),
        MEMBER("MEMBER", "家庭成员", 50),
        GUEST("GUEST", "访客", 10);

        private final String value;
        private final String description;
        private final int level;

        Role(String value, String description, int level) {
            this.value = value;
            this.description = description;
            this.level = level;
        }

        public String getValue() {
            return value;
        }

        public String getDescription() {
            return description;
        }

        public int getLevel() {
            return level;
        }

        /**
         * 根据值获取角色
         */
        public static Role fromValue(String value) {
            for (Role role : Role.values()) {
                if (role.getValue().equals(value)) {
                    return role;
                }
            }
            throw new IllegalArgumentException("Unknown role: " + value);
        }
    }

    /**
     * 获取用户角色
     */
    public Role getUserRole() {
        return Role.fromValue(role);
    }

    /**
     * 设置用户角色
     */
    public void setUserRole(Role role) {
        this.role = role.getValue();
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(getUserRole());
    }

    /**
     * 检查权限级别是否足够
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return getUserRole().getLevel() >= requiredLevel;
    }
}