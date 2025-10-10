package com.haven.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 权限实体
 *
 * @author HavenButler
 */
@Entity
@Table(name = "admin_permissions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionEntity {

    /**
     * 权限ID
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 权限代码
     */
    @Column(name = "permission_code", nullable = false, unique = true, length = 100)
    private String permissionCode;

    /**
     * 权限名称
     */
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;

    /**
     * 权限描述
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * 资源类型
     */
    @Column(name = "resource_type", length = 50)
    private String resourceType; // MENU, BUTTON, API, PAGE

    /**
     * 资源路径
     */
    @Column(name = "resource_path", length = 200)
    private String resourcePath;

    /**
     * 操作类型
     */
    @Column(name = "action_type", length = 50)
    private String actionType; // READ, WRITE, DELETE, EXECUTE

    /**
     * 父权限ID
     */
    @Column(name = "parent_id")
    private Long parentId;

    /**
     * 排序号
     */
    @Column(name = "sort_order")
    private Integer sortOrder;

    /**
     * 权限状态
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status; // ACTIVE, INACTIVE

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
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}