package com.haven.storage.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 家庭存储统计信息 - 优化版本
 * <p>
 * 数据库表结构：
 * <pre>
 * CREATE TABLE family_storage_stats (
 *   family_id VARCHAR(50) PRIMARY KEY COMMENT '家庭唯一标识',
 *   version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
 *   total_files INT NOT NULL DEFAULT 0 COMMENT '总文件数量',
 *   total_size BIGINT NOT NULL DEFAULT 0 COMMENT '总存储大小(字节)',
 *   total_images INT NOT NULL DEFAULT 0 COMMENT '图片文件数量',
 *   total_documents INT NOT NULL DEFAULT 0 COMMENT '文档文件数量',
 *   total_videos INT NOT NULL DEFAULT 0 COMMENT '视频文件数量',
 *   total_audio INT NOT NULL DEFAULT 0 COMMENT '音频文件数量',
 *   total_others INT NOT NULL DEFAULT 0 COMMENT '其他文件数量',
 *   largest_file_size BIGINT COMMENT '最大文件大小(字节)',
 *   largest_file_name VARCHAR(255) COMMENT '最大文件名称',
 *   most_recent_file_time TIMESTAMP COMMENT '最近文件时间',
 *   total_uploads INT NOT NULL DEFAULT 0 COMMENT '总上传次数',
 *   total_deletes INT NOT NULL DEFAULT 0 COMMENT '总删除次数',
 *   peak_file_count INT NOT NULL DEFAULT 0 COMMENT '峰值文件数量',
 *   peak_storage_size BIGINT NOT NULL DEFAULT 0 COMMENT '峰值存储大小',
 *   storage_type VARCHAR(50) COMMENT '当前存储类型(local/minio/cloud)',
 *   storage_healthy BOOLEAN NOT NULL DEFAULT TRUE COMMENT '存储健康状态',
 *   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
 *   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
 * ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭存储统计信息表';
 * </pre>
 * <p>
 * 优化特性：
 * - 乐观锁支持，防止并发更新问题
 * - 数据库字段类型优化（int vs Integer）
 * - 审计字段和版本控制
 * - 支持缓存和性能优化
 * - 完整的统计指标和监控能力
 * <p>
 * 使用说明：
 * - 通过 {@link FamilyStorageStatsService} 进行业务操作
 * - 支持实时更新和批量重算
 * - 缓存键格式：family:stats:{familyId}
 *
 * @author HavenButler
 * @version 1.0
 * @since 2024-01-01
 */
@Slf4j
@Data
@Entity
@Table(name = "family_storage_stats", indexes = {
    @Index(name = "idx_family_id_stats", columnList = "family_id", unique = true),
    @Index(name = "idx_last_updated", columnList = "last_updated"),
    @Index(name = "idx_storage_type", columnList = "storage_type"),
    @Index(name = "idx_total_size", columnList = "total_size"),
    @Index(name = "idx_storage_healthy", columnList = "storage_healthy")
})
@EntityListeners(AuditingEntityListener.class)
public class FamilyStorageStats {

    @Id
    @Column(name = "family_id", length = 50, nullable = false)
    private String familyId;

    /**
     * 乐观锁版本号 - 防止并发更新冲突
     * 每次更新自动递增，冲突时抛出OptimisticLockException
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * 总文件数量 - 包含所有类型的文件总数
     * 范围：0 ~ 21亿（int最大值）
     */
    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    /**
     * 总存储大小 - 所有文件占用的磁盘空间（字节）
     * 范围：0 ~ 9223372036854775807字节（约8EB）
     */
    @Column(name = "total_size", nullable = false)
    private long totalSize;

    @ElementCollection
    @CollectionTable(name = "family_stats_by_type", joinColumns = @JoinColumn(name = "family_id"))
    @MapKeyColumn(name = "file_type", length = 100)
    @Column(name = "file_count")
    private Map<String, Integer> filesByType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // 存储适配器相关信息
    @Column(name = "storage_type", length = 50)
    private String storageType;     // 当前存储类型

    @Column(name = "storage_healthy", nullable = false)
    private boolean storageHealthy; // 存储健康状态

    // ==================== 详细分类统计信息 ====================

    /**
     * 图片文件数量 - 包括jpg、png、gif、bmp等图片格式
     * 统计来源：基于MIME类型判断(image/*)
     */
    @Column(name = "total_images", nullable = false)
    private int totalImages = 0;

    /**
     * 文档文件数量 - 包括pdf、doc、docx、txt、xls等文档格式
     * 统计来源：基于MIME类型判断(application/pdf, text/*等)
     */
    @Column(name = "total_documents", nullable = false)
    private int totalDocuments = 0;

    /**
     * 视频文件数量 - 包括mp4、avi、mkv、mov等视频格式
     * 统计来源：基于MIME类型判断(video/*)
     */
    @Column(name = "total_videos", nullable = false)
    private int totalVideos = 0;

    /**
     * 音频文件数量 - 包括mp3、wav、flac、aac等音频格式
     * 统计来源：基于MIME类型判断(audio/*)
     */
    @Column(name = "total_audio", nullable = false)
    private int totalAudio = 0;

    /**
     * 其他文件数量 - 不属于上述分类的其他文件
     * 包括：压缩包、可执行文件、系统文件等
     */
    @Column(name = "total_others", nullable = false)
    private int totalOthers = 0;

    // ==================== 文件特征统计信息 ====================

    /**
     * 最大文件大小 - 家庭中单个文件的最大尺寸（字节）
     * 用途：存储容量规划、大文件监控
     */
    @Column(name = "largest_file_size")
    private Long largestFileSize;

    /**
     * 最大文件名称 - 最大文件的原始文件名
     * 用途：文件定位、用户展示
     */
    @Column(name = "largest_file_name", length = 255)
    private String largestFileName;

    /**
     * 最近文件时间 - 家庭中最新文件的上传时间
     * 用途：活跃度分析、用户行为分析
     */
    @Column(name = "most_recent_file_time")
    private LocalDateTime mostRecentFileTime;

    // ==================== 操作统计与峰值信息 ====================

    /**
     * 总上传次数 - 累计文件上传操作次数
     * 用途：用户活跃度统计、操作频率分析
     */
    @Column(name = "total_uploads")
    private int totalUploads = 0;

    /**
     * 总删除次数 - 累计文件删除操作次数
     * 用途：存储空间管理、用户行为分析
     */
    @Column(name = "total_deletes")
    private int totalDeletes = 0;

    /**
     * 峰值文件数量 - 历史最高文件数量
     * 用途：容量规划、增长趋势分析
     */
    @Column(name = "peak_file_count")
    private int peakFileCount = 0;

    /**
     * 峰值存储大小 - 历史最高存储使用量（字节）
     * 用途：容量规划、存储预警
     */
    @Column(name = "peak_storage_size")
    private long peakStorageSize = 0L;

    // ==================== 实用方法 ====================

    /**
     * 更新峰值统计
     */
    public void updatePeakStats() {
        if (totalFiles > peakFileCount) {
            peakFileCount = totalFiles;
        }
        if (totalSize > peakStorageSize) {
            peakStorageSize = totalSize;
        }
    }

    /**
     * 获取存储使用率（基于预设的容量限制）
     */
    public double getStorageUsagePercentage() {
        long totalBytes = totalSize;
        long maxBytes = 1024L * 1024 * 1024 * 100; // 100GB

        if (totalBytes <= 0) {
            return 0.0;
        }

        return Math.min(100.0, (totalBytes * 100.0) / maxBytes);
    }

    /**
     * 获取平均文件大小
     */
    public double getAverageFileSize() {
        if (totalFiles <= 0) {
            return 0.0;
        }
        return (double) totalSize / totalFiles;
    }

    /**
     * 检查是否接近容量限制
     */
    public boolean isNearCapacityLimit() {
        return getStorageUsagePercentage() > 80.0;
    }

    /**
     * 获取统计摘要
     */
    public String getSummary() {
        return String.format(
            "FamilyStorageStats{familyId='%s', totalFiles=%d, totalSize=%dMB, " +
            "totalImages=%d, totalDocuments=%d, totalVideos=%d, totalAudio=%d, totalOthers=%d}",
            familyId, totalFiles, totalSize / (1024 * 1024),
            totalImages, totalDocuments, totalVideos, totalAudio, totalOthers
        );
    }

    /**
     * 获取文件类型分布字符串
     */
    public String getFileTypeDistribution() {
        if (filesByType == null || filesByType.isEmpty()) {
            return "No files";
        }

        return filesByType.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .reduce((a, b) -> a + ", " + b)
                .orElse("No files");
    }

    /**
     * 检查是否有文件
     */
    public boolean hasFiles() {
        return totalFiles > 0;
    }

    /**
     * 计算文件大小（友好的显示格式）
     */
    public String getFormattedTotalSize() {
        long size = totalSize;
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        }
    }

    /**
     * 安全地增加文件计数
     */
    public void incrementTotalFiles(int delta) {
        this.totalFiles = Math.max(0, this.totalFiles + delta);
    }

    /**
     * 安全地增加总大小
     */
    public void incrementTotalSize(long delta) {
        this.totalSize = Math.max(0L, this.totalSize + delta);
    }

    /**
     * 安全地增加分类计数
     */
    public void incrementCategoryCount(String category, int delta) {
        switch (category.toLowerCase()) {
            case "image" -> this.totalImages = Math.max(0, this.totalImages + delta);
            case "document" -> this.totalDocuments = Math.max(0, this.totalDocuments + delta);
            case "video" -> this.totalVideos = Math.max(0, this.totalVideos + delta);
            case "audio" -> this.totalAudio = Math.max(0, this.totalAudio + delta);
            default -> this.totalOthers = Math.max(0, this.totalOthers + delta);
        }
    }
}