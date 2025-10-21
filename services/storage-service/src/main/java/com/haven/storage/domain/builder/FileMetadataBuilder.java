package com.haven.storage.domain.builder;

import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.domain.model.file.FileUploadResult;
import com.haven.storage.domain.model.file.FileVisibility;
import com.haven.storage.security.UserContext;
import com.haven.storage.utils.FileTypeDetector;
import com.haven.storage.utils.FileUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 文件元数据构建器
 * <p>
 * 负责从上传请求构建文件元数据对象
 * 遵循base-model的数据构建规范
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class FileMetadataBuilder {

    private final FileTypeDetector fileTypeDetector;

    public FileMetadataBuilder(FileTypeDetector fileTypeDetector) {
        this.fileTypeDetector = fileTypeDetector;
    }

    /**
     * 从上传请求构建文件元数据
     *
     * @param request            文件上传请求
     * @param currentStorageType 当前存储类型
     * @return 构建完成的文件元数据
     * @throws SystemException 当构建过程中发生系统异常时抛出
     */
    public FileMetadata buildFromRequest(FileUploadRequest request, String currentStorageType) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        log.debug("开始构建文件元数据: family={}, userId={}, file={}",
                request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName());

        try {
            // 确保使用JWT上下文中的用户信息
            String currentUserId = UserContext.getCurrentUserId();
            String currentFamilyId = UserContext.getCurrentFamilyId();
            String uploaderUserId = currentUserId != null ? currentUserId : request.getUploaderUserId();
            String familyId = currentFamilyId != null ? currentFamilyId : request.getFamilyId();
            // 构建基础元数据
            FileMetadata metadata = new FileMetadata();
            // 1. 设置基础信息
            setBasicInfo(metadata, request, FileUtils.generateFileId(), familyId, uploaderUserId, traceId);
            // 2. 设置文件相关信息
            setFileInfo(metadata, request, traceId);
            // 3. 设置权限相关信息
            setPermissionInfo(metadata, request, currentUserId, traceId);
            // 4. 设置存储相关信息
            setStorageInfo(metadata, currentStorageType, traceId);
            // 5. 设置预览相关信息
            setPreviewInfo(metadata, traceId);
            // 6. 设置元数据
            setCustomMetadata(metadata, request, traceId);
            log.info("文件元数据构建完成: fileId={}, fileName={}, familyId={}, userId={}, traceId={}",
                    metadata.getFileId(), metadata.getFileName(), metadata.getFamilyId(),
                    metadata.getUploaderUserId(), traceId);
            log.debug("文件元数据构建完成: fileId={}, fileName={}, family={}",
                    metadata.getFileId(), metadata.getOriginalFileName(), metadata.getFamilyId());

            return metadata;

        } catch (Exception e) {
            log.error("文件元数据构建失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.CACHE_ERROR, e);
        }
    }

    /**
     * 设置基础信息
     */
    private void setBasicInfo(FileMetadata metadata, FileUploadRequest request, String fileID,
                              String familyId, String uploaderUserId, String traceId) {
        try {
            metadata.setFileId(fileID);
            // 设置数字辅助ID（基于时间戳，确保唯一性）
            metadata.setNumericId(System.currentTimeMillis());
            metadata.setFamilyId(familyId);
            metadata.setOriginalName(request.getOriginalFileName());
            metadata.setFileName(generateUniqueFileName(request.getOriginalFileName()));
            metadata.setFolderPath(request.getFolderPath() != null ? request.getFolderPath() : "/");
            metadata.setUploaderUserId(uploaderUserId);
            metadata.setUploadTime(LocalDateTime.now());
            metadata.setLastAccessTime(LocalDateTime.now());
            metadata.setAccessCount(0);

            log.debug("基础信息设置完成: fileId={}, numericId={}, familyId={}, fileName={}, uploaderUserId={}, traceId={}",
                    fileID, metadata.getNumericId(), familyId, metadata.getFileName(), uploaderUserId, traceId);

        } catch (Exception e) {
            log.error("设置基础信息失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 设置文件相关信息
     */
    private void setFileInfo(FileMetadata metadata, FileUploadRequest request, String traceId) {
        try {
            metadata.setFileSize(request.getFileSize());

            // 使用统一的文件类型检测器
            FileTypeDetector.FileTypeDetectionResult detectionResult =
                    fileTypeDetector.detectFileType(request.getOriginalFileName(), request.getContentType(), traceId);

            // 设置检测到的文件类型信息
            metadata.setFileType(detectionResult.getCategory());
            metadata.setMimeType(detectionResult.getDetectedMimeType());
            metadata.setContentType(request.getContentType());

            log.info("文件类型检测完成: fileName={}, originalMimeType={}, detectedMimeType={}, fileType={}, detectionMethod={}, traceId={}",
                    request.getOriginalFileName(), detectionResult.getOriginalMimeType(),
                    detectionResult.getDetectedMimeType(), detectionResult.getCategory(),
                    detectionResult.getDetectionMethod(), traceId);

            log.debug("文件信息设置完成: fileSize={}, fileType={}, contentType={}, traceId={}",
                    metadata.getFileSize(), metadata.getFileType(), metadata.getContentType(), traceId);

        } catch (Exception e) {
            log.error("设置文件信息失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 设置权限相关信息
     */
    private void setPermissionInfo(FileMetadata metadata, FileUploadRequest request,
                                   String currentUserId, String traceId) {
        try {
            // 优先使用JWT上下文的用户ID作为所有者
            String ownerId = currentUserId != null ? currentUserId : request.getEffectiveOwnerId();
            metadata.setOwnerId(ownerId);
            metadata.setFileVisibility(request.getVisibility());

            log.debug("权限信息设置完成: ownerId={}, accessLevel={}, traceId={}",
                    ownerId, metadata.getFileVisibility(), traceId);

        } catch (Exception e) {
            log.error("设置权限信息失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 设置存储相关信息
     */
    private void setStorageInfo(FileMetadata metadata, String currentStorageType, String traceId) {
        try {
            metadata.setStoragePath(""); // 待上传后设置
            metadata.setStorageType(currentStorageType);

            log.debug("存储信息设置完成: storageType={}, traceId={}", currentStorageType, traceId);

        } catch (Exception e) {
            log.error("设置存储信息失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 设置预览相关信息
     */
    private void setPreviewInfo(FileMetadata metadata, String traceId) {
        try {
            metadata.setThumbnailPath(null);
            metadata.setHasPreview(false);

            log.debug("预览信息设置完成: traceId={}", traceId);

        } catch (Exception e) {
            log.error("设置预览信息失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }

    /**
     * 设置自定义元数据
     */
    private void setCustomMetadata(FileMetadata metadata, FileUploadRequest request, String traceId) {
        try {
            metadata.setDescription(request.getDescription());
            metadata.setTags(request.getTags());

            log.debug("自定义元数据设置完成: description={}, tags={}, traceId={}",
                    metadata.getDescription(),
                    metadata.getTags(), traceId);

        } catch (Exception e) {
            log.error("设置自定义元数据失败: traceId={}, error={}", traceId, e.getMessage(), e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, e);
        }
    }



    /**
     * 生成唯一的文件名
     *
     * @param originalName 原始文件名
     * @return 唯一的文件名
     */
    private String generateUniqueFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "unnamed_file_" + System.currentTimeMillis();
        }

        // 获取文件扩展名
        int lastDotIndex = originalName.lastIndexOf('.');
        String name = lastDotIndex > 0 ? originalName.substring(0, lastDotIndex) : originalName;
        String extension = lastDotIndex > 0 ? originalName.substring(lastDotIndex) : "";

        // 移除特殊字符并添加时间戳
        String cleanName = name.replaceAll("[^a-zA-Z0-9_\\-\\u4e00-\\u9fa5]", "_");
        String timestamp = String.valueOf(System.currentTimeMillis());

        return cleanName + "_" + timestamp + extension;
    }



}