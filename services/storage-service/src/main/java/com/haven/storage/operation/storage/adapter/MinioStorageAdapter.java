package com.haven.storage.operation.storage.adapter;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.operation.storage.StorageAdapter;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import okio.BufferedSource;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@RequiredArgsConstructor
@Component
public class MinioStorageAdapter implements StorageAdapter {
    private final MinioClient minioClient;

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
        return StorageType.MINIO;
    }

    @Override
    public URI getFileAccessUrl(FileStorageInfo fileStorageInfo) {
        return null;
    }
}
