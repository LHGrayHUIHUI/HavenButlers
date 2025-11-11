package com.haven.storage.utils;

/**
 * 根据类型的名称创建桶等链接的名称和地址的
 */
public class StorageInfo {
    /**
     * 构建家庭专用桶名
     *
     */
    private String buildBucketName(String familyId) {
        return ("family" + "-" + familyId).toLowerCase();
    }
}
