package com.haven.storage.domain.model.enums;

import lombok.Getter;

/**
 * 文件操作类型枚举
 */
@Getter
public enum FileOperation {

    UPLOAD("上传", 0),  // 新增上传操作
    VIEW("查看", 10),
    DOWNLOAD("下载", 20),
    MODIFY("修改", 30),
    DELETE("删除", 40),
    SHARE("分享文件", 50),
    MODIFY_PERMISSIONS("修改权限", 60);
    private final String description;
    private final int permissionLevel; // 权限等级，数值越大需要的权限越高

    FileOperation(String description, int permissionLevel) {
        this.description = description;
        this.permissionLevel = permissionLevel;
    }

    /**
     * 检查当前操作是否比另一个操作需要更高权限
     */
    public boolean requiresHigherPermissionThan(FileOperation other) {
        return this.permissionLevel > other.getPermissionLevel();
    }
}
