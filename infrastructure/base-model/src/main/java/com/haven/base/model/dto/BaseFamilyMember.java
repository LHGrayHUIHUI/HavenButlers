package com.haven.base.model.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.haven.base.model.entity.BaseEntity;
import jakarta.persistence.MappedSuperclass;
import lombok.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 家庭成员DTO
 */
@EqualsAndHashCode(callSuper = false)
@Data
@MappedSuperclass
public abstract class BaseFamilyMember extends BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 成员用户ID
     */
    private String userId;

    /**
     * 成员昵称
     */
    private String nickname;

    /**
     * 成员头像
     */
    private String avatarUrl;

    /**
     * 成员角色（OWNER/ADMIN/MEMBER/GUEST）
     */
    private String role;

    /**
     * 加入时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime joinTime;

    /**
     * 权限列表
     */
    private List<String> permissions;

    /**
     * 是否为管理员
     */
    public boolean isAdmin() {
        return "OWNER".equals(role) || "ADMIN".equals(role);
    }

    /**
     * 是否为拥有者
     */
    public boolean isOwner() {
        return "OWNER".equals(role);
    }
}
