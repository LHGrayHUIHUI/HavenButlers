package com.haven.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * 用户实体
 *
 * @author HavenButler
 */
@Entity
@Table(name = "admin_users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity {

    /**
     * 用户ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户名
     */
    @Column(name = "username", nullable = false, unique = true, length = 50)
    private String username;

    /**
     * 密码（加密后）
     */
    @Column(name = "password", nullable = false, length = 255)
    private String password;

    /**
     * 邮箱
     */
    @Column(name = "email", length = 100)
    private String email;

    /**
     * 显示名称
     */
    @Column(name = "display_name", length = 100)
    private String displayName;

    /**
     * 用户状态
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE, LOCKED

    /**
     * 用户角色
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<RoleEntity> roles;

    /**
     * 最后登录时间
     */
    @Column(name = "last_login_time")
    private Instant lastLoginTime;

    /**
     * 最后登录IP
     */
    @Column(name = "last_login_ip", length = 50)
    private String lastLoginIp;

    /**
     * 登录失败次数
     */
    @Column(name = "login_failed_count")
    private Integer loginFailedCount;

    /**
     * 账户锁定时间
     */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (status == null) {
            status = "ACTIVE";
        }
        if (loginFailedCount == null) {
            loginFailedCount = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}