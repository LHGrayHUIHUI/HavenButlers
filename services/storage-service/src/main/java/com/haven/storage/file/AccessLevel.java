package com.haven.storage.file;

/**
 * 文件访问权限级别
 *
 * 定义文件的访问权限控制级别
 * 支持从私有到公开的多级权限管理
 *
 * @author HavenButler
 */
public enum AccessLevel {
    /**
     * 私有文件
     * 仅文件所有者可以访问
     */
    PRIVATE("私有"),

    /**
     * 家庭文件
     * 家庭成员可以访问
     */
    FAMILY("家庭"),

    /**
     * 公开文件
     * 所有人都可以访问
     */
    PUBLIC("公开");

    private final String description;

    AccessLevel(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 检查权限级别是否高于指定级别
     *
     * @param other 另一个权限级别
     * @return 是否更高
     */
    public boolean isHigherThan(AccessLevel other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * 检查权限级别是否低于指定级别
     *
     * @param other 另一个权限级别
     * @return 是否更低
     */
    public boolean isLowerThan(AccessLevel other) {
        return this.ordinal() < other.ordinal();
    }
}