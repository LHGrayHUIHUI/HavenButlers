package com.haven.storage.domain.model.file;

import lombok.Getter;

/**
 * 文件可见性级别
 *
 * 定义文件的可见性控制级别
 * 支持从私有到公开的多级可见性管理
 *
 * @author HavenButler
 */
@Getter
public enum FileVisibility {
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

    FileVisibility(String description) {
        this.description = description;
    }

    /**
     * 检查可见性级别是否高于指定级别
     *
     * @param other 另一个可见性级别
     * @return 是否更高
     */
    public boolean isHigherThan(FileVisibility other) {
        return this.ordinal() > other.ordinal();
    }

    /**
     * 检查可见性级别是否低于指定级别
     *
     * @param other 另一个可见性级别
     * @return 是否更低
     */
    public boolean isLowerThan(FileVisibility other) {
        return this.ordinal() < other.ordinal();
    }
}