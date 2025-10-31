package com.haven.account.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

/**
 * 家庭成员状态枚举
 * 定义家庭成员在家庭中的状态
 *
 * @author HavenButler
 * @since 2025-01-16
 */
public enum MemberStatus {

    /**
     * 活跃状态 - 正常参与家庭活动
     */
    ACTIVE("ACTIVE", "活跃"),

    /**
     * 暂离状态 - 暂时离开家庭，但保留成员身份
     */
    AWAY("AWAY", "暂离"),

    /**
     * 禁用状态 - 被管理员禁用，无法访问家庭数据
     */
    DISABLED("DISABLED", "禁用"),

    /**
     * 离开状态 - 已主动离开家庭
     */
    LEFT("LEFT", "已离开");

    private final String code;

    @Getter
    private final String description;

    MemberStatus(String code, String description) {
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
     * 根据代码获取枚举值
     */
    @JsonCreator
    public static MemberStatus fromCode(String code) {
        if (code == null) {
            return null;
        }

        for (MemberStatus status : MemberStatus.values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }

        throw new IllegalArgumentException("未知的家庭成员状态代码: " + code);
    }

    /**
     * 检查是否为活跃状态
     */
    public boolean isActive() {
        return this == ACTIVE;
    }

    /**
     * 检查是否可以访问家庭数据
     */
    public boolean canAccessFamilyData() {
        return this == ACTIVE || this == AWAY;
    }

    /**
     * 检查是否被禁用
     */
    public boolean isDisabled() {
        return this == DISABLED;
    }

    /**
     * 检查是否已离开
     */
    public boolean hasLeft() {
        return this == LEFT;
    }

    @Override
    public String toString() {
        return code;
    }
}