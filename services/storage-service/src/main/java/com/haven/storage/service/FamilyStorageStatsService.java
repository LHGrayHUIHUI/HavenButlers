package com.haven.storage.service;

import com.haven.storage.domain.model.entity.FamilyStorageStats;
import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.repository.FamilyStorageStatsRepository;
import com.haven.storage.security.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 家庭存储统计服务
 * <p>
 * 负责维护和更新家庭存储统计信息：
 * - 文件上传/删除时实时更新统计
 * - 定期重新计算统计数据
 * - 提供存储使用情况分析
 *
 * @author HavenButler
 */
@Slf4j
@Service
@RequiredArgsConstructor
@EnableCaching
public class FamilyStorageStatsService {

    private final FamilyStorageStatsRepository statsRepository;
    private final FileTypeDetector fileTypeDetector;

    // ==================== 缓存配置常量 ====================
    private static final String CACHE_NAME = "familyStorageStats";

    // ==================== 文件操作常量 ====================

    private static final String UPLOAD_OPERATION = "上传";
    private static final String DELETE_OPERATION = "删除";

    /**
     * 文件上传时更新统计信息
     */
    @Transactional
    public void onFileUploaded(FileMetadata fileMetadata) {
        validateFileMetadata(fileMetadata, UPLOAD_OPERATION);
        updateFileStatistics(fileMetadata, true, UPLOAD_OPERATION);
    }

    /**
     * 文件删除时更新统计信息
     */
    @Transactional
    public void onFileDeleted(FileMetadata fileMetadata) {
        validateFileMetadata(fileMetadata, DELETE_OPERATION);
        updateFileStatistics(fileMetadata, false, DELETE_OPERATION);
    }

    /**
     * 更新文件类型统计
     */
    private void updateFileTypeStats(FamilyStorageStats stats, String fileType, int delta) {
        if (stats.getFilesByType() == null) {
            stats.setFilesByType(new HashMap<>());
        }

        Map<String, Integer> filesByType = stats.getFilesByType();
        filesByType.put(fileType, filesByType.getOrDefault(fileType, 0) + delta);
    }

    /**
     * 更新详细分类统计 - 直接调用FamilyStorageStats方法
     */
    private void updateDetailedStats(FamilyStorageStats stats, FileMetadata fileMetadata, boolean isIncrement) {
        // 使用完整的MIME类型而不是简化的文件类型
        String mimeType = fileMetadata.getContentType() != null ?
                         fileMetadata.getContentType() : fileMetadata.getFileType();
        String category = fileTypeDetector.getCategoryByMimeType(mimeType);
        int delta = isIncrement ? 1 : -1;

        // 直接调用FamilyStorageStats的incrementCategoryCount方法
        stats.incrementCategoryCount(category, delta);

        // 更新峰值统计（仅在增加时）
        if (isIncrement) {
            stats.updatePeakStats();
        }
    }


    /**
     * 更新最大文件统计
     */
    private void updateLargestFileStats(FamilyStorageStats stats, FileMetadata fileMetadata) {
        if (stats.getLargestFileSize() == null || fileMetadata.getFileSize() > stats.getLargestFileSize()) {
            stats.setLargestFileSize(fileMetadata.getFileSize());
            stats.setLargestFileName(fileMetadata.getOriginalFileName());
            log.debug("更新最大文件信息: fileId={}, fileName={}, size={}",
                    fileMetadata.getFileId(), fileMetadata.getOriginalFileName(), fileMetadata.getFileSize());
        }
    }

    /**
     * 检查并更新删除后的最大文件信息
     * 如果删除的是当前最大文件，需要重新查找最大的文件
     *
     * @param stats 家庭统计信息
     * @param deletedFileId 被删除的文件ID（预留参数，后续扩展使用）
     */
    private void checkAndUpdateLargestFileAfterDeletion(FamilyStorageStats stats, String deletedFileId) {
        // 如果当前没有最大文件信息，无需处理
        if (stats.getLargestFileSize() == null || stats.getLargestFileName() == null) {
            return;
        }

        // TODO: 后续可以根据deletedFileId查询文件元数据表，确认被删除的文件是否是当前最大文件
        // 然后如果是的话，重新查找家庭中最大的文件
        if (stats.getTotalFiles() == 0) {
            log.debug("家庭文件数量为0，清空最大文件信息: familyId={}", stats.getFamilyId());
            stats.setLargestFileSize(null);
            stats.setLargestFileName(null);
        }
    }


    /**
     * 获取或创建家庭存储统计（内部方法，用于更新操作）
     * <p>
     * 用于内部更新操作时获取统计信息：
     * - 如果统计信息不存在，创建空记录（不进行昂贵的重新计算）
     * - 适用于 onFileUploaded、onFileDeleted 等场景
     *
     * @param familyId 家庭ID
     * @return 家庭存储统计信息
     */
    protected FamilyStorageStats getFamilyStats(String familyId) {
        if (familyId == null || familyId.trim().isEmpty()) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }

        return statsRepository.findByFamilyId(familyId)
                .orElseGet(() -> {
                    log.info("家庭统计信息不存在，创建新的统计记录: familyId={} - User: {}, TraceID: {}",
                            familyId, UserContext.getCurrentUserName(), UserContext.getCurrentTraceId());
                    FamilyStorageStats newStats = createNewStats(familyId);
                    log.debug("创建新的家庭统计记录完成: {} - User: {}, TraceID: {}",
                            newStats.getSummary(), UserContext.getCurrentUserName(), UserContext.getCurrentTraceId());
                    return newStats;
                });
    }

    /**
     * 获取家庭存储统计（公开方法，带缓存）
     *
     * @param familyId 家庭ID
     * @return 家庭存储统计信息
     */
    @Cacheable(value = CACHE_NAME, key = "#familyId", unless = "#result == null")
    public FamilyStorageStats getFamilyStorageStats(String familyId) {
        if (familyId == null || familyId.trim().isEmpty()) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }

        return statsRepository.findByFamilyId(familyId)
                .orElseGet(() -> {
                    log.info("家庭统计信息不存在，返回默认统计: familyId={} - User: {}, TraceID: {}",
                            familyId, UserContext.getCurrentUserName(), UserContext.getCurrentTraceId());
                    return createNewStats(familyId);
                });
    }

    /**
     * 清除家庭统计缓存
     *
     * @param familyId 家庭ID
     */
    @CacheEvict(value = CACHE_NAME, key = "#familyId")
    public void evictFamilyStatsCache(String familyId) {
        log.debug("清除家庭统计缓存: familyId={} - User: {}, TraceID: {}",
                familyId, UserContext.getCurrentUserName(), UserContext.getCurrentTraceId());
    }

    // 移除冗余的updateFamilyStatsCache方法
// 说明：缓存更新已通过@CacheEvict在业务操作前清除，避免数据不一致

    // 移除暂时用不到的缓存监控和批量操作方法
// 说明：以下方法在当前业务场景中暂时用不到，后续需要时可重新添加：
// - getQueryCount() / incrementQueryCount() - 查询统计监控
// - warmupFamilyStatsCache() / warmupFamilyStatsCacheBatch() - 缓存预热
// - evictAllFamilyStatsCache() - 批量缓存清除

    // ==================== 私有辅助方法 ====================

    /**
     * 验证文件元数据
     *
     * @param fileMetadata 文件元数据
     * @param operation 操作类型（上传/删除）
     */
    private void validateFileMetadata(FileMetadata fileMetadata, String operation) {
        String familyId = fileMetadata.getFamilyId();
        String fileId = fileMetadata.getFileId();
        String fileName = fileMetadata.getOriginalFileName();
        long fileSize = fileMetadata.getFileSize();

        log.debug("开始更新文件{}统计: fileId={}, familyId={}, fileName={}, size={}" +
                " - User: {}, TraceID: {}",
                operation, fileId, familyId, fileName, fileSize,
                UserContext.getCurrentUserName(),
                UserContext.getCurrentTraceId());

        // 参数验证
        if (fileId == null || familyId == null) {
            throw new IllegalArgumentException("文件ID和家庭ID不能为空");
        }

        if (fileSize < 0) {
            throw new IllegalArgumentException("文件大小不能为负数: " + fileSize);
        }
    }

    /**
     * 统一更新文件统计信息
     *
     * @param fileMetadata 文件元数据
     * @param isIncrement 是否为增加操作（true=上传，false=删除）
     * @param operation 操作类型描述
     */
    private void updateFileStatistics(FileMetadata fileMetadata, boolean isIncrement, String operation) {
        String familyId = fileMetadata.getFamilyId();
        String fileId = fileMetadata.getFileId();
        String fileName = fileMetadata.getOriginalFileName();
        long fileSize = fileMetadata.getFileSize();

        try {
            // 获取或创建统计记录
            FamilyStorageStats familyStorageStats = getFamilyStats(familyId);

            // 记录更新前的状态
            int beforeFiles = familyStorageStats.getTotalFiles();
            long beforeSize = familyStorageStats.getTotalSize();

            // 检查数据合理性（删除时）
            if (!isIncrement && beforeFiles <= 0) {
                log.warn("删除文件前统计显示文件数量为0，可能存在数据不一致: familyId={}, currentFiles={}" +
                        " - User: {}, TraceID: {}",
                        familyId, beforeFiles,
                        UserContext.getCurrentUserName(),
                        UserContext.getCurrentTraceId());
            }

            // 更新基础统计
            updateBasicStats(familyStorageStats, fileSize, isIncrement);

            // 更新文件类型统计
            updateFileTypeStats(familyStorageStats, fileMetadata.getFileType(), isIncrement ? 1 : -1);

            // 更新详细分类统计
            updateDetailedStats(familyStorageStats, fileMetadata, isIncrement);

            // 更新最大文件信息（仅上传时）
            if (isIncrement) {
                updateLargestFileStats(familyStorageStats, fileMetadata);
                familyStorageStats.setMostRecentFileTime(LocalDateTime.now());
            } else {
                // 检查是否需要更新最大文件信息（如果删除的是当前最大文件）
                checkAndUpdateLargestFileAfterDeletion(familyStorageStats, fileId);
            }

            // 清除缓存，避免脏数据
            evictFamilyStatsCache(familyId);

            // 保存统计信息
            familyStorageStats = statsRepository.save(familyStorageStats);

            logOperationSuccess(operation, fileId, familyId, fileName, beforeFiles, beforeSize, familyStorageStats);

        } catch (IllegalArgumentException e) {
            logOperationError(operation, fileId, familyId, fileName, "参数错误", e.getMessage());
            throw e;
        } catch (Exception e) {
            logOperationError(operation, fileId, familyId, fileName, "系统异常", e.getMessage());
            throw new RuntimeException("文件" + operation + "统计更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新基础统计信息
     */
    private void updateBasicStats(FamilyStorageStats stats, long fileSize, boolean isIncrement) {
        if (isIncrement) {
            stats.setTotalFiles(stats.getTotalFiles() + 1);
            stats.setTotalSize(stats.getTotalSize() + fileSize);
        } else {
            stats.setTotalFiles(Math.max(0, stats.getTotalFiles() - 1));
            stats.setTotalSize(Math.max(0, stats.getTotalSize() - fileSize));
        }
    }

    /**
     * 记录操作成功日志
     */
    private void logOperationSuccess(String operation, String fileId, String familyId, String fileName,
                                    int beforeFiles, long beforeSize, FamilyStorageStats updatedStats) {
        log.info("文件{}统计更新成功: fileId={}, familyId={}, fileName={}, " +
                "文件计数: {}->{}, 存储大小: {}->{}, 最新文件数: {}, 最新存储大小: {}MB" +
                " - User: {}, TraceID: {}",
                operation, fileId, familyId, fileName,
                beforeFiles, updatedStats.getTotalFiles(),
                beforeSize, updatedStats.getTotalSize(),
                updatedStats.getTotalFiles(),
                updatedStats.getTotalSize() / (1024 * 1024),
                UserContext.getCurrentUserName(),
                UserContext.getCurrentTraceId());
    }

    /**
     * 记录操作失败日志
     */
    private void logOperationError(String operation, String fileId, String familyId, String fileName,
                                  String errorType, String errorMessage) {
        log.error("文件{}统计更新失败 - {}: fileId={}, familyId={}, fileName={}, error={}" +
                " - User: {}, TraceID: {}",
                operation, errorType, fileId, familyId, fileName, errorMessage,
                UserContext.getCurrentUserName(),
                UserContext.getCurrentTraceId());
    }

    /**
     * 创建新地统计记录
     */
    private FamilyStorageStats createNewStats(String familyId) {
        FamilyStorageStats stats = new FamilyStorageStats();
        stats.setFamilyId(familyId);
        stats.setTotalFiles(0);
        stats.setTotalSize(0L);
        stats.setFilesByType(new HashMap<>());

        // 初始化所有分类统计字段为0，避免返回null值
        stats.setTotalImages(0);
        stats.setTotalDocuments(0);
        stats.setTotalVideos(0);
        stats.setTotalAudio(0);
        stats.setTotalOthers(0);

        // 初始化最大文件信息为null（因为没有文件）
        stats.setLargestFileSize(null);
        stats.setLargestFileName(null);
        stats.setMostRecentFileTime(null);

        stats.setStorageHealthy(true);
        return stats;
    }

}