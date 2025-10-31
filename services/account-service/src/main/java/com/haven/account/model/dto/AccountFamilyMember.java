package com.haven.account.model.dto;

import com.haven.base.model.dto.BaseFamilyMember;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 家庭成员数据传输对象
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AccountFamilyMember extends BaseFamilyMember  {

    private Long id;

    private Long familyId;

    private String username;

    private String email;

    private String avatarUrl;

    /**
     * 获取加入时间（兼容性方法）
     */
    public String getJoinedAt() {
        return getJoinTime() != null ? getJoinTime().toString() : null;
    }

    /**
     * 获取用户ID（兼容性方法）
     */
    public String getUserId() {
        return super.getUserId();
    }

    /**
     * 获取角色（兼容性方法）
     */
    public String getRole() {
        return super.getRole();
    }

}