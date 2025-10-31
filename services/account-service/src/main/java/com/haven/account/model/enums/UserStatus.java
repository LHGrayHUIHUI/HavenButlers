package com.haven.account.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 用户状态枚举
 * 定义用户账户的各种状态
 *
 * @author HavenButler
 */
public enum UserStatus {

    /**
     * 激活状态
     */
    ACTIVE("ACTIVE", "激活"),

    /**
     * 未激活状态
     */
    INACTIVE("INACTIVE", "未激活"),

    /**
     * 锁定状态
     */
    LOCKED("LOCKED", "锁定");

    private final String code;
    private final String description;

    UserStatus(String code, String description) {
        this.code = code;
        this.description = description;
    }

    /**
     * 获取状态代码
     */
    @JsonValue
    public String getCode() {
        return code;
    }

    /**
     * 获取状态描述
     */
    public String getDescription() {
        return description;
    }

    /**
     * 根据代码获取枚举值
     */
    @JsonCreator
    public static UserStatus fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (UserStatus status : UserStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }

        throw new IllegalArgumentException("未知的用户状态代码: " + code);
    }

    /**
     * 检查是否为激活状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否为锁定状态
     */
    public boolean isLocked() {
        return this == LOCKED;
    }

    /**
     * 检查是否为未激活状态
     */
    public boolean isInactive() {
        return this == INACTIVE;
    }

    @Override
    public String toString() {
        return code;
    }
}