package com.haven.storage.strategy.storage.impl;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.strategy.storage.StorageNamingStrategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class S3StorageNamingStrategy implements StorageNamingStrategy {

    @Override
    public String buildFamilyBucketName(String familyId) {
        // MinIO桶名规则：小写，前缀"minio-"
        return "minio-family-" + familyId.toLowerCase();
    }

    @Override
    public String buildFilePath(String familyId, String fileType) {
        // MinIO路径格式：familyId/yyyy/MM/fileType/
        return familyId + "/" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + (fileType == null ? "default" : fileType) + "/";
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.CLOUD_S3;
    }
}
