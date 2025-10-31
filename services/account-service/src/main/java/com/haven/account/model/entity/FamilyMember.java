package com.haven.account.model.entity;

import com.haven.account.model.enums.FamilyRole;
import com.haven.account.model.enums.MemberStatus;
import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 家庭成员实体类
 *
 * 此实体用于管理用户与家庭之间的成员关系，包含用户在特定家庭中的角色、状态和权限信息。
 * 每个用户可以在多个家庭中拥有不同的角色，实现了多家庭权限隔离。
 *
 * <p>核心特性：</p>
 * <ul>
 *   <li>多家庭支持：一个用户可以加入多个家庭，每个家庭有独立的角色</li>
 *   <li>权限层级：支持访客、成员、管理员、所有者四个权限层级</li>
 *   <li>状态管理：支持活跃、暂离、禁用、离开四种成员状态</li>
 *   <li>审计追踪：记录加入时间、更新时间等审计信息</li>
 * </ul>
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
public class FamilyMember extends BaseEntity {

    
    /**
     * 家庭ID
     * 关联到家庭表的主键，表示成员所属的家庭
     * 与userId组合保证用户在家庭中的唯一性
     */
    @Column(name = "family_id", nullable = false)
    private Long familyId;

    /**
     * 用户ID
     * 关联到用户表的主键，表示家庭成员对应的用户账户
     * 与familyId组合保证用户家庭关系的唯一性
     */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /**
     * 家庭角色代码
     * 存储家庭成员在家庭中的权限角色，使用枚举代码字符串存储
     * 可选值：family_owner(所有者)、family_admin(管理员)、family_member(成员)、family_guest(访客)
     * 权限级别：所有者(150) > 管理员(100) > 成员(50) > 访客(10)
     */
    @Column(name = "family_role", nullable = false, length = 20)
    private String familyRole;

    /**
     * 成员状态
     * 表示用户在家庭中的参与状态，影响数据访问权限
     * 默认值：ACTIVE（活跃状态）
     * 可选值：ACTIVE(活跃)、AWAY(暂离)、DISABLED(禁用)、LEFT(已离开)
     * 只有ACTIVE和AWAY状态可以访问家庭数据
     */
    @Column(name = "status", length = 20)
    private String status = "ACTIVE";

    /**
     * 加入家庭时间
     * 记录用户首次加入家庭的时间戳，创建后不可修改
     * 用于成员统计、活跃度分析和权限历史追踪
     */
    @CreatedDate
    @Column(name = "joined_at", nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    /**
     * 最后更新时间
     * 记录成员信息最后一次修改的时间，包括角色变更、状态更新等
     * 由JPA审计功能自动维护，用于数据变更追踪
     */
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        // BaseEntity会自动处理创建时间和更新时间
        if (joinedAt == null) {
            joinedAt = LocalDateTime.now();
        }
        // updatedAt会由@LastModifiedDate自动处理
    }

      // 注：更新时间由BaseEntity自动处理，无需重复定义


    /**
     * 获取家庭角色
     */
    public FamilyRole getFamilyRole() {
        return FamilyRole.fromCode(familyRole);
    }

    /**
     * 设置家庭角色
     */
    public void setFamilyRole(FamilyRole role) {
        this.familyRole = role.getCode();
    }

    /**
     * 获取家庭成员状态
     */
    public MemberStatus getMemberStatus() {
        return MemberStatus.fromCode(status);
    }

    /**
     * 设置家庭成员状态
     */
    public void setMemberStatus(MemberStatus status) {
        this.status = status.getCode();
    }

    /**
     * 检查是否为家庭管理员
     */
    public boolean isFamilyAdmin() {
        return FamilyRole.ADMIN.equals(getFamilyRole()) || FamilyRole.OWNER.equals(getFamilyRole());
    }

    /**
     * 检查是否为家庭所有者
     */
    public boolean isFamilyOwner() {
        return FamilyRole.OWNER.equals(getFamilyRole());
    }

    /**
     * 检查权限级别是否足够
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return getFamilyRole().getLevel() >= requiredLevel;
    }
}