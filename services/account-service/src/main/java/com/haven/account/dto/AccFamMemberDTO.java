package com.haven.account.dto;

import com.haven.base.model.dto.FamilyDTO.FamilyMemberDTO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 账户服务家庭成员数据传输对象
 * 继承base模块的FamilyMemberDTO功能
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class AccFamMemberDTO extends FamilyMemberDTO {

    /**
     * 本地数据库主键ID
     */
    private Long id;

    /**
     * 成员UUID
     */
    private UUID uuid;

    /**
     * 家庭ID（本地数据库）
     */
    private Long familyId;

    /**
     * 用户名（account特有）
     */
    private String username;

    /**
     * 邮箱（account特有）
     */
    private String email;

    /**
     * 加入时间
     */
    private LocalDateTime joinedAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 构造函数
     */
    public AccFamMemberDTO(Long familyId, String userId, String role) {
        this.familyId = familyId;
        this.setUserId(userId);
        this.setRole(role);
        // 设置base模块的joinTime
        this.setJoinTime(LocalDateTime.now());
    }

    /**
     * 从base FamilyMemberDTO转换
     */
    public static AccFamMemberDTO fromBase(FamilyMemberDTO base) {
        AccFamMemberDTO dto = new AccFamMemberDTO();
        dto.setUserId(base.getUserId());
        dto.setNickname(base.getNickname());
        dto.setAvatarUrl(base.getAvatarUrl());
        dto.setRole(base.getRole());
        dto.setJoinTime(base.getJoinTime());
        dto.setPermissions(base.getPermissions());
        return dto;
    }

    /**
     * 转换为base FamilyMemberDTO
     */
    public FamilyMemberDTO toBase() {
        FamilyMemberDTO base = new FamilyMemberDTO();
        base.setUserId(this.getUserId());
        base.setNickname(this.getNickname());
        base.setAvatarUrl(this.getAvatarUrl());
        base.setRole(this.getRole());
        base.setJoinTime(this.getJoinTime());
        base.setPermissions(this.getPermissions());
        return base;
    }

    /**
     * 重写isAdmin方法，支持account-service的角色映射
     */
    @Override
    public boolean isAdmin() {
        String currentRole = getRole();
        return "family_admin".equals(currentRole) || "ADMIN".equals(currentRole) || "OWNER".equals(currentRole);
    }

    /**
     * 检查是否为拥有者
     */
    @Override
    public boolean isOwner() {
        String currentRole = getRole();
        return "OWNER".equals(currentRole);
    }

    /**
     * 映射方法：设置base模块的joinTime
     */
    public void setJoinTimeFromJoinedAt() {
        this.setJoinTime(this.joinedAt);
    }

    /**
     * 映射方法：获取base模块的joinTime
     */
    public void setJoinedAtFromJoinTime() {
        this.joinedAt = this.getJoinTime();
    }
}