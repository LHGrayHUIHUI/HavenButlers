package com.haven.storage.service;

import com.haven.storage.domain.model.entity.FamilyStorageStats;
import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.FileCategory;
import com.haven.storage.repository.FamilyStorageStatsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

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

    private final FamilyStorageStatsRepository familyStorageStatsRepository;

    /**
     * 统一的存储统计更新方法
     *
     * <p>根据文件操作类型自动更新家庭存储统计信息。支持文件上传、删除、修改操作的统计更新。
     * 对于修改操作，直接传入文件大小变化量。</p>
     *
     * <p>处理逻辑：</p>
     * <ul>
     *   <li>UPLOAD: 增加文件数量和存储大小</li>
     *   <li>DELETE: 减少文件数量和存储大小</li>
     *   <li>MODIFY: 根据文件大小变化调整统计（需要sizeDifference参数）</li>
     * </ul>
     *
     * @param operation      文件操作类型
     * @param fileMetadata   文件元数据（不能为空）
     * @param sizeDifference 文件大小变化量（仅MODIFY操作时需要，上传为正值，删除为负值，修改为实际变化值）
     * @throws IllegalArgumentException 当必要参数为空时
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateStorageStats(FileOperation operation,
                                   FileMetadata fileMetadata,
                                   long sizeDifference) {
        // 参数验证
        validateUpdateParameters(operation, fileMetadata);

        String familyId = fileMetadata.getFamilyId();
        log.debug("开始更新家庭存储统计 - familyId: {}, operation: {}, fileId: {}, sizeDiff: {}",
                familyId, operation, fileMetadata.getFileId(), sizeDifference);

        try {
            // 获取或创建家庭存储统计记录
            FamilyStorageStats stats = getOrCreateFamilyStorageStats(familyId);

            // 确定文件分类
            FileCategory fileCategory = determineFileCategory(fileMetadata);

            // 根据操作类型更新统计
            switch (operation) {
                case UPLOAD:
                    handleUploadOperation(stats, fileMetadata, fileCategory);
                    break;
                case DELETE:
                    handleDeleteOperation(stats, fileMetadata, fileCategory);
                    break;
                case MODIFY:
                    handleModifyOperation(stats, sizeDifference);
                    break;
                default:
                    log.warn("不支持的文件操作类型 - operation: {}", operation);
                    return;
            }

            // 保存统计记录
            familyStorageStatsRepository.save(stats);

            log.debug("家庭存储统计更新完成 - familyId: {}, operation: {}", familyId, operation);

        } catch (Exception e) {
            log.error("更新家庭存储统计失败 - familyId: {}, operation: {}, error: {}",
                    familyId, operation, e.getMessage(), e);
            throw new RuntimeException("更新家庭存储统计失败", e);
        }
    }

    /**
     * 文件上传操作的统计更新
     */
    private void handleUploadOperation(FamilyStorageStats stats,
                                       FileMetadata fileMetadata,
                                       FileCategory fileCategory) {
        long fileSize = extractFileSize(fileMetadata);

        // 更新总体统计
        stats.setTotalFiles(stats.getTotalFiles() + 1);
        stats.setTotalSize(stats.getTotalSize() + fileSize);

        // 更新分类统计
        updateFilesByTypeStats(stats, fileCategory, 1);

        log.debug("处理上传操作统计 - familyId: {}, fileSize: {}, category: {}",
                fileMetadata.getFamilyId(), fileSize, fileCategory);
    }

    /**
     * 文件删除操作的统计更新
     */
    private void handleDeleteOperation(FamilyStorageStats stats,
                                       FileMetadata fileMetadata,
                                       FileCategory fileCategory) {
        long fileSize = extractFileSize(fileMetadata);

        // 更新总体统计（确保不出现负数）
        stats.setTotalFiles(Math.max(0, stats.getTotalFiles() - 1));
        stats.setTotalSize(Math.max(0, stats.getTotalSize() - fileSize));

        // 更新分类统计
        updateFilesByTypeStats(stats, fileCategory, -1);

        log.debug("处理删除操作统计 - familyId: {}, fileSize: {}, category: {}",
                fileMetadata.getFamilyId(), fileSize, fileCategory);
    }

    /**
     * 文件修改操作的统计更新
     */
    private void handleModifyOperation(FamilyStorageStats stats,
                                       long sizeDifference) {
        if (sizeDifference == 0) {
            log.debug("文件大小未发生变化，跳过统计更新 - familyId: {}", stats.getFamilyId());
            return;
        }
        // 更新总存储统计（确保不出现负数）
        stats.setTotalSize(Math.max(0, stats.getTotalSize() + sizeDifference));
        log.debug("处理修改操作统计 - familyId: {}, sizeDifference: {}", stats.getFamilyId(), sizeDifference);
    }

    /**
     * 更新按类型统计文件数量
     */
    private void updateFilesByTypeStats(FamilyStorageStats stats,
                                        FileCategory fileCategory,
                                        int countChange) {
        if (stats.getFilesByType() == null) {
            stats.setFilesByType(new HashMap<>());
        }

        Map<String, Integer> filesByType = stats.getFilesByType();
        String categoryKey = fileCategory.getCategoryName();

        int currentCount = filesByType.getOrDefault(categoryKey, 0);
        int newCount = Math.max(0, currentCount + countChange);

        if (newCount > 0) {
            filesByType.put(categoryKey, newCount);
        } else {
            filesByType.remove(categoryKey);
        }
    }

    /**
     * 获取或创建家庭存储统计记录
     */
    private FamilyStorageStats getOrCreateFamilyStorageStats(String familyId) {
        return familyStorageStatsRepository.findByFamilyId(familyId)
                .orElseGet(() -> {
                    log.debug("创建新的家庭存储统计记录 - familyId: {}", familyId);
                    FamilyStorageStats newStats = new FamilyStorageStats();
                    newStats.setFamilyId(familyId);
                    newStats.setTotalFiles(0);
                    newStats.setTotalSize(0L);
                    newStats.setFilesByType(new HashMap<>());
                    return newStats;
                });
    }

    /**
     * 从文件元数据中提取文件大小
     */
    private long extractFileSize(FileMetadata fileMetadata) {
        if (fileMetadata == null) {
            return 0L;
        }
        return fileMetadata.getFileSize(); // fileSize是long类型，不会为null
    }

    /**
     * 根据文件元数据确定文件分类
     */
    private FileCategory determineFileCategory(FileMetadata fileMetadata) {
        // 1. 优先使用文件元数据中的类型信息
        if (StringUtils.hasText(fileMetadata.getFileType())) {
            try {
                return FileCategory.valueOf(fileMetadata.getFileType().toUpperCase());
            } catch (IllegalArgumentException e) {
                log.debug("未知的文件类型: {}, 使用默认分类", fileMetadata.getFileType());
            }
        }
        return FileCategory.UNKNOWN;
    }


    /**
     * 验证更新参数
     */
    private void validateUpdateParameters(FileOperation operation,
                                          FileMetadata fileMetadata) {
        if (operation == null) {
            throw new IllegalArgumentException("文件操作类型不能为空");
        }
        if (fileMetadata == null) {
            throw new IllegalArgumentException("文件元数据不能为空");
        }
        if (!StringUtils.hasText(fileMetadata.getFamilyId())) {
            throw new IllegalArgumentException("家庭ID不能为空");
        }
    }


}