package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.exception.FileUploadException;
import com.haven.storage.adapter.storage.StorageAdapter;
import com.haven.storage.domain.model.file.*;
import com.haven.storage.domain.builder.FileMetadataBuilder;
import com.haven.storage.service.base.BaseService;
import com.haven.storage.service.bean.FileUploadResultWithCode;
import com.haven.storage.validator.UnifiedFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 统一文件存储服务
 * <p>
 * 合并了 FileMetadataService 和 FamilyFileStorageService 的功能，
 * 提供统一的文件存储、元数据管理、搜索和统计服务。
 * <p>
 * 🎯 核心功能：
 * - 文件上传、下载、删除的完整生命周期管理
 * - 文件元数据的统一管理和持久化
 * - 家庭文件组织、搜索和统计
 * - 多级缓存策略提升性能
 * - 存储适配器模式支持多种存储后端
 * <p>
 * 💡 设计优势：
 * - 消除代码重复，统一数据源
 * - 元数据与物理文件状态一致性保证
 * - 统一的权限验证和缓存策略
 * - 便于事务管理和错误处理
 *
 * @author HavenButler
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FileStorageService extends BaseService {

    @Value("${storage.file.storage-type:local}")
    private String storageType;

    // 存储适配器实例（使用策略模式，Spring会自动选择合适的实现）
    private final StorageAdapter storageAdapter;

    // 统一文件验证器
    private final UnifiedFileValidator unifiedFileValidator;

    // 文件元数据构建器
    private final FileMetadataBuilder metadataBuilder;

    // 统一的文件元数据存储（实际应用中应替换为数据库）
    private final Map<String, FileMetadata> fileMetadataStore = new ConcurrentHashMap<>();

    // 按家庭分组的文件元数据缓存 - 提升查询性能
    private final Map<String, List<FileMetadata>> familyFilesCache = new ConcurrentHashMap<>();

    // 存储统计缓存
    private final Map<String, FamilyStorageStats> storageStatsCache = new ConcurrentHashMap<>();

    // ==================== 文件上传下载核心功能 ====================

    /**
     * 验证文件上传请求
     * <p>
     * 统一的文件上传验证入口，整合了所有验证逻辑：
     * - 用户身份认证和权限验证
     * - 文件基础信息验证（大小、类型、名称等）
     * - 文件可见性级别验证
     * - 文件夹路径验证
     *
     * @param request 文件上传请求
     */
    public void validateUploadRequest(FileUploadRequest request) {
        // 委托给统一文件验证器进行验证
        unifiedFileValidator.validateUploadRequest(request);
    }

    /**
     * 构建文件元数据
     * <p>
     * 统一的文件元数据构建入口，整合了元数据创建逻辑：
     * - 根据上传请求构建完整的文件元数据
     * - 设置存储类型和默认值
     * - 生成文件ID和时间戳
     *
     * @param request 文件上传请求
     * @return 构建完成的文件元数据
     */
    public FileMetadata buildFileMetadata(FileUploadRequest request) {
        log.debug("开始构建文件元数据: family={}, userId={}, file={}",
                request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName());

        // 委托给元数据构建器进行构建
        FileMetadata fileMetadata = metadataBuilder.buildFromRequest(request, getCurrentStorageType());

        log.debug("文件元数据构建完成: fileId={}, fileName={}, family={}",
                fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), fileMetadata.getFamilyId());

        return fileMetadata;
    }


    /**
     * 统一文件上传处理
     * <p>
     * 整合完整的文件上传流程：
     * - 请求验证和元数据构建
     * - 物理文件存储和元数据管理
     * - 缓存更新和统计信息维护
     * - 统一错误处理和日志记录
     * <p>
     * 💡 设计优势：
     * - 单一职责：完整处理文件上传流程
     * - 事务性：确保元数据和物理文件一致性
     * - 高性能：同步完成核心上传，异步处理附加任务
     *
     * @param request 文件上传请求
     * @return 包含文件信息的上传结果
     * @throws FileUploadException 当上传过程中发生异常时抛出
     */
    @TraceLog(value = "文件上传", module = "unified-file", type = "UPLOAD")
    public FileMetadata completeFileUpload(FileUploadRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.info("开始文件上传: family={}, userId={}, fileName={}, visibility={}, fileSize={}KB, traceId={}",
                request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName(),
                request.getVisibility(),
                request.getFile() != null ? request.getFile().getSize() / 1024 : 0,
                traceId);

        try {
            // 1. 验证上传请求（参数、权限、文件格式等）
            validateUploadRequest(request);

            // 2. 构建文件元数据（生成ID、设置默认值等）
            FileMetadata fileMetadata = buildFileMetadata(request);

            // 3. 保存文件元数据到持久化存储（提前保存，确保有记录）
            FileMetadata savedMetadata = saveFileMetadata(fileMetadata);

            // 4. 使用存储适配器上传物理文件
            FileUploadResult storageResult = storageAdapter.uploadFile(savedMetadata, request.getFile());

            if (!storageResult.isSuccess()) {
                // 物理文件上传失败，清理已保存的元数据
                deleteFileMetadata(savedMetadata.getFileId());
                throw new FileUploadException(
                    "物理文件上传失败: " + storageResult.getErrorMessage(),
                    request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName()
                );
            }

            // 5. 更新最终元数据（可能包含存储路径等信息）
            FileMetadata finalMetadata = savedMetadata;
            if (storageResult.getFileMetadata() != null) {
                finalMetadata = storageResult.getFileMetadata();
                // 更新保存的元数据
                updateFileMetadata(finalMetadata);
            }

            // 6. 缓存文件元数据到家庭缓存（提升后续查询性能）
            cacheFileMetadata(finalMetadata.getFamilyId(), finalMetadata);

            // 7. 更新存储统计信息
            updateStorageStats(finalMetadata.getFamilyId(), request.getFile().getSize(), 1);

            log.info("文件上传完成: fileId={}, fileName={}, family={}, fileSize={}bytes, storageType={}, traceId={}",
                    finalMetadata.getFileId(), finalMetadata.getOriginalFileName(),
                    finalMetadata.getFamilyId(), finalMetadata.getFileSize(),
                    getCurrentStorageType(), traceId);

            return finalMetadata;

        } catch (ValidationException | AuthException | BusinessException e) {
            // 业务异常，重新抛出由全局异常处理器处理
            throw e;

        } catch (Exception e) {
            // 系统异常，包装为FileUploadException
            log.error("文件上传系统异常: family={}, fileName={}, storageType={}, error={}, traceId={}",
                    request.getFamilyId(), request.getOriginalFileName(),
                    getCurrentStorageType(), e.getMessage(), traceId, e);

            throw new FileUploadException(
                "文件上传过程中发生系统异常: " + e.getMessage(),
                request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName()
            );
        }
    }


    /**
     * 下载家庭文件
     */
    @TraceLog(value = "下载家庭文件", module = "unified-file", type = "DOWNLOAD")
    public FileDownloadResult downloadFile(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 验证文件是否存在且有权限访问
            FileMetadata metadata = getFileMetadata(fileId, familyId);
            if (metadata == null) {
                return FileDownloadResult.failure("文件不存在或无权限访问");
            }

            // 2. 使用存储适配器下载物理文件
            FileDownloadResult result = storageAdapter.downloadFile(fileId, familyId);

            if (result.isSuccess()) {
                // 3. 更新访问统计
                updateAccessStats(metadata);

                log.info("文件下载成功: family={}, fileId={}, size={}bytes, storageType={}, traceId={}",
                        familyId, fileId, result.getFileContent().length,
                        storageAdapter.getStorageType(), traceId);
            }

            return result;

        } catch (Exception e) {
            log.error("文件下载失败: family={}, fileId={}, storageType={}, error={}, traceId={}",
                    familyId, fileId, storageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileDownloadResult.failure("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除家庭文件
     */
    @TraceLog(value = "删除家庭文件", module = "unified-file", type = "DELETE")
    public FileDeleteResult deleteFile(String fileId, String familyId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 获取文件元数据（用于统计更新）
            FileMetadata metadata = getFileMetadata(fileId, familyId);
            if (metadata == null) {
                return FileDeleteResult.failure("文件不存在");
            }

            // 2. 权限验证
            if (!userId.equals(metadata.getOwnerId())) {
                return FileDeleteResult.failure("无权限删除此文件");
            }

            // 3. 使用存储适配器删除物理文件
            boolean deleted = storageAdapter.deleteFile(fileId, familyId);

            if (deleted) {
                // 4. 软删除元数据
                softDeleteFileMetadata(fileId);

                // 5. 从缓存中移除
                removeFileFromCache(familyId, fileId);

                // 6. 更新存储统计
                updateStorageStats(familyId, -metadata.getFileSize(), -1);

                log.info("文件删除成功: family={}, fileId={}, storageType={}, traceId={}",
                        familyId, fileId, storageAdapter.getStorageType(), traceId);

                return FileDeleteResult.success(metadata.getOriginalName(), traceId);
            } else {
                return FileDeleteResult.failure("文件删除失败");
            }

        } catch (Exception e) {
            log.error("文件删除失败: family={}, fileId={}, storageType={}, error={}, traceId={}",
                    familyId, fileId, storageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileDeleteResult.failure("文件删除失败: " + e.getMessage());
        }
    }

    // ==================== 文件查询和搜索功能 ====================

    /**
     * 获取家庭文件列表
     */
    @TraceLog(value = "获取家庭文件列表", module = "unified-file", type = "LIST")
    public FamilyFileList getFamilyFiles(String familyId, String folderPath) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<FileMetadata> allFiles = getFamilyFileMetadata(familyId);

            // 按文件夹过滤
            List<FileMetadata> folderFiles = allFiles.stream()
                    .filter(file -> folderPath.equals(file.getFolderPath()))
                    .sorted(Comparator.comparing(FileMetadata::getUploadTime).reversed())
                    .collect(Collectors.toList());

            // 获取文件夹列表
            Set<String> subFolders = allFiles.stream()
                    .map(FileMetadata::getFolderPath)
                    .filter(path -> path.startsWith(folderPath) && !path.equals(folderPath))
                    .collect(Collectors.toSet());

            FamilyFileList fileList = new FamilyFileList();
            fileList.setFamilyId(familyId);
            fileList.setCurrentPath(folderPath);
            fileList.setFiles(folderFiles);
            fileList.setSubFolders(new ArrayList<>(subFolders));
            fileList.setTotalFiles(folderFiles.size());
            fileList.setTotalSize(folderFiles.stream().mapToLong(FileMetadata::getFileSize).sum());
            fileList.setTraceId(traceId);

            log.info("文件列表获取成功: family={}, folder={}, files={}, traceId={}",
                    familyId, folderPath, folderFiles.size(), traceId);

            return fileList;

        } catch (Exception e) {
            log.error("获取文件列表失败: family={}, folder={}, error={}, traceId={}",
                    familyId, folderPath, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 获取用户的文件列表
     */
    public List<FileMetadata> getUserFiles(String userId, String familyId, boolean includeDeleted) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> userId.equals(file.getOwnerId()))
                    .filter(file -> includeDeleted || file.getDeleted() != 1)
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("获取用户文件列表失败: userId={}, familyId={}, error={}, traceId={}",
                    userId, familyId, e.getMessage(), traceId, e);
            return List.of();
        }
    }

    /**
     * 根据文件类型获取文件列表
     */
    public List<FileMetadata> getFilesByType(String familyId, String fileType) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataStore.values().stream()
                    .filter(file -> familyId.equals(file.getFamilyId()))
                    .filter(file -> file.getDeleted() != 1)
                    .filter(file -> file.getFileType() != null && file.getFileType().startsWith(fileType))
                    .sorted((f1, f2) -> f2.getCreateTime().compareTo(f1.getCreateTime()))
                    .toList();

        } catch (Exception e) {
            log.error("根据类型获取文件列表失败: familyId={}, fileType={}, error={}, traceId={}",
                    familyId, fileType, e.getMessage(), traceId);
            return List.of();
        }
    }

    /**
     * 搜索家庭文件
     */
    @TraceLog(value = "搜索家庭文件", module = "unified-file", type = "SEARCH")
    public FileSearchResult searchFiles(String familyId, String keyword) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<FileMetadata> allFiles = getFamilyFileMetadata(familyId);

            // 按文件名搜索
            List<FileMetadata> matchedFiles = allFiles.stream()
                    .filter(file -> matchesSearchKeyword(file, keyword))
                    .sorted(Comparator.comparing(FileMetadata::getUploadTime).reversed())
                    .collect(Collectors.toList());

            FileSearchResult searchResult = new FileSearchResult();
            searchResult.setFamilyId(familyId);
            searchResult.setKeyword(keyword);
            searchResult.setMatchedFiles(matchedFiles);
            searchResult.setTotalMatches(matchedFiles.size());
            searchResult.setTraceId(traceId);

            log.info("文件搜索完成: family={}, keyword={}, matches={}, traceId={}",
                    familyId, keyword, matchedFiles.size(), traceId);

            return searchResult;

        } catch (Exception e) {
            log.error("文件搜索失败: family={}, keyword={}, error={}, traceId={}",
                    familyId, keyword, e.getMessage(), traceId);
            throw e;
        }
    }

    // ==================== 文件元数据管理功能 ====================

    /**
     * 保存文件元数据
     */
    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 设置默认值
            if (fileMetadata.getCreateTime() == null) {
                fileMetadata.setCreateTime(LocalDateTime.now());
            }
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileMetadata.getFileId(), fileMetadata);

            log.debug("文件元数据保存成功: fileId={}, fileName={}, familyId={}, traceId={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), fileMetadata.getFamilyId(), traceId);
            return fileMetadata;

        } catch (Exception e) {
            log.error("保存文件元数据失败: fileName={}, error={}, traceId={}",
                    fileMetadata.getOriginalFileName(), e.getMessage(), traceId);
            throw new RuntimeException("保存文件元数据失败", e);
        }
    }

    /**
     * 根据文件ID获取文件元数据
     */
    public FileMetadata getFileMetadata(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            FileMetadata fileMetadata = fileMetadataStore.get(fileId);

            if (fileMetadata != null) {
                log.debug("文件元数据获取成功: fileId={}, fileName={}, traceId={}",
                        fileId, fileMetadata.getOriginalFileName(), traceId);
            } else {
                log.debug("文件元数据不存在: fileId={}, traceId={}", fileId, traceId);
            }

            return fileMetadata;

        } catch (Exception e) {
            log.error("获取文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId);
            return null;
        }
    }

    /**
     * 根据文件ID和家庭ID获取文件元数据（带权限验证）
     */
    public FileMetadata getFileMetadata(String fileId, String familyId) {
        FileMetadata metadata = getFileMetadata(fileId);
        if (metadata == null || !familyId.equals(metadata.getFamilyId())) {
            return null;
        }
        return metadata;
    }

    /**
     * 更新文件元数据
     */
    public FileMetadata updateFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            if (fileMetadata.getFileId() == null || !fileMetadataStore.containsKey(fileMetadata.getFileId())) {
                throw new IllegalArgumentException("文件不存在: " + fileMetadata.getFileId());
            }

            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileMetadata.getFileId(), fileMetadata);

            // 同时更新缓存
            updateCacheAfterMetadataUpdate(fileMetadata);

            log.info("文件元数据更新成功: fileId={}, fileName={}, traceId={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), traceId);

            return fileMetadata;

        } catch (Exception e) {
            log.error("更新文件元数据失败: fileId={}, error={}, traceId={}",
                    fileMetadata.getFileId(), e.getMessage(), traceId);
            throw new RuntimeException("更新文件元数据失败", e);
        }
    }

    /**
     * 软删除文件元数据
     */
    public boolean deleteFileMetadata(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            FileMetadata fileMetadata = fileMetadataStore.get(fileId);
            if (fileMetadata == null) {
                log.warn("删除文件失败 - 文件不存在: fileId={}, traceId={}", fileId, traceId);
                return false;
            }

            // 软删除
            fileMetadata.setDeleted(1);
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileId, fileMetadata);

            // 更新缓存
            updateCacheAfterMetadataUpdate(fileMetadata);

            log.info("文件元数据删除成功: fileId={}, fileName={}, traceId={}",
                    fileId, fileMetadata.getOriginalFileName(), traceId);

            return true;

        } catch (Exception e) {
            log.error("删除文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId);
            return false;
        }
    }

    // ==================== 存储统计功能 ====================

    /**
     * 获取家庭存储统计
     */
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        FamilyStorageStats stats = storageStatsCache.computeIfAbsent(familyId, this::calculateStorageStats);
        stats.setStorageType(storageAdapter.getStorageType());
        stats.setStorageHealthy(storageAdapter.isHealthy());
        return stats;
    }

    // ==================== 工具方法 ====================

    /**
     * 获取当前存储类型
     */
    public String getCurrentStorageType() {
        return storageAdapter.getStorageType();
    }

    /**
     * 检查存储健康状态
     */
    public boolean isStorageHealthy() {
        return storageAdapter.isHealthy();
    }

    /**
     * 获取文件访问URL
     */
    public String getFileAccessUrl(String fileId, String familyId, int expireMinutes) {
        return storageAdapter.getFileAccessUrl(fileId, familyId, expireMinutes);
    }

    /**
     * 动态切换存储适配器（已弃用，因为使用策略模式由Spring自动管理）
     */
    public boolean switchStorageAdapter(String newStorageType) {
        log.warn("存储适配器动态切换功能已弃用。当前使用的是：{}", storageAdapter.getStorageType());
        return false;
    }

    // ==================== 私有方法实现 ====================

    /**
     * 缓存文件元数据到家庭缓存
     */
    private void cacheFileMetadata(String familyId, FileMetadata metadata) {
        familyFilesCache.computeIfAbsent(familyId, k -> new ArrayList<>()).add(metadata);
    }

    /**
     * 从家庭缓存中获取文件元数据
     */
    private List<FileMetadata> getFamilyFileMetadata(String familyId) {
        return familyFilesCache.getOrDefault(familyId, new ArrayList<>());
    }

    /**
     * 从缓存中获取特定文件元数据
     */
    private FileMetadata getFileMetadataFromCache(String familyId, String fileId) {
        return getFamilyFileMetadata(familyId).stream()
                .filter(file -> file.getFileId().equals(fileId))
                .findFirst()
                .orElse(null);
    }

    /**
     * 从缓存中移除文件
     */
    private void removeFileFromCache(String familyId, String fileId) {
        List<FileMetadata> files = familyFilesCache.get(familyId);
        if (files != null) {
            files.removeIf(file -> file.getFileId().equals(fileId));
        }
    }

    /**
     * 更新元数据后同步缓存
     */
    private void updateCacheAfterMetadataUpdate(FileMetadata updatedMetadata) {
        String familyId = updatedMetadata.getFamilyId();
        String fileId = updatedMetadata.getFileId();

        List<FileMetadata> familyFiles = familyFilesCache.get(familyId);
        if (familyFiles != null) {
            for (int i = 0; i < familyFiles.size(); i++) {
                if (familyFiles.get(i).getFileId().equals(fileId)) {
                    familyFiles.set(i, updatedMetadata);
                    break;
                }
            }
        }
    }

    /**
     * 更新访问统计
     */
    private void updateAccessStats(FileMetadata metadata) {
        metadata.setLastAccessTime(LocalDateTime.now());
        metadata.setAccessCount(metadata.getAccessCount() + 1);
        updateFileMetadata(metadata);
    }

    /**
     * 更新存储统计
     */
    private void updateStorageStats(String familyId, long sizeChange, int fileCountChange) {
        FamilyStorageStats stats = storageStatsCache.computeIfAbsent(familyId, this::calculateStorageStats);
        stats.setTotalSize(stats.getTotalSize() + sizeChange);
        stats.setTotalFiles(stats.getTotalFiles() + fileCountChange);
        stats.setLastUpdated(LocalDateTime.now());
    }

    /**
     * 计算存储统计
     */
    private FamilyStorageStats calculateStorageStats(String familyId) {
        List<FileMetadata> files = getFamilyFileMetadata(familyId);

        FamilyStorageStats stats = new FamilyStorageStats();
        stats.setFamilyId(familyId);
        stats.setTotalFiles(files.size());
        stats.setTotalSize(files.stream().mapToLong(FileMetadata::getFileSize).sum());
        stats.setLastUpdated(LocalDateTime.now());

        // 按类型统计
        Map<String, Integer> filesByType = files.stream()
                .collect(Collectors.groupingBy(
                        FileMetadata::getFileType,
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        stats.setFilesByType(filesByType);
        stats.setStorageType(storageAdapter.getStorageType());
        stats.setStorageHealthy(storageAdapter.isHealthy());

        return stats;
    }

    /**
     * 软删除文件元数据
     */
    private void softDeleteFileMetadata(String fileId) {
        FileMetadata fileMetadata = fileMetadataStore.get(fileId);
        if (fileMetadata != null) {
            fileMetadata.setDeleted(1);
            fileMetadata.setUpdateTime(LocalDateTime.now());
            fileMetadataStore.put(fileId, fileMetadata);
            updateCacheAfterMetadataUpdate(fileMetadata);
        }
    }

    /**
     * 检查文件是否匹配搜索关键词
     */
    private boolean matchesSearchKeyword(FileMetadata file, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return false;
        }

        String lowercaseKeyword = keyword.toLowerCase().trim();

        // 检查文件名
        String fileName = file.getOriginalFileName();
        if (fileName != null && fileName.toLowerCase().contains(lowercaseKeyword)) {
            return true;
        }

        // 检查标签
        List<String> tags = file.getTags();
        if (tags != null) {
            return tags.stream()
                    .anyMatch(tag -> tag != null && tag.toLowerCase().contains(lowercaseKeyword));
        }

        // 检查描述
        String description = file.getDescription();
        if (description != null && description.toLowerCase().contains(lowercaseKeyword)) {
            return true;
        }

        return false;
    }
}