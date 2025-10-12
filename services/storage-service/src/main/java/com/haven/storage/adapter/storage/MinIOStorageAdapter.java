package com.haven.storage.file.adapter;

import com.haven.storage.file.FileUploadResult;
import com.haven.storage.file.FileDownloadResult;
import com.haven.storage.file.FileMetadata;
import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.Item;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * MinIO对象存储适配器
 *
 * 功能特性：
 * - 基于桶的家庭数据隔离
 * - 自动创建存储桶
 * - 支持预签名URL访问
 * - 支持对象标签管理
 * - 分布式存储支持
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "storage.file.storage-type", havingValue = "minio")
public class MinIOStorageAdapter implements StorageAdapter {

    private final MinioClient minioClient;

    @Value("${storage.file.minio.bucket-prefix:family}")
    private String bucketPrefix;

    @Value("${storage.file.minio.auto-create-bucket:true}")
    private boolean autoCreateBucket;

    @Value("${storage.file.local.max-file-size:104857600}") // 100MB
    private long maxFileSize;

    @Value("${storage.file.local.allowed-extensions:pdf,doc,docx,txt,jpg,jpeg,png,gif,mp4,avi,mp3,wav,zip,rar}")
    private String allowedExtensions;

    private static final String STORAGE_TYPE = "minio";

    @Override
    public FileUploadResult uploadFile(String familyId, String folderPath,
                                     MultipartFile file, String uploaderUserId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(familyId) || file == null || file.isEmpty()) {
                return FileUploadResult.failure("参数错误：familyId和file不能为空");
            }

            // 文件大小验证
            if (file.getSize() > maxFileSize) {
                return FileUploadResult.failure("文件大小超过限制：" + (maxFileSize / 1024 / 1024) + "MB");
            }

            // 文件类型验证
            String fileName = file.getOriginalFilename();
            if (!isAllowedFileType(fileName)) {
                return FileUploadResult.failure("不支持的文件类型：" + getFileExtension(fileName));
            }

            // 构建桶名和对象名
            String bucketName = buildBucketName(familyId);
            String fileId = UUID.randomUUID().toString();
            String objectName = buildObjectName(folderPath, fileId, fileName);

            // 确保桶存在
            ensureBucketExists(bucketName);

            // 上传文件到MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // 设置对象标签（用于管理和统计）
            minioClient.setObjectTags(
                    SetObjectTagsArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .tags(buildObjectTags(familyId, uploaderUserId))
                            .build()
            );

            // 创建文件元数据
            FileMetadata metadata = new FileMetadata();
            metadata.setFileId(fileId);
            metadata.setOriginalName(fileName);
            metadata.setFileSize(file.getSize());
            metadata.setContentType(file.getContentType());
            metadata.setFamilyId(familyId);
            metadata.setFolderPath(folderPath);
            metadata.setStoragePath(bucketName + "/" + objectName);
            metadata.setStorageType(STORAGE_TYPE);
            metadata.setUploaderUserId(uploaderUserId);
            metadata.setUploadTime(LocalDateTime.now());

            log.info("MinIO文件上传成功：familyId={}, fileId={}, bucket={}, object={}, size={}",
                    familyId, fileId, bucketName, objectName, file.getSize());

            return FileUploadResult.success(metadata, "tr-" + System.currentTimeMillis());

        } catch (Exception e) {
            log.error("MinIO文件上传失败：familyId={}, fileName={}, error={}",
                    familyId, file.getOriginalFilename(), e.getMessage());
            return FileUploadResult.failure("文件上传失败：" + e.getMessage());
        }
    }

    @Override
    public FileDownloadResult downloadFile(String fileId, String familyId) {
        try {
            // 参数验证
            if (!StringUtils.hasText(fileId) || !StringUtils.hasText(familyId)) {
                return FileDownloadResult.failure("参数错误：fileId和familyId不能为空");
            }

            // 构建桶名
            String bucketName = buildBucketName(familyId);

            // 查找对象
            String objectName = findObjectByFileId(bucketName, fileId);
            if (objectName == null) {
                return FileDownloadResult.failure("文件不存在或无权限访问");
            }

            // 下载文件内容
            GetObjectResponse response = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            byte[] fileContent = response.readAllBytes();
            String fileName = extractFileNameFromObject(objectName);
            String contentType = getContentType(fileName);

            log.info("MinIO文件下载成功：familyId={}, fileId={}, size={}",
                    familyId, fileId, fileContent.length);

            // 创建临时的FileMetadata用于返回
            FileMetadata tempMetadata = new FileMetadata();
            tempMetadata.setOriginalName(fileName);
            tempMetadata.setContentType(contentType);

            return FileDownloadResult.success(fileContent, tempMetadata, "tr-" + System.currentTimeMillis());

        } catch (Exception e) {
            log.error("MinIO文件下载失败：familyId={}, fileId={}, error={}",
                    familyId, fileId, e.getMessage());
            return FileDownloadResult.failure("文件下载失败：" + e.getMessage());
        }
    }

    @Override
    public boolean deleteFile(String fileId, String familyId) {
        try {
            // 构建桶名
            String bucketName = buildBucketName(familyId);

            // 查找对象
            String objectName = findObjectByFileId(bucketName, fileId);
            if (objectName == null) {
                log.warn("删除MinIO文件失败：文件不存在，familyId={}, fileId={}", familyId, fileId);
                return false;
            }

            // 删除对象
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );

            log.info("MinIO文件删除成功：familyId={}, fileId={}", familyId, fileId);
            return true;

        } catch (Exception e) {
            log.error("MinIO文件删除失败：familyId={}, fileId={}, error={}",
                    familyId, fileId, e.getMessage());
            return false;
        }
    }

    @Override
    public List<String> listFiles(String familyId, String folderPath) {
        try {
            String bucketName = buildBucketName(familyId);
            String prefix = buildObjectPrefix(folderPath);

            // 检查桶是否存在
            if (!bucketExists(bucketName)) {
                return new ArrayList<>();
            }

            // 列出对象
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .prefix(prefix)
                            .recursive(false)
                            .build()
            );

            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get().objectName();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(objectName -> objectName != null)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.error("MinIO获取文件列表失败：familyId={}, folderPath={}, error={}",
                    familyId, folderPath, e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public boolean isHealthy() {
        try {
            // 尝试列出桶来测试连接
            minioClient.listBuckets();
            return true;
        } catch (Exception e) {
            log.error("MinIO健康检查失败：{}", e.getMessage());
            return false;
        }
    }

    @Override
    public String getStorageType() {
        return STORAGE_TYPE;
    }

    @Override
    public String getFileAccessUrl(String fileId, String familyId, int expireMinutes) {
        try {
            String bucketName = buildBucketName(familyId);
            String objectName = findObjectByFileId(bucketName, fileId);

            if (objectName == null) {
                return null;
            }

            // 生成预签名URL
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(objectName)
                            .expiry(expireMinutes, TimeUnit.MINUTES)
                            .build()
            );

        } catch (Exception e) {
            log.error("生成MinIO访问URL失败：familyId={}, fileId={}, error={}",
                    familyId, fileId, e.getMessage());
            return null;
        }
    }

    /**
     * 构建家庭专用桶名
     */
    private String buildBucketName(String familyId) {
        return (bucketPrefix + "-" + familyId).toLowerCase();
    }

    /**
     * 构建对象名称
     */
    private String buildObjectName(String folderPath, String fileId, String fileName) {
        String prefix = buildObjectPrefix(folderPath);
        String extension = getFileExtension(fileName);
        return prefix + fileId + "." + extension;
    }

    /**
     * 构建对象前缀
     */
    private String buildObjectPrefix(String folderPath) {
        if (!StringUtils.hasText(folderPath) || "/".equals(folderPath)) {
            return "";
        }

        String cleanPath = folderPath.replaceAll("^/+", "")
                .replaceAll("/+$", "");

        return cleanPath.isEmpty() ? "" : cleanPath + "/";
    }

    /**
     * 确保桶存在
     */
    private void ensureBucketExists(String bucketName) throws Exception {
        if (!bucketExists(bucketName)) {
            if (autoCreateBucket) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
                log.info("创建MinIO桶：{}", bucketName);
            } else {
                throw new IllegalStateException("桶不存在且未启用自动创建：" + bucketName);
            }
        }
    }

    /**
     * 检查桶是否存在
     */
    private boolean bucketExists(String bucketName) throws Exception {
        return minioClient.bucketExists(
                BucketExistsArgs.builder()
                        .bucket(bucketName)
                        .build()
        );
    }

    /**
     * 根据fileId查找对象名称
     */
    private String findObjectByFileId(String bucketName, String fileId) {
        try {
            if (!bucketExists(bucketName)) {
                return null;
            }

            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucketName)
                            .recursive(true)
                            .build()
            );

            return StreamSupport.stream(results.spliterator(), false)
                    .map(result -> {
                        try {
                            return result.get().objectName();
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(objectName -> objectName != null && objectName.contains(fileId))
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.error("查找MinIO对象失败：bucket={}, fileId={}, error={}",
                    bucketName, fileId, e.getMessage());
            return null;
        }
    }

    /**
     * 从对象名提取原始文件名
     */
    private String extractFileNameFromObject(String objectName) {
        int lastSlash = objectName.lastIndexOf('/');
        return lastSlash >= 0 ? objectName.substring(lastSlash + 1) : objectName;
    }

    /**
     * 构建对象标签
     */
    private java.util.Map<String, String> buildObjectTags(String familyId, String uploaderUserId) {
        java.util.Map<String, String> tags = new java.util.HashMap<>();
        tags.put("familyId", familyId);
        tags.put("uploaderUserId", uploaderUserId);
        tags.put("uploadTime", String.valueOf(System.currentTimeMillis()));
        return tags;
    }

    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }

        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex > 0 && lastDotIndex < fileName.length() - 1) {
            return fileName.substring(lastDotIndex + 1).toLowerCase();
        }

        return "";
    }

    /**
     * 检查文件类型是否允许
     */
    private boolean isAllowedFileType(String fileName) {
        if (!StringUtils.hasText(allowedExtensions)) {
            return true;
        }

        String extension = getFileExtension(fileName);
        if (!StringUtils.hasText(extension)) {
            return false;
        }

        List<String> allowed = Arrays.asList(allowedExtensions.split(","));
        return allowed.stream().anyMatch(ext -> ext.trim().equalsIgnoreCase(extension));
    }

    /**
     * 根据文件名获取Content-Type
     */
    private String getContentType(String fileName) {
        String extension = getFileExtension(fileName);
        switch (extension) {
            case "pdf": return "application/pdf";
            case "doc": case "docx": return "application/msword";
            case "txt": return "text/plain";
            case "jpg": case "jpeg": return "image/jpeg";
            case "png": return "image/png";
            case "gif": return "image/gif";
            case "mp4": return "video/mp4";
            case "mp3": return "audio/mpeg";
            case "zip": return "application/zip";
            case "rar": return "application/x-rar-compressed";
            default: return "application/octet-stream";
        }
    }
}