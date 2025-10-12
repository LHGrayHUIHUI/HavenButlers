package com.haven.storage.service;

import com.haven.base.annotation.TraceLog;
import com.haven.base.utils.TraceIdUtil;
import com.haven.storage.adapter.storage.LocalStorageAdapter;
import com.haven.storage.adapter.storage.MinIOStorageAdapter;
import com.haven.storage.adapter.storage.StorageAdapter;
import com.haven.storage.domain.model.file.*;
import jakarta.annotation.PostConstruct;
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
@Service
public class FamilyFileStorageService {

    @Value("${storage.file.storage-type:local}")
    private String storageType;

    // 存储适配器实例
    private final LocalStorageAdapter localStorageAdapter;

    private final MinIOStorageAdapter minioStorageAdapter;


    // 当前使用的存储适配器
    private StorageAdapter currentStorageAdapter;

    // 文件元数据缓存 - 所有存储方式共用
    private final Map<String, List<FileMetadata>> familyFilesCache = new ConcurrentHashMap<>();

    // 存储统计缓存
    private final Map<String, FamilyStorageStats> storageStatsCache = new ConcurrentHashMap<>();

    public FamilyFileStorageService(LocalStorageAdapter localStorageAdapter, MinIOStorageAdapter minioStorageAdapter) {
        this.localStorageAdapter = localStorageAdapter;
        this.minioStorageAdapter = minioStorageAdapter;
    }

    /**
     * 初始化存储适配器
     */
    @PostConstruct
    public void initializeStorageAdapter() {
        switch (storageType.toLowerCase()) {
            case "local":
                if (localStorageAdapter == null) {
                    throw new IllegalStateException("LocalStorageAdapter未配置，无法使用本地存储");
                }
                currentStorageAdapter = localStorageAdapter;
                break;
            case "minio":
                if (minioStorageAdapter == null) {
                    throw new IllegalStateException("MinIOStorageAdapter未配置，无法使用MinIO存储");
                }
                currentStorageAdapter = minioStorageAdapter;
                break;
            default:
                log.warn("未知的存储类型：{}，使用本地存储作为默认选项", storageType);
                if (localStorageAdapter == null) {
                    throw new IllegalStateException("LocalStorageAdapter未配置，无法使用默认本地存储");
                }
                currentStorageAdapter = localStorageAdapter;
                break;
        }

        log.info("存储适配器初始化完成：{} ({})",
                currentStorageAdapter.getStorageType(),
                currentStorageAdapter.getClass().getSimpleName());

        // 执行健康检查
        if (!currentStorageAdapter.isHealthy()) {
            log.error("存储适配器健康检查失败：{}", currentStorageAdapter.getStorageType());
        }
    }

    /**
     * 上传文件到家庭存储
     */
    @TraceLog(value = "上传家庭文件", module = "file-storage", type = "UPLOAD")
    public FileUploadResult uploadFile(String familyId, String folderPath, MultipartFile file,
                                       String uploaderUserId) {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 使用存储适配器上传文件
            FileUploadResult result = currentStorageAdapter.uploadFile(familyId, folderPath, file, uploaderUserId);

            if (result.isSuccess()) {
                // 缓存文件元数据
                cacheFileMetadata(familyId, result.getFileMetadata());

                // 更新存储统计
                updateStorageStats(familyId, file.getSize(), 1);

                log.info("文件上传成功: family={}, file={}, size={}bytes, storageType={}, TraceID={}",
                        familyId, file.getOriginalFilename(), file.getSize(),
                        currentStorageAdapter.getStorageType(), traceId);
            }

            return result;

        } catch (Exception e) {
            log.error("文件上传失败: family={}, file={}, storageType={}, error={}, TraceID={}",
                    familyId, file.getOriginalFilename(), currentStorageAdapter.getStorageType(), e.getMessage(), traceId);
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
            FileDownloadResult result = currentStorageAdapter.downloadFile(fileId, familyId);

            if (result.isSuccess()) {
                // 更新缓存中的访问记录
                FileMetadata metadata = getFileMetadata(familyId, fileId);
                if (metadata != null) {
                    metadata.setLastAccessTime(LocalDateTime.now());
                    metadata.setAccessCount(metadata.getAccessCount() + 1);
                }

                log.info("文件下载成功: family={}, fileId={}, size={}bytes, storageType={}, TraceID={}",
                        familyId, fileId, result.getFileContent().length,
                        currentStorageAdapter.getStorageType(), traceId);
            }

            return result;

        } catch (Exception e) {
            log.error("文件下载失败: family={}, fileId={}, storageType={}, error={}, TraceID={}",
                    familyId, fileId, currentStorageAdapter.getStorageType(), e.getMessage(), traceId);
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
            boolean deleted = currentStorageAdapter.deleteFile(fileId, familyId);

            if (deleted) {
                // 从缓存中移除文件信息
                removeFileFromCache(familyId, fileId);

                // 更新存储统计
                if (metadata != null) {
                    updateStorageStats(familyId, -metadata.getFileSize(), -1);
                }

                log.info("文件删除成功: family={}, fileId={}, storageType={}, TraceID={}",
                        familyId, fileId, currentStorageAdapter.getStorageType(), traceId);

                return FileDeleteResult.success(metadata != null ? metadata.getOriginalName() : "文件", traceId);
            } else {
                return FileDeleteResult.failure("文件删除失败");
            }

        } catch (Exception e) {
            log.error("文件删除失败: family={}, fileId={}, storageType={}, error={}, TraceID={}",
                    familyId, fileId, currentStorageAdapter.getStorageType(), e.getMessage(), traceId);
            return FileDeleteResult.failure("文件删除失败：" + e.getMessage());
        }
    }

    /**
     * 获取家庭存储统计
     */
    @TraceLog(value = "获取存储统计", module = "file-storage", type = "STATS")
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        FamilyStorageStats stats = storageStatsCache.computeIfAbsent(familyId, this::calculateStorageStats);
        stats.setStorageType(currentStorageAdapter.getStorageType());
        stats.setStorageHealthy(currentStorageAdapter.isHealthy());
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
     * 动态切换存储适配器
     */
    public boolean switchStorageAdapter(String newStorageType) {
        try {
            StorageAdapter newAdapter = null;

            switch (newStorageType.toLowerCase()) {
                case "local":
                    newAdapter = localStorageAdapter;
                    break;
                case "minio":
                    newAdapter = minioStorageAdapter;
                    break;
                default:
                    log.error("不支持的存储类型：{}", newStorageType);
                    return false;
            }

            // 健康检查
            if (!newAdapter.isHealthy()) {
                log.error("新存储适配器健康检查失败：{}", newStorageType);
                return false;
            }

            // 切换适配器
            String oldType = currentStorageAdapter.getStorageType();
            currentStorageAdapter = newAdapter;
            storageType = newStorageType;

            log.info("存储适配器切换成功：{} -> {}", oldType, newStorageType);
            return true;

        } catch (Exception e) {
            log.error("存储适配器切换失败：{}, error={}", newStorageType, e.getMessage());
            return false;
        }
    }

    /**
     * 获取文件访问URL
     */
    public String getFileAccessUrl(String fileId, String familyId, int expireMinutes) {
        return currentStorageAdapter.getFileAccessUrl(fileId, familyId, expireMinutes);
    }

    /**
     * 获取当前存储类型
     */
    public String getCurrentStorageType() {
        return currentStorageAdapter.getStorageType();
    }

    /**
     * 检查存储健康状态
     */
    public boolean isStorageHealthy() {
        return currentStorageAdapter.isHealthy();
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
        stats.setStorageType(currentStorageAdapter.getStorageType());
        stats.setStorageHealthy(currentStorageAdapter.isHealthy());

        return stats;
    }
}