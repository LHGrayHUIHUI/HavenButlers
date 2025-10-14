package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.adapter.storage.StorageAdapter;
import com.haven.storage.domain.model.file.*;
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
 * 家庭文件存储服务
 * <p>
 * 🎯 核心功能：
 * - 多种存储方式支持（本地、MinIO、云存储）
 * - 家庭文件的上传、下载、删除
 * - 文件夹组织和管理
 * - 基于familyId的数据隔离
 * - 文件元数据管理
 * - 存储适配器模式动态切换
 * <p>
 * 💡 使用场景：
 * - 家庭照片、视频存储
 * - 重要文档管理
 * - 孩子成长记录
 * - 设备说明书存档
 * - 多云存储备份
 *
 * @author HavenButler
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class FamilyFileStorageService {

    @Value("${storage.file.storage-type:local}")
    private String storageType;

    // 存储适配器实例（使用策略模式，Spring会自动选择合适的实现）
    private final StorageAdapter storageAdapter;

    // 文件元数据缓存 - 所有存储方式共用
    private final Map<String, List<FileMetadata>> familyFilesCache = new ConcurrentHashMap<>();

    // 存储统计缓存
    private final Map<String, FamilyStorageStats> storageStatsCache = new ConcurrentHashMap<>();


    /**
     * 上传文件到家庭存储
     */
    @TraceLog(value = "上传家庭文件(FileMetadata)", module = "file-storage", type = "UPLOAD_METADATA")
    public FileUploadResult uploadFile(FileMetadata fileMetadata, MultipartFile file) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 从FileMetadata中提取参数，避免重复构造
            String familyId = fileMetadata.getFamilyId();
            String folderPath = fileMetadata.getFolderPath();
            String uploaderUserId = fileMetadata.getUploaderUserId();
            String fileId = fileMetadata.getFileId();

            // 使用存储适配器上传文件（传入fileId）
            FileUploadResult result = storageAdapter.uploadFile(fileMetadata, file);

            if (result.isSuccess()) {
                // 缓存文件元数据
                cacheFileMetadata(familyId, result.getFileMetadata());

                // 更新存储统计
                updateStorageStats(familyId, file.getSize(), 1);

                log.info("文件上传成功(FileMetadata): family={}, file={}, fileId={}, size={}bytes, storageType={}, TraceID={}",
                        familyId, file.getOriginalFilename(), fileId, file.getSize(),
                        storageAdapter.getStorageType(), traceId);
            }

            return result;

        } catch (Exception e) {
            log.error("文件上传失败(FileMetadata): family={}, file={}, fileId={}, storageType={}, error={}, TraceID={}",
                    fileMetadata.getFamilyId(), fileMetadata.getOriginalFileName(), fileMetadata.getFileId(),
                    storageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileUploadResult.failure("文件上传失败：" + e.getMessage());
        }
    }

    /**
     * 下载家庭文件
     */
    @TraceLog(value = "下载家庭文件", module = "file-storage", type = "DOWNLOAD")
    public FileDownloadResult downloadFile(String fileId, String familyId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 使用存储适配器下载文件
            FileDownloadResult result = storageAdapter.downloadFile(fileId, familyId);

            if (result.isSuccess()) {
                // 更新缓存中的访问记录
                FileMetadata metadata = getFileMetadata(familyId, fileId);
                if (metadata != null) {
                    metadata.setLastAccessTime(LocalDateTime.now());
                    metadata.setAccessCount(metadata.getAccessCount() + 1);
                }

                log.info("文件下载成功: family={}, fileId={}, size={}bytes, storageType={}, TraceID={}",
                        familyId, fileId, result.getFileContent().length,
                        storageAdapter.getStorageType(), traceId);
            }

            return result;

        } catch (Exception e) {
            log.error("文件下载失败: family={}, fileId={}, storageType={}, error={}, TraceID={}",
                    familyId, fileId, storageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileDownloadResult.failure("文件下载失败：" + e.getMessage());
        }
    }

    /**
     * 获取家庭文件列表
     */
    @TraceLog(value = "获取文件列表", module = "file-storage", type = "LIST")
    public FamilyFileList getFamilyFiles(String familyId, String folderPath) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<FileMetadata> allFiles = getFamilyFileMetadata(familyId);

            // 按文件夹过滤
            List<FileMetadata> folderFiles = allFiles.stream()
                    .filter(file -> file.getFolderPath().equals(folderPath))
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

            log.info("文件列表获取成功: family={}, folder={}, files={}, TraceID={}",
                    familyId, folderPath, folderFiles.size(), traceId);

            return fileList;

        } catch (Exception e) {
            log.error("获取文件列表失败: family={}, folder={}, error={}, TraceID={}",
                    familyId, folderPath, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 删除家庭文件
     */
    @TraceLog(value = "删除家庭文件", module = "file-storage", type = "DELETE")
    public FileDeleteResult deleteFile(String fileId, String familyId, String userId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 获取文件元数据（用于统计更新）
            FileMetadata metadata = getFileMetadata(familyId, fileId);

            // 使用存储适配器删除文件
            boolean deleted = storageAdapter.deleteFile(fileId, familyId);

            if (deleted) {
                // 从缓存中移除文件信息
                removeFileFromCache(familyId, fileId);

                // 更新存储统计
                if (metadata != null) {
                    updateStorageStats(familyId, -metadata.getFileSize(), -1);
                }

                log.info("文件删除成功: family={}, fileId={}, storageType={}, TraceID={}",
                        familyId, fileId, storageAdapter.getStorageType(), traceId);

                return FileDeleteResult.success(metadata != null ? metadata.getOriginalName() : "文件", traceId);
            } else {
                return FileDeleteResult.failure("文件删除失败");
            }

        } catch (Exception e) {
            log.error("文件删除失败: family={}, fileId={}, storageType={}, error={}, TraceID={}",
                    familyId, fileId, storageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileDeleteResult.failure("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取家庭存储统计
     */
    @TraceLog(value = "获取存储统计", module = "file-storage", type = "STATS")
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        FamilyStorageStats stats = storageStatsCache.computeIfAbsent(familyId, this::calculateStorageStats);
        stats.setStorageType(storageAdapter.getStorageType());
        stats.setStorageHealthy(storageAdapter.isHealthy());
        return stats;
    }

    /**
     * 搜索家庭文件
     */
    @TraceLog(value = "搜索家庭文件", module = "file-storage", type = "SEARCH")
    public FileSearchResult searchFiles(String familyId, String keyword) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            List<FileMetadata> allFiles = getFamilyFileMetadata(familyId);

            // 按文件名搜索
            List<FileMetadata> matchedFiles = allFiles.stream()
                    .filter(file -> file.getFileName().toLowerCase().contains(keyword.toLowerCase()) ||
                            (file.getTags() != null && file.getTags().stream()
                                    .anyMatch(tag -> tag.toLowerCase().contains(keyword.toLowerCase()))))
                    .sorted(Comparator.comparing(FileMetadata::getUploadTime).reversed())
                    .collect(Collectors.toList());

            FileSearchResult searchResult = new FileSearchResult();
            searchResult.setFamilyId(familyId);
            searchResult.setKeyword(keyword);
            searchResult.setMatchedFiles(matchedFiles);
            searchResult.setTotalMatches(matchedFiles.size());
            searchResult.setTraceId(traceId);

            log.info("文件搜索完成: family={}, keyword={}, matches={}, TraceID={}",
                    familyId, keyword, matchedFiles.size(), traceId);

            return searchResult;

        } catch (Exception e) {
            log.error("文件搜索失败: family={}, keyword={}, error={}, TraceID={}",
                    familyId, keyword, e.getMessage(), traceId);
            throw e;
        }
    }

    /**
     * 动态切换存储适配器（已弃用，因为使用策略模式由Spring自动管理）
     */
    public boolean switchStorageAdapter(String newStorageType) {
        log.warn("存储适配器动态切换功能已弃用。当前使用的是：{}",
                storageAdapter.getStorageType());
        return false;
    }

    /**
     * 获取文件访问URL
     */
    public String getFileAccessUrl(String fileId, String familyId, int expireMinutes) {
        return storageAdapter.getFileAccessUrl(fileId, familyId, expireMinutes);
    }

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

    // 私有方法实现

    private void cacheFileMetadata(String familyId, FileMetadata metadata) {
        familyFilesCache.computeIfAbsent(familyId, k -> new ArrayList<>()).add(metadata);
    }

    private List<FileMetadata> getFamilyFileMetadata(String familyId) {
        return familyFilesCache.getOrDefault(familyId, new ArrayList<>());
    }

    private FileMetadata getFileMetadata(String familyId, String fileId) {
        return getFamilyFileMetadata(familyId).stream()
                .filter(file -> file.getFileId().equals(fileId))
                .findFirst()
                .orElse(null);
    }

    private void removeFileFromCache(String familyId, String fileId) {
        List<FileMetadata> files = familyFilesCache.get(familyId);
        if (files != null) {
            files.removeIf(file -> file.getFileId().equals(fileId));
        }
    }

    private void updateStorageStats(String familyId, long sizeChange, int fileCountChange) {
        FamilyStorageStats stats = storageStatsCache.computeIfAbsent(familyId, this::calculateStorageStats);
        stats.setTotalSize(stats.getTotalSize() + sizeChange);
        stats.setTotalFiles(stats.getTotalFiles() + fileCountChange);
        stats.setLastUpdated(LocalDateTime.now());
    }

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
}