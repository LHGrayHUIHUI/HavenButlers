package com.haven.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * 角色实体
 *
 * @author HavenButler
 */
@Entity
@Table(name = "admin_roles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleEntity {

    /**
     * 角色ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 角色名称
     */
    @Column(name = "role_name", nullable = false, unique = true, length = 50)
    private String roleName;

    /**
     * 角色显示名称
     */
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /**
     * 角色描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 角色状态
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE

    /**
     * 角色权限
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    private Set<PermissionEntity> permissions;

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
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}