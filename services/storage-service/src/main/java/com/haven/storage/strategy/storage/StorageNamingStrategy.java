package com.haven.storage.strategy.storage;


import com.haven.storage.domain.model.enums.StorageType;

/**
 * 存储命名策略接口：定义不同存储类型的命名规则
 */
public interface StorageNamingStrategy {

    /**
     * 构建家庭专用桶名
     */
    String buildFamilyBucketName(String familyId);

    /**
     * 构建文件存储路径（不含文件名）
     *
     * @param familyId 家庭ID
     * @param fileType 文件类型（如"image"、"video"，可选）
     * @return 路径（如"family/123/image/202405/"）
     */
    String buildFilePath(String familyId, String fileType);

    /**
     * 获取策略对应的存储类型
     */
    StorageType getStorageType();
}
