package com.haven.storage.operation.storage.adapter;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.operation.storage.StorageAdapter;
import lombok.RequiredArgsConstructor;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.net.URI;

@RequiredArgsConstructor
@Component
public class CosStorageAdapter implements StorageAdapter {

    @Override
    public String buildFamilyBucketName(String familyId) {
        return "";
    }

    @Override
    public String buildFilePath(String familyId, String fileType) {
        return "";
    }

    @Override
    public boolean uploadFile(FileStorageInfo fileStorageInfo, BufferedSource bufferedSource) {
        return false;
    }

    @Override
    public BufferedSource downloadFile(String fileId, String familyId) {
        return null;
    }

    @Override
    public boolean deleteFile(String fileId, String familyId) {
        return false;
    }

    @Override
    public boolean isHealthy() {
        return false;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.CLOUD_COS;
    }

    @Override
    public URI getFileAccessUrl(FileStorageInfo fileStorageInfo) {
        return URI.create(fileStorageInfo.getFileId());
    }
}
