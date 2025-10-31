package com.haven.account.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 用户角色枚举
 * 定义用户系统级别的角色权限
 *
 * @author HavenButler
 * @since 2025-01-16
 */
public enum UserRole {

    /**
     * 普通用户
     */
    USER("USER", "普通用户", 1),

    /**
     * 高级用户
     */
    POWER_USER("POWER_USER", "高级用户", 5),

    /**
     * 系统管理员
     */
    ADMIN("ADMIN", "系统管理员", 9),

    /**
     * 超级管理员
     */
    SUPER_ADMIN("SUPER_ADMIN", "超级管理员", 10);

    private final String code;

    @Getter
    private final String description;

    @Getter
    private final int level;

    UserRole(String code, String description, int level) {
        this.code = code;
        this.description = description;
        this.level = level;
    }

    /**
     * 获取角色代码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 根据代码获取枚举值
     */
    @JsonCreator
    public static UserRole fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (UserRole role : UserRole.values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }

        throw new IllegalArgumentException("未知的用户角色代码: " + code);
    }

    /**
     * 检查是否为管理员角色
     */
    public boolean isAdmin() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    /**
     * 检查是否为超级管理员
     */
    public boolean isSuperAdmin() {
        return this == SUPER_ADMIN;
    }

    /**
     * 检查是否为普通用户
     */
    public boolean isUser() {
        return this == USER;
    }

    /**
     * 检查是否有足够的权限级别
     */
    public boolean hasPermissionLevel(int requiredLevel) {
        return this.level >= requiredLevel;
    }

    @Override
    public String toString() {
        return code;
    }
}