package com.haven.storage.domain.model.enums;

import lombok.Getter;

/**
 * 用户角色枚举
 */
@Getter
public enum UserRole {
    OWNER("所有者"),
    FAMILY_MEMBER("家庭成员"),
    PUBLIC_USER("公共用户");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }

    /**
     * 检查该角色是否可以访问指定可见性级别的文件
     */
    public boolean canAccess(FileVisibility visibility) {
        return switch (this) {
            case OWNER -> true;
            case FAMILY_MEMBER -> visibility.isHigherThan(FileVisibility.PRIVATE);
            case PUBLIC_USER -> visibility == FileVisibility.PUBLIC;
        };
    }
}
