package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.exception.AuthException;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.exception.ValidationException;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.adapter.storage.StorageAdapter;
import com.haven.storage.domain.builder.FileMetadataBuilder;
import com.haven.storage.domain.model.file.*;
import com.haven.storage.exception.FileUploadException;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.service.base.BaseService;
import com.haven.storage.service.cache.FileMetadataCacheService;
import com.haven.storage.utils.FileUtils;
import com.haven.storage.validator.UnifiedFileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.haven.storage.security.UserContext;

/**
 * 统一文件存储服务
 * <p>
 * 基于PostgreSQL+Redis+MinIO架构的文件存储服务：
 * - PostgreSQL：文件元数据持久化存储
 * - Redis：多级缓存提升性能
 * - MinIO：对象存储物理文件
 * <p>
 * 🎯 核心功能：
 * - 文件上传、下载、删除的完整生命周期管理
 * - 文件元数据的统一管理和事务保证
 * - 家庭文件组织、搜索和统计
 * - 多级缓存策略提升性能
 * - 存储适配器模式支持多种存储后端
 * <p>
 * 💡 设计优势：
 * - 数据持久化：PostgreSQL保证数据安全
 * - 高性能缓存：Redis提供多级缓存
 * - 事务一致性：确保元数据和物理文件状态一致
 * - 统一的权限验证和缓存策略
 * - 支持水平扩展的分布式架构
 *
 * @author HavenButler
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FileStorageService extends BaseService {


    private final StorageAdapter storageAdapter;// 存储适配器实例（使用策略模式，Spring会自动选择合适的实现）
    private final UnifiedFileValidator unifiedFileValidator; // 统一文件验证器
    private final FileMetadataBuilder metadataBuilder;// 文件元数据构建器
    private final FileMetadataRepository fileMetadataRepository; // Spring Data JPA数据访问层
    private final FileMetadataCacheService cacheService;// Redis缓存服务

    // 家庭存储统计服务
    private final FamilyStorageStatsService familyStorageStatsService;

    // ==================== 文件上传下载核心功能 ====================

    /**
     * 统一文件上传处理
     * <p>
     * 基于PostgreSQL+Redis+MinIO架构的文件上传流程：
     * - 请求验证和元数据构建
     * - 事务性保存元数据到PostgreSQL
     * - 物理文件存储到MinIO
     * - Redis缓存更新和统计信息维护
     * - 统一错误处理和日志记录
     * <p>
     * 💡 设计优势：
     * - 事务一致性：确保元数据和物理文件状态一致
     * - 高性能：Redis缓存提升查询性能
     * - 数据安全：PostgreSQL持久化保证数据不丢失
     * - 分布式：支持水平扩展和负载均衡
     *
     * @param request 文件上传请求
     * @return 包含文件信息的上传结果
     * @throws FileUploadException 当上传过程中发生异常时抛出
     */
    @TraceLog(value = "文件上传", module = "unified-file", type = "UPLOAD")
    @Transactional
    public FileMetadata completeFileUpload(FileUploadRequest request) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        log.info("开始文件上传: family={}, userId={}, fileName={}, visibility={}, fileSize={}KB, traceId={}",
                request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName(),
                request.getVisibility(),
                request.getFile() != null ? request.getFile().getSize() / 1024 : 0,
                traceId);

        try {
            // 1. 验证上传请求（参数、权限、文件格式等）
            unifiedFileValidator.validateUploadRequest(request);
            // 2. 构建文件元数据（生成ID、设置默认值等）
            FileMetadata fileMetadata = metadataBuilder.buildFromRequest(request, getCurrentStorageType());
            // 3. 保存文件元数据到PostgreSQL（事务内）
            fileMetadata = saveFileMetadata(fileMetadata);
            // 4. 使用存储适配器上传物理文件到MinIO
            FileUploadResult storageResult = storageAdapter.uploadFile(fileMetadata, request.getFile());
            if (!storageResult.isSuccess()) {
                // 物理文件上传失败，事务会回滚自动清理元数据
                throw new FileUploadException("物理文件上传失败: " + storageResult.getErrorMessage(), request.getFamilyId(), request.getUploaderUserId(), request.getOriginalFileName());
            }
            // 5. 更新最终元数据（可能包含存储路径等信息）
            fileMetadata = updateFileMetadata(storageResult.getFileMetadata());
            // 6. 缓存文件元数据到Redis（提升后续查询性能）
            cacheService.cacheFileMetadata(fileMetadata);
            // 7. 更新家庭存储统计信息（文件上传成功）
            familyStorageStatsService.onFileUploaded(fileMetadata);

            // 8. 清理家庭相关缓存（因为文件列表发生变化）
            // 清理所有缓存以确保数据一致性
            cacheService.evictFileMetadata(fileMetadata.getFileId());
            cacheService.evictAllCache();

            log.info("文件上传完成: fileId={}, fileName={}, family={}, traceId={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(),
                    fileMetadata.getFamilyId(), traceId);
            return fileMetadata;

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
     * 下载家庭文件 - 支持流式传输
     * <p>
     * 基于UserContext的文件下载流程：
     * 1. 从报文头获取用户ID和家庭ID
     * 2. 先从缓存中获取文件的元数据，没有的话在数据库中查询
     * 3. 通过用户ID或家庭ID去匹配文件，校验当前用户是否有权限读取此文件
     * 4. 对于元数据进行判断是否存在存储的信息，物理存储的地址的进行判断
     * 5. 通过storageAdapter适配去寻找和文件，使用存储适配器下载物理文件（支持流式传输）
     * 6. 返回包含文件资源和元数据的下载结果
     */
    @TraceLog(value = "下载家庭文件", module = "unified-file", type = "DOWNLOAD")
    public FileDownloadResult downloadFile(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 先通过报文头获取用户ID和家庭ID
            String currentUserId = UserContext.getCurrentUserId();
            String currentFamilyId = UserContext.getCurrentFamilyId();

            log.info("开始文件下载: fileId={}, requestFamilyId={}, currentUserId={}, currentFamilyId={}, traceId={}",
                    fileId, familyId, currentUserId, currentFamilyId, traceId);

            // 2. 先从缓存中获取文件的元数据，没有的话在数据库中查询
            FileMetadata metadata = getFileMetadata(fileId);
            if (metadata == null) {
                return FileDownloadResult.failure("文件不存在");
            }

            // 3. 通过用户ID或家庭ID去匹配这个文件，当前用户是否有权限读取这个文件
            if (!unifiedFileValidator.validateFileReadPermission(metadata, currentUserId, currentFamilyId, familyId)) {
                log.warn("用户无权限访问文件: fileId={}, currentUserId={}, currentFamilyId={}, fileFamilyId={}, fileOwnerId={}, traceId={}",
                        fileId, currentUserId, currentFamilyId, metadata.getFamilyId(), metadata.getOwnerId(), traceId);
                return FileDownloadResult.failure("无权限访问此文件");
            }

            // 4. 对于元数据进行判断是否存在存储的信息，物理存储的地址的进行判断
            if (!unifiedFileValidator.isValidStorageMetadata(metadata)) {
                return FileDownloadResult.failure("文件存储信息不完整或已损坏");
            }

            // 5. 通过storageAdapter适配去寻找和文件，使用存储适配器下载物理文件
            FileDownloadResult storageResult = storageAdapter.downloadFile(fileId, metadata.getFamilyId());
            storageResult.setFileMetadata(metadata);
            // 6. 更新访问统计
            updateAccessStatsAsync(metadata);
            log.info("文件下载成功: fileId={}", storageResult);
            return storageResult;
        } catch (Exception e) {
            log.error("文件下载失败: fileId={}, familyId={}, storageType={}, error={}, traceId={}",
                    fileId, familyId, storageAdapter.getStorageType(), e.getMessage(), traceId, e);
            return FileDownloadResult.failure("文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除家庭文件
     * <p>
     * 基于PostgreSQL+Redis架构的文件删除流程：
     * - 事务性软删除元数据到PostgreSQL
     * - 从MinIO删除物理文件
     * - 清理Redis相关缓存
     * - 更新统计信息
     */
    @TraceLog(value = "删除家庭文件", module = "unified-file", type = "DELETE")
    @Transactional
    public FileDeleteResult deleteFile(String fileId, String familyId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 获取文件元数据（用于权限验证和统计更新）
            FileMetadata metadata = getFileMetadataFromDatabase(fileId, familyId);
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
                // 4. 软删除元数据到PostgreSQL（事务内）
                softDeleteFileMetadataInDatabase(fileId);

                // 5. 更新家庭存储统计信息（文件删除）
                familyStorageStatsService.onFileDeleted(metadata);

                // 6. 清理Redis相关缓存
                cacheService.evictFileMetadata(fileId);
                cacheService.evictAllCache();

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
            // 从PostgreSQL获取所有未删除的家庭文件
            List<FileMetadata> allFiles = fileMetadataRepository.findActiveFilesByFamily(familyId);
            log.info("获取到家庭文件总数: familyId={}, allFiles.size={}, folderPath={}, traceId={}",
                    familyId, allFiles.size(), folderPath, traceId);
            allFiles.forEach(file -> log.debug("文件路径信息: fileId={}, folderPath={}, originalName={}, traceId={}",
                    file.getFileId(), file.getFolderPath(), file.getOriginalFileName(), traceId));

            // 标准化路径格式，确保路径匹配的一致性
            String formatFolderPath = FileUtils.formatFolderPath(folderPath);
            log.info("标准化后的路径: originalPath={}, normalizedPath={}, traceId={}",
                    folderPath, folderPath, traceId);

            // 按文件夹过滤（使用标准化路径进行匹配）
            List<FileMetadata> folderFiles = allFiles.stream()
                    .filter(file -> "/".equals(formatFolderPath) || formatFolderPath.equals(FileUtils.formatFolderPath(file.getFolderPath())))
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
     * 搜索家庭文件
     * <p>
     * 基于PostgreSQL+Redis架构的文件搜索：
     * - 先检查Redis缓存中的搜索结果
     * - 缓存未命中时从PostgreSQL进行全文搜索
     * - 支持按文件名、描述、标签搜索
     * - 搜索结果会缓存提升后续搜索性能
     */
    @TraceLog(value = "搜索家庭文件", module = "unified-file", type = "SEARCH")
    public FileSearchResult searchFiles(String familyId, String keyword) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 1. 先检查Redis缓存
            Optional<Object> cachedResult = cacheService.getCachedSearchResult(familyId, keyword);
            if (cachedResult.isPresent() && cachedResult.get() instanceof FileSearchResult) {
                return (FileSearchResult) cachedResult.get();
            }

            // 2. 缓存未命中，从PostgreSQL搜索
            List<FileMetadata> matchedFiles = searchFilesFromDatabase(familyId, keyword);

            // 3. 构建搜索结果
            FileSearchResult searchResult = new FileSearchResult();
            searchResult.setFamilyId(familyId);
            searchResult.setKeyword(keyword);
            searchResult.setMatchedFiles(matchedFiles);
            searchResult.setTotalMatches(matchedFiles.size());
            searchResult.setTraceId(traceId);

            // 4. 缓存搜索结果
            cacheService.cacheSearchResult(familyId, keyword, searchResult);

            log.info("文件搜索完成: family={}, keyword={}, matches={}, traceId={}",
                    familyId, keyword, matchedFiles.size(), traceId);

            return searchResult;

        } catch (Exception e) {
            log.error("文件搜索失败: family={}, keyword={}, error={}, traceId={}",
                    familyId, keyword, e.getMessage(), traceId);
            return createEmptySearchResult(familyId, keyword, traceId);
        }
    }

    /**
     * 从PostgreSQL搜索文件
     */
    private List<FileMetadata> searchFilesFromDatabase(String familyId, String keyword) {
        try {
            // 使用PostgreSQL的全文搜索功能
            return fileMetadataRepository.searchFiles(familyId, keyword, null)
                    .getContent()
                    .stream()
                    .toList();

        } catch (Exception e) {
            log.error("数据库搜索失败: familyId={}, keyword={}, error={}", familyId, keyword, e.getMessage());
            return List.of();
        }
    }

    /**
     * 创建空的搜索结果
     */
    private FileSearchResult createEmptySearchResult(String familyId, String keyword, String traceId) {
        FileSearchResult searchResult = new FileSearchResult();
        searchResult.setFamilyId(familyId);
        searchResult.setKeyword(keyword);
        searchResult.setMatchedFiles(List.of());
        searchResult.setTotalMatches(0);
        searchResult.setTraceId(traceId);
        return searchResult;
    }

    // ==================== 文件元数据管理功能 ====================

    /**
     * 保存文件元数据到PostgreSQL
     */
    public FileMetadata saveFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 设置默认值
            if (fileMetadata.getCreateTime() == null) {
                fileMetadata.setCreateTime(LocalDateTime.now());
            }
            fileMetadata.setUpdateTime(LocalDateTime.now());

            fileMetadata = fileMetadataRepository.save(fileMetadata);

            // 缓存文件元数据
            cacheService.cacheFileMetadata(fileMetadata);

            log.debug("文件元数据保存成功: fileMetadata={}", fileMetadata);
            return fileMetadata;

        } catch (Exception e) {
            log.error("保存文件元数据失败: fileName={}, error={}, traceId={}",
                    fileMetadata.getOriginalFileName(), e.getMessage(), traceId);
            throw new RuntimeException("保存文件元数据失败", e);
        }
    }

    /**
     * 从PostgreSQL获取文件元数据
     */
    private FileMetadata getFileMetadataFromDatabase(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            return fileMetadataRepository.findActiveFileByIdAndFamily(fileId, familyId)
                    .orElse(null);

        } catch (Exception e) {
            log.error("获取文件元数据失败: fileId={}, familyId={}, error={}, traceId={}",
                    fileId, familyId, e.getMessage(), traceId);
            return null;
        }
    }

    /**
     * 更新文件元数据到PostgreSQL
     */
    public FileMetadata updateFileMetadata(FileMetadata fileMetadata) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            fileMetadata.setUpdateTime(LocalDateTime.now());
            FileMetadata updated = fileMetadataRepository.save(fileMetadata);

            // 更新缓存
            cacheService.cacheFileMetadata(updated);

            log.info("文件元数据更新成功: fileId={}, fileName={}, traceId={}",
                    updated.getFileId(), updated.getOriginalFileName(), traceId);

            return updated;

        } catch (Exception e) {
            log.error("更新文件元数据失败: fileId={}, error={}, traceId={}",
                    fileMetadata.getFileId(), e.getMessage(), traceId);
            throw new RuntimeException("更新文件元数据失败", e);
        }
    }

    /**
     * 软删除文件元数据到PostgreSQL
     */
    private void softDeleteFileMetadataInDatabase(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            int updated = fileMetadataRepository.softDeleteById(fileId, LocalDateTime.now());

            log.info("文件元数据软删除成功: fileId={}, updated={}, traceId={}",
                    fileId, updated, traceId);

        } catch (Exception e) {
            log.error("软删除文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId);
            throw new RuntimeException("软删除文件元数据失败", e);
        }
    }

    /**
     * 异步更新访问统计
     */
    private void updateAccessStatsAsync(FileMetadata metadata) {
        try {
            fileMetadataRepository.incrementAccessCount(metadata.getFileId(), LocalDateTime.now());
        } catch (Exception e) {
            log.warn("更新访问统计失败: fileId={}, error={}", metadata.getFileId(), e.getMessage());
        }
    }

    // ==================== 兼容性方法（保持原有接口） ====================

    /**
     * 根据文件ID获取文件元数据（兼容性方法）
     */
    public FileMetadata getFileMetadata(String fileId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        FileMetadata fileMetadata = null;
        try {
            // 先从缓存获取
            Optional<FileMetadata> metadataOptional = cacheService.getCachedFileMetadata(fileId);
            if (metadataOptional.isPresent()) {
                fileMetadata = metadataOptional.get();
            }
            if (fileMetadata != null) {
                return fileMetadata;
            }

            Optional<FileMetadata> metadataRepository = fileMetadataRepository.findById(fileId);
            if (metadataRepository.isPresent()) {
                fileMetadata = metadataRepository.get();
            }
            if (fileMetadata != null) {
                cacheService.cacheFileMetadata(fileMetadata);
                return fileMetadata;
            }
            // 缓存未命中，从数据库获取
            return null;

        } catch (Exception e) {
            log.error("获取文件元数据失败: fileId={}, error={}, traceId={}",
                    fileId, e.getMessage(), traceId);
            return null;
        }
    }


    // ==================== 存储统计功能 ====================

    /**
     * 获取家庭存储统计
     * <p>
     * 基于专门的统计服务获取准确的家庭存储统计信息：
     * - 统计信息实时更新，无需等待缓存失效
     * - 包含详细的文件分类统计
     * - 支持存储健康状态监控
     */
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        try {
            // 使用专门地统计服务获取准确的数据
            FamilyStorageStats stats = familyStorageStatsService.getFamilyStats(familyId);
            // 设置存储适配器相关信息
            stats.setStorageType(storageAdapter.getStorageType());
            stats.setStorageHealthy(storageAdapter.isHealthy());
            return stats;

        } catch (Exception e) {
            log.error("获取家庭存储统计失败: familyId={}, error={}", familyId, e.getMessage());
            return createDefaultStorageStats(familyId);
        }
    }

    /**
     * 创建默认存储统计
     */
    private FamilyStorageStats createDefaultStorageStats(String familyId) {
        FamilyStorageStats stats = new FamilyStorageStats();
        stats.setFamilyId(familyId);
        stats.setTotalFiles(0);
        stats.setTotalSize(0L);
        stats.setFilesByType(new HashMap<>());
        stats.setLastUpdated(LocalDateTime.now());
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

}