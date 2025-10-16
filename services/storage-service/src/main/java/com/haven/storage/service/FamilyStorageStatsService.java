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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            // 获取或创建统计记录
            FamilyStorageStats stats = statsRepository.findByFamilyId(familyId)
                    .orElse(createNewStats(familyId));
            // 更新基础统计
            stats.setTotalFiles(stats.getTotalFiles() + 1);
            stats.setTotalSize(stats.getTotalSize() + fileMetadata.getFileSize());
            stats.setLastUpdated(LocalDateTime.now());
            // 更新文件类型统计
            updateFileTypeStats(stats, fileMetadata.getFileType(), 1);
            // 更新详细分类统计
            updateDetailedStats(stats, fileMetadata, true);
            // 更新最大文件信息
            updateLargestFileStats(stats, fileMetadata);

            // 更新最近文件时间
            stats.setMostRecentFileTime(LocalDateTime.now());

            // 保存统计信息
            statsRepository.save(stats);

            log.debug("文件上传统计更新完成: familyId={}, totalFiles={}, totalSize={}",
                    familyId, stats.getTotalFiles(), stats.getTotalSize());

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

            // 获取统计记录
            FamilyStorageStats stats = statsRepository.findByFamilyId(familyId)
                    .orElse(createNewStats(familyId));

            // 更新基础统计
            stats.setTotalFiles(Math.max(0, stats.getTotalFiles() - 1));
            stats.setTotalSize(Math.max(0, stats.getTotalSize() - fileMetadata.getFileSize()));
            stats.setLastUpdated(LocalDateTime.now());

            // 更新文件类型统计
            updateFileTypeStats(stats, fileMetadata.getFileType(), -1);

            // 更新详细分类统计
            updateDetailedStats(stats, fileMetadata, false);

            // 保存统计信息
            statsRepository.save(stats);

            log.debug("文件删除统计更新完成: familyId={}, totalFiles={}, totalSize={}",
                    familyId, stats.getTotalFiles(), stats.getTotalSize());

        } catch (Exception e) {
            log.error("更新文件删除统计失败: fileId={}, familyId={}, error={}",
                    fileMetadata.getFileId(), fileMetadata.getFamilyId(), e.getMessage());
        }
    }

    /**
     * 重新计算家庭存储统计
     */
    @Transactional
    public FamilyStorageStats recalculateStats(String familyId) {
        try {
            // 从数据库重新计算统计信息
            long totalFiles = fileMetadataRepository.countActiveFilesByFamily(familyId);
            long totalSize = fileMetadataRepository.sumFileSizeByFamily(familyId);

            List<Object[]> typeStats = fileMetadataRepository.countFilesByTypeByFamily(familyId);
            Map<String, Integer> filesByType = new HashMap<>();
            for (Object[] stat : typeStats) {
                String fileType = (String) stat[0];
                Long count = (Long) stat[1];
                filesByType.put(fileType, count.intValue());
            }

            // 获取所有文件用于详细统计
            List<FileMetadata> allFiles = fileMetadataRepository.findActiveFilesByFamily(familyId);

            // 构建统计结果
            FamilyStorageStats stats = statsRepository.findByFamilyId(familyId)
                    .orElse(createNewStats(familyId));

            stats.setTotalFiles((int) totalFiles);
            stats.setTotalSize(totalSize);
            stats.setFilesByType(filesByType);
            stats.setLastUpdated(LocalDateTime.now());

            // 计算详细统计
            calculateDetailedStats(stats, allFiles);

            // 保存统计信息
            return statsRepository.save(stats);

        } catch (Exception e) {
            log.error("重新计算家庭存储统计失败: familyId={}, error={}", familyId, e.getMessage());
            return createNewStats(familyId);
        }
    }

    /**
     * 获取家庭存储统计
     */
    @Transactional(readOnly = true)
    public FamilyStorageStats getFamilyStats(String familyId) {
        return statsRepository.findByFamilyId(familyId)
                .orElseGet(() -> recalculateStats(familyId));
    }

    /**
     * 创建新的统计记录
     */
    private FamilyStorageStats createNewStats(String familyId) {
        FamilyStorageStats stats = new FamilyStorageStats();
        stats.setFamilyId(familyId);
        stats.setTotalFiles(0);
        stats.setTotalSize(0L);
        stats.setFilesByType(new HashMap<>());
        stats.setLastUpdated(LocalDateTime.now());
        stats.setStorageHealthy(true);
        return stats;
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
        String fileType = fileMetadata.getFileType();
        int delta = isIncrement ? 1 : -1;

        // 更新图片统计
        if (fileTypeDetector.getCategoryByMimeType(fileType).equals("image")) {
            stats.setTotalImages((stats.getTotalImages() == null ? 0 : stats.getTotalImages()) + delta);
        }

        // 更新文档统计
        if (fileTypeDetector.getCategoryByMimeType(fileType).equals("document")) {
            stats.setTotalDocuments((stats.getTotalDocuments() == null ? 0 : stats.getTotalDocuments()) + delta);
        }

        // 更新视频统计
        if (fileTypeDetector.getCategoryByMimeType(fileType).equals("video")) {
            stats.setTotalVideos((stats.getTotalVideos() == null ? 0 : stats.getTotalVideos()) + delta);
        }

        // 更新其他统计
        String category = fileTypeDetector.getCategoryByMimeType(fileType);
        if (!category.equals("image") && !category.equals("document") && !category.equals("video")) {
            stats.setTotalOthers((stats.getTotalOthers() == null ? 0 : stats.getTotalOthers()) + delta);
        }
    }

    /**
     * 计算详细统计
     */
    private void calculateDetailedStats(FamilyStorageStats stats, List<FileMetadata> allFiles) {
        stats.setTotalImages(0);
        stats.setTotalDocuments(0);
        stats.setTotalVideos(0);
        stats.setTotalOthers(0);
        stats.setLargestFileSize(0L);
        stats.setLargestFileName(null);

        for (FileMetadata file : allFiles) {
            String fileType = file.getFileType();

            // 使用FileTypeDetector进行分类统计
            String category = fileTypeDetector.getCategoryByMimeType(fileType);

            if (category.equals("image")) {
                stats.setTotalImages(stats.getTotalImages() + 1);
            } else if (category.equals("document")) {
                stats.setTotalDocuments(stats.getTotalDocuments() + 1);
            } else if (category.equals("video")) {
                stats.setTotalVideos(stats.getTotalVideos() + 1);
            } else {
                stats.setTotalOthers(stats.getTotalOthers() + 1);
            }

            // 最大文件统计
            if (file.getFileSize() > (stats.getLargestFileSize() == null ? 0 : stats.getLargestFileSize())) {
                stats.setLargestFileSize(file.getFileSize());
                stats.setLargestFileName(file.getOriginalFileName());
            }
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
}