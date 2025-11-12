package com.haven.storage.operation.storage.adapter;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.processor.context.FileProcessContext;
import lombok.RequiredArgsConstructor;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Component
public class LocalStorageAdapter implements StorageAdapter {
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
        return StorageType.LOCAL;
    }

    @Override
    public URI getFileAccessUrl(FileStorageInfo fileStorageInfo) {
        return null;
    }
}
