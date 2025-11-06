package com.haven.storage.permission;

import com.haven.storage.model.enums.FileOperation;
import com.haven.storage.model.enums.FileVisibility;
import com.haven.storage.model.enums.UserRole;

import java.util.EnumSet;
import java.util.Set;

/**
 * 权限矩阵 - 定义用户角色、文件可见性和操作之间的权限关系
 */
public class PermissionMatrix {

    /**
     * 检查是否有权限执行操作
     */
    public static boolean hasPermission(UserRole role, FileVisibility visibility, FileOperation operation) {
        return switch (role) {
            case OWNER ->
                // 所有者拥有所有权限
                    true;
            case FAMILY_MEMBER -> {
                // 家庭成员的权限取决于文件可见性
                if (visibility == FileVisibility.PRIVATE) {
                    yield false;
                }
                // 家庭和公开文件：可以查看和下载，不能修改和删除
                yield operation == FileOperation.VIEW || operation == FileOperation.DOWNLOAD;
                // 家庭和公开文件：可以查看和下载，不能修改和删除
            }
            case PUBLIC_USER ->
                // 公共用户只能访问公开文件，且只能查看
                    visibility == FileVisibility.PUBLIC && operation == FileOperation.VIEW;
        };
    }

    /**
     * 获取用户在指定可见性级别下的所有可用操作
     */
    public static Set<FileOperation> getPermissions(UserRole role, FileVisibility visibility) {
        return switch (role) {
            case OWNER -> EnumSet.allOf(FileOperation.class);
            case FAMILY_MEMBER -> {
                if (visibility == FileVisibility.PRIVATE) {
                    yield EnumSet.noneOf(FileOperation.class);
                }
                yield EnumSet.of(FileOperation.VIEW, FileOperation.DOWNLOAD);
            }
            case PUBLIC_USER -> {
                if (visibility == FileVisibility.PUBLIC) {
                    yield EnumSet.of(FileOperation.VIEW);
                }
                yield EnumSet.noneOf(FileOperation.class);
            }
        };
    }
}
