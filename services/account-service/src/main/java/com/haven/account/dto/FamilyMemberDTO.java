package com.haven.account.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 家庭成员数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
public class FamilyMemberDTO {

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
    public FamilyMemberDTO() {
    }

    /**
     * 构造函数
     */
    public FamilyMemberDTO(Long familyId, Long userId, String role) {
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