package com.haven.storage.strategy.storage.impl;


import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.strategy.storage.StorageNamingStrategy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;


public class LocalStorageNamingStrategy implements StorageNamingStrategy {

    @Override
    public String buildFamilyBucketName(String familyId) {
        // 本地存储可能不需要桶名，返回空或统一前缀
        return "local-family-" + familyId;
    }

    @Override
    public String buildFilePath(String familyId, String fileType) {
        // 本地路径格式：/baseDir/familyId/fileType/yyyyMM/
        return familyId + "/" + (fileType == null ? "default" : fileType) + "/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM")) + "/";
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }
}
