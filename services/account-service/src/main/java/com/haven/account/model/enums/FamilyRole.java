package com.haven.account.model.enums;

import lombok.Getter;

/**
 * 家庭角色枚举
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Getter
public enum FamilyRole {
    OWNER("family_owner", "家庭所有者", 150),
    ADMIN("family_admin", "家庭管理员", 100),
    MEMBER("family_member", "家庭成员", 50),
    GUEST("family_guest", "访客", 10);

    private final String code;
    private final String description;
    private final int level;

    FamilyRole(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    /**
     * 根据代码获取枚举
     */
    public static FamilyRole fromCode(String code) {
        for (FamilyRole role : values()) {
            if (role.getCode().equals(code)) {
                return role;
            }
        }
        return GUEST; // 默认访客
    }

    /**
     * 检查是否有足够的权限级别
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return this.level >= requiredLevel;
    }
}