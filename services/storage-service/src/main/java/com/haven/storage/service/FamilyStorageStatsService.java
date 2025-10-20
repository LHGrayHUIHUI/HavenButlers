package com.haven.storage.service;

import com.haven.storage.domain.model.file.FamilyStorageStats;
import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.repository.FamilyStorageStatsRepository;
import com.haven.storage.repository.FileMetadataRepository;
import com.haven.storage.utils.FileTypeDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class FamilyStorageStatsService {

    private final FamilyStorageStatsRepository statsRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final FileTypeDetector fileTypeDetector;

    /**
     * 文件上传时更新统计信息
     */
    @Transactional
    public void onFileUploaded(FileMetadata fileMetadata) {
        try {
            String familyId = fileMetadata.getFamilyId();
            // 获取或创建统计记录（更新操作时不需要自动重新计算）
            FamilyStorageStats familyStorageStats = getFamilyStats(familyId);
            // 更新基础统计
            familyStorageStats.setTotalFiles(familyStorageStats.getTotalFiles() + 1);
            familyStorageStats.setTotalSize(familyStorageStats.getTotalSize() + fileMetadata.getFileSize());
            familyStorageStats.setLastUpdated(LocalDateTime.now());
            // 更新文件类型统计
            updateFileTypeStats(familyStorageStats, fileMetadata.getFileType(), 1);
            // 更新详细分类统计
            updateDetailedStats(familyStorageStats, fileMetadata, true);
            // 更新最大文件信息
            updateLargestFileStats(familyStorageStats, fileMetadata);
            // 更新最近文件时间
            familyStorageStats.setMostRecentFileTime(LocalDateTime.now());
            // 保存统计信息
            statsRepository.save(familyStorageStats);
            log.debug("文件上传统计更新完成: familyStorageStats={}", familyStorageStats);

        } catch (Exception e) {
            log.error("更新文件上传统计失败: fileId={}, familyId={}, error={}",
                    fileMetadata.getFileId(), fileMetadata.getFamilyId(), e.getMessage());
        }
    }

    /**
     * 文件删除时更新统计信息
     */
    @Transactional
    public void onFileDeleted(FileMetadata fileMetadata) {
        try {
            String familyId = fileMetadata.getFamilyId();

            // 获取统计记录（更新操作时不需要自动重新计算）
            FamilyStorageStats familyStorageStats = getFamilyStats(familyId);
            // 更新基础统计
            familyStorageStats.setTotalFiles(Math.max(0, familyStorageStats.getTotalFiles() - 1));
            familyStorageStats.setTotalSize(Math.max(0, familyStorageStats.getTotalSize() - fileMetadata.getFileSize()));
            familyStorageStats.setLastUpdated(LocalDateTime.now());
            // 更新文件类型统计
            updateFileTypeStats(familyStorageStats, fileMetadata.getFileType(), -1);
            // 更新详细分类统计
            updateDetailedStats(familyStorageStats, fileMetadata, false);
            // 保存统计信息
            statsRepository.save(familyStorageStats);
            log.debug("文件删除统计更新完成: familyStorageStats={}", familyStorageStats);
        } catch (Exception e) {
            log.error("更新文件删除统计失败: fileId={}, familyId={}, error={}",
                    fileMetadata.getFileId(), fileMetadata.getFamilyId(), e.getMessage());
        }
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
     * 更新详细分类统计
     */
    private void updateDetailedStats(FamilyStorageStats stats, FileMetadata fileMetadata, boolean isIncrement) {
        // 使用完整的MIME类型而不是简化的文件类型
        String mimeType = fileMetadata.getContentType() != null ?
                         fileMetadata.getContentType() : fileMetadata.getFileType();
        String category = fileTypeDetector.getCategoryByMimeType(mimeType);
        int delta = isIncrement ? 1 : -1;

        switch (category) {
            case "image" -> stats.setTotalImages((stats.getTotalImages() == null ? 0 : stats.getTotalImages()) + delta);
            case "document" ->
                    stats.setTotalDocuments((stats.getTotalDocuments() == null ? 0 : stats.getTotalDocuments()) + delta);
            case "video" -> stats.setTotalVideos((stats.getTotalVideos() == null ? 0 : stats.getTotalVideos()) + delta);
            case "audio" -> stats.setTotalAudio((stats.getTotalAudio() == null ? 0 : stats.getTotalAudio()) + delta);
            default -> stats.setTotalOthers((stats.getTotalOthers() == null ? 0 : stats.getTotalOthers()) + delta);
        }
    }


    /**
     * 更新最大文件统计
     */
    private void updateLargestFileStats(FamilyStorageStats stats, FileMetadata fileMetadata) {
        if (stats.getLargestFileSize() == null || fileMetadata.getFileSize() > stats.getLargestFileSize()) {
            stats.setLargestFileSize(fileMetadata.getFileSize());
            stats.setLargestFileName(fileMetadata.getOriginalFileName());
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
        return statsRepository.findByFamilyId(familyId)
                .orElseGet(() -> {
                    log.debug("家庭统计信息不存在，创建空记录: familyId={}", familyId);
                    return createNewStats(familyId);
                });
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

        stats.setLastUpdated(LocalDateTime.now());
        stats.setStorageHealthy(true);
        return stats;
    }

}