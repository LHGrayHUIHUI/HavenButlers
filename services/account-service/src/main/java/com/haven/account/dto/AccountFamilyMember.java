package com.haven.account.dto;

import com.haven.base.model.dto.BaseFamilyMember;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 家庭成员数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AccountFamilyMember extends BaseFamilyMember implements Serializable {

    private Long id;
    private UUID uuid;

    private Long familyId;

    private Long userId;

    private String username;

    private String email;

    private String avatarUrl;

    private String role;

    private String status;

    private LocalDateTime joinedAt;

    private LocalDateTime updatedAt;

    /**
     * 默认构造函数
     */
    public AccountFamilyMember() {
    }

    /**
     * 构造函数
     */
    public AccountFamilyMember(Long familyId, Long userId, String role) {
        this.familyId = familyId;
        this.userId = userId;
        this.role = role;
        this.status = "ACTIVE";
    }

    /**
     * 检查是否为管理员
     */
    public boolean isAdmin() {
        return "family_admin".equals(role);
    }
}