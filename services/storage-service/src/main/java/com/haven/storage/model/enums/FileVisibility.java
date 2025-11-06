package com.haven.storage.model.enums;

import lombok.Getter;

/**
 * 文件可见性级别 (File Visibility Level)
 * <p>
 * 定义文件的访问可见性控制级别。
 * 每个级别关联一个明确的等级值 (levelValue)，
 * 用于判断权限高低，确保未来级别扩展的健壮性。
 * 支持从私有 (PRIVATE) 到完全公开 (PUBLIC) 的多级管理。
 *
 * @author HavenButler
 * @version 1.1 (优化：引入 levelValue 进行级别比较，解耦声明顺序)
 */
@Getter
public enum FileVisibility {
    /**
     * 私有文件 - 等级: 10
     * 仅文件所有者可以访问。
     */
    PRIVATE("私有", 10),

    /**
     * 家庭文件 - 等级: 20
     * 家庭成员可以访问。
     */
    FAMILY("家庭", 20),

    /**
     * 公开文件 - 等级: 100
     * 所有人都可以访问。
     */
    PUBLIC("公开", 100);

    private final String description;
    private final int levelValue; // 新增：用于比较的级别值

    FileVisibility(String description, int levelValue) {
        this.description = description;
        this.levelValue = levelValue;
    }

    /**
     * 检查当前可见性级别是否高于指定级别
     *
     * @param other 另一个可见性级别
     * @return 是否更高 (通过 levelValue 比较)
     */
    public boolean isHigherThan(FileVisibility other) {
        // 使用 levelValue 进行比较，避免依赖枚举声明顺序
        return this.levelValue > other.levelValue;
    }

    /**
     * 检查当前可见性级别是否低于指定级别
     *
     * @param other 另一个可见性级别
     * @return 是否更低 (通过 levelValue 比较)
     */
    public boolean isLowerThan(FileVisibility other) {
        // 使用 levelValue 进行比较，避免依赖枚举声明顺序
        return this.levelValue < other.levelValue;
    }

    // 也可以添加 isAtLeast(FileVisibility other) 或 isAtMost(FileVisibility other)
}