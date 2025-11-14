package com.haven.storage.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Comment;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 家庭存储统计信息实体 - 存储使用情况监控和分析
 * <p>
 * ============================================================================
 * 文件描述：家庭存储使用情况统计和监控数据的数据库映射实体
 * ============================================================================
 * <p>
 * 核心职责：
 * 1. 存储使用统计 - 实时跟踪和统计家庭存储空间使用情况
 * 2. 文件分类分析 - 按文件类型详细分类统计和分析使用模式
 * 3. 容量管理和预警 - 监控存储配额使用情况，提供容量预警
 * 4. 健康状态监控 - 跟踪存储后端的健康状态和可用性
 * 5. 趋势分析和预测 - 记录历史数据和峰值，支持增长趋势分析
 * 6. 性能优化支持 - 提供存储优化决策的数据支持
 * @author HavenButler
 * @version 1.0
 * @since 2024-01-01
 * @see FileMetadata 文件元数据实体，统计数据的主要来源
 * @see FileStorageData 文件存储数据实体，提供存储健康状态
 * @see com.haven.storage.service.FamilyStorageStatsService 统计服务，提供统计计算和更新逻辑
 */
@Slf4j
@Data
@Entity
@Table(name = "family_storage_stats", indexes = {
    @Index(name = "idx_family_id_stats", columnList = "family_id", unique = true),    // 家庭ID唯一索引
    @Index(name = "idx_last_updated", columnList = "updated_at"),                    // 更新时间索引
    @Index(name = "idx_storage_type", columnList = "storage_type"),                  // 存储类型索引
    @Index(name = "idx_total_size", columnList = "total_size"),                      // 总大小索引
    @Index(name = "idx_storage_healthy", columnList = "storage_healthy"),            // 健康状态索引
    @Index(name = "idx_quota_usage", columnList = "quota_usage_percentage")           // 配额使用率索引
})
@EntityListeners(AuditingEntityListener.class)
@Comment("家庭存储统计信息表 - 监控家庭存储使用情况和健康状态")
public class FamilyStorageStats {

    /**
     * 家庭唯一标识符
     * <p>
     * 作为主键，与家庭管理系统的家庭ID保持一致
     * 用于数据隔离和权限控制的基础，确保统计数据的准确性
     * <p>
     * 主键特性：
     * - 每个家庭对应唯一一条统计记录
     * - 支持高效的按家庭查询和更新
     * - 便于统计数据的聚合和分析
     * - 支持家庭数据的迁移和备份
     */
    @Id
    @Column(name = "family_id", length = 50, nullable = false)
    @Comment("家庭唯一标识符")
    private String familyId;

    /**
     * 乐观锁版本号 - 防止并发更新冲突
     * <p>
     * 每次更新自动递增，冲突时抛出OptimisticLockException
     * 用于保证统计数据在并发环境下的数据一致性
     * <p>
     * 并发控制场景：
     * - 多个用户同时上传文件到同一家庭
     * - 统计数据的定时重算和手动更新
     * - 系统管理和维护操作的并发执行
     * - 异步统计任务的并发处理
     */
    @Version
    @Column(name = "version", nullable = false)
    @Comment("乐观锁版本号")
    private Long version;

    // ==================== 基础统计信息 ====================

    /**
     * 总文件数量 - 包含所有类型的文件总数
     * <p>
     * 范围：0 ~ 21亿（int最大值）
     * 统计包括：图片、文档、视频、音频、其他文件
     * 用于家庭活跃度分析和使用模式评估
     * <p>
     * 统计规则：
     * - 仅统计活跃状态文件（未删除且启用）
     * - 包含所有文件类型和分类
     * - 实时更新，反映当前状态
     * - 定期与实际数据校验和修复
     */
    @Column(name = "total_files", nullable = false)
    @Comment("总文件数量")
    private int totalFiles = 0;

    /**
     * 总存储大小 - 所有文件占用的磁盘空间（字节）
     * <p>
     * 范围：0 ~ 9223372036854775807字节（约8EB）
     * 用于容量计费、配额管理和存储规划
     * 与 FileStorageData 中的实际存储大小保持一致
     * <p>
     * 计算规则：
     * - 统计所有活跃文件的实际存储大小
     * - 不包括软删除和不可用文件
     * - 考虑压缩和加密后的实际大小
     * - 支持定期校验和数据修复
     */
    @Column(name = "total_size", nullable = false)
    @Comment("总存储大小(字节)")
    private long totalSize = 0L;

    /**
     * 按类型统计文件数量 - 动态分类统计
     * <p>
     * 支持自定义文件类型分类，默认包括：
     * - image: 图片文件（JPEG、PNG、GIF等）
     * - document: 文档文件（PDF、Word、Excel等）
     * - video: 视频文件（MP4、AVI、MKV等）
     * - audio: 音频文件（MP3、WAV、FLAC等）
     * - other: 其他文件（压缩包、可执行文件等）
     * <p>
     * 扩展性：
     * - 支持动态添加新的文件类型
     * - 可配置分类规则和统计维度
     * - 支持自定义统计周期和聚合级别
     */
    @ElementCollection
    @CollectionTable(name = "family_stats_by_type", 
        joinColumns = @JoinColumn(name = "family_id"),
        indexes = @Index(name = "idx_stats_family_type", columnList = "family_id, file_type"))
    @MapKeyColumn(name = "file_type", length = 100)
    @Column(name = "file_count")
    @Comment("按类型统计文件数量")
    private Map<String, Integer> filesByType;

    // ==================== 详细分类统计信息 ====================

    /**
     * 图片文件数量 - 包括各种图片格式文件
     * <p>
     * 统计来源：基于MIME类型判断(image/*)
     * 常见格式：JPEG、PNG、GIF、BMP、TIFF、WebP、SVG等
     * 用于图片管理策略和存储优化
     */
    @Column(name = "total_images", nullable = false)
    @Comment("图片文件数量")
    private int totalImages = 0;

    /**
     * 文档文件数量 - 包括各种文档格式文件
     * <p>
     * 统计来源：基于MIME类型判断(application/pdf, text/*等)
     * 常见格式：PDF、Word、Excel、PowerPoint、TXT、Markdown、RTF等
     * 用于文档管理和知识库构建
     */
    @Column(name = "total_documents", nullable = false)
    @Comment("文档文件数量")
    private int totalDocuments = 0;

    /**
     * 视频文件数量 - 包括各种视频格式文件
     * <p>
     * 统计来源：基于MIME类型判断(video/*)
     * 常见格式：MP4、AVI、MKV、MOV、WMV、FLV、WebM、M4V等
     * 用于视频存储策略和转码优化
     */
    @Column(name = "total_videos", nullable = false)
    @Comment("视频文件数量")
    private int totalVideos = 0;

    /**
     * 音频文件数量 - 包括各种音频格式文件
     * <p>
     * 统计来源：基于MIME类型判断(audio/*)
     * 常见格式：MP3、WAV、FLAC、AAC、OGG、M4A、WMA等
     * 用于音频管理和流媒体优化
     */
    @Column(name = "total_audio", nullable = false)
    @Comment("音频文件数量")
    private int totalAudio = 0;

    /**
     * 其他文件数量 - 不属于上述分类的其他文件
     * <p>
     * 包括：压缩包、可执行文件、系统文件、代码文件等
     * 常见格式：ZIP、RAR、EXE、DMG、JAR、数据库文件、配置文件等
     * 用于系统文件管理和安全策略制定
     */
    @Column(name = "total_others", nullable = false)
    @Comment("其他文件数量")
    private int totalOthers = 0;

    // ==================== 文件特征统计信息 ====================

    /**
     * 最大文件大小 - 家庭中单个文件的最大尺寸（字节）
     * <p>
     * 用途：
     * - 存储容量规划和限制设置
     * - 大文件监控和管理策略
     * - 性能优化和带宽估算
     * - 存储成本分析和优化
     */
    @Column(name = "largest_file_size")
    @Comment("最大文件大小(字节)")
    private Long largestFileSize;

    /**
     * 最大文件名称 - 最大文件的原始文件名
     * <p>
     * 用途：
     * - 大文件定位和管理
     * - 用户展示和文件查找
     * - 存储优化决策支持
     * - 文件分析和异常检测
     */
    @Column(name = "largest_file_name", length = 255)
    @Comment("最大文件名称")
    private String largestFileName;

    /**
     * 最近文件时间 - 家庭中最新文件的上传时间
     * <p>
     * 用途：
     * - 家庭活跃度分析
     * - 用户行为模式分析
     * - 存储清理策略制定
     * - 数据备份和恢复策略
     */
    @Column(name = "most_recent_file_time")
    @Comment("最近文件时间")
    private LocalDateTime mostRecentFileTime;

    /**
     * 平均文件大小 - 家庭文件的平均尺寸（字节）
     * <p>
     * 计算方式：totalSize / totalFiles
     * 用于性能分析和存储优化决策
     * 瞬态字段，不持久化到数据库
     */
    @Transient
    private Double averageFileSize;

    // ==================== 操作统计与峰值信息 ====================

    /**
     * 总上传次数 - 累计文件上传操作次数
     * <p>
     * 用途：
     * - 用户活跃度统计
     * - 操作频率分析
     * - 系统负载评估
     * - 容量规划和预测
     */
    @Column(name = "total_uploads")
    @Comment("总上传次数")
    private int totalUploads = 0;

    /**
     * 总删除次数 - 累计文件删除操作次数
     * <p>
     * 用途：
     * - 存储空间管理
     * - 用户行为分析
     * - 数据生命周期评估
     * - 存储优化策略制定
     */
    @Column(name = "total_deletes")
    @Comment("总删除次数")
    private int totalDeletes = 0;

    /**
     * 峰值文件数量 - 历史最高文件数量
     * <p>
     * 用途：
     * - 容量规划和增长预测
     * - 存储策略优化
     * - 性能基线建立
     * - 扩容决策支持
     */
    @Column(name = "peak_file_count")
    @Comment("峰值文件数量")
    private int peakFileCount = 0;

    /**
     * 峰值存储大小 - 历史最高存储使用量（字节）
     * <p>
     * 用途：
     * - 容量规划和预警
     * - 存储扩容决策
     * - 成本预估和预算制定
     * - 增长趋势分析
     */
    @Column(name = "peak_storage_size")
    @Comment("峰值存储大小")
    private long peakStorageSize = 0L;

    // ==================== 存储配置和健康状态 ====================


    /**
     * 存储健康状态 - 存储后端的可用性和性能状态
     * <p>
     * true: 健康 - 存储后端正常可用，性能良好
     * false: 异常 - 存储后端存在问题或不可用
     * 用于监控存储系统的运行状态
     */
    @Column(name = "storage_healthy", nullable = false)
    @Comment("存储健康状态")
    private boolean storageHealthy = true;

    /**
     * 存储配额限制 - 家庭的最大存储容量限制（字节）
     * <p>
     * 用于容量控制和计费管理
     * 如果为null表示无限制
     * 支持动态调整和分级配额管理
     */
    @Column(name = "quota_limit")
    @Comment("存储配额限制(字节)")
    private Long quotaLimit;

    /**
     * 配额使用百分比 - 存储配额的使用率（0-100）
     * <p>
     * 计算方式：(totalSize / quotaLimit) * 100
     * 用于配额监控和预警，支持两级小数精度
     */
    @Column(name = "quota_usage_percentage", precision = 5, scale = 2)
    @Comment("配额使用百分比")
    private Double quotaUsagePercentage;

    /**
     * 最后清理时间 - 最后一次执行存储清理的时间
     * <p>
     * 用于：
     * - 清理策略执行跟踪
     * - 存储维护计划制定
     * - 定期任务调度参考
     * - 清理效果评估
     */
    @Column(name = "last_cleanup_time")
    @Comment("最后清理时间")
    private LocalDateTime lastCleanupTime;

    // ==================== 审计字段 ====================

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Comment("创建时间")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    @Comment("更新时间")
    private LocalDateTime updatedAt;

    // ==================== 业务方法 ====================

    /**
     * 更新峰值统计 - 记录历史最高值
     * <p>
     * 在文件上传或存储增加后调用
     * 更新文件数量和存储大小的历史峰值
     */
    public void updatePeakStats() {
        if (totalFiles > peakFileCount) {
            peakFileCount = totalFiles;
            log.debug("更新峰值文件数量：{} -> {}", peakFileCount, totalFiles);
        }
        if (totalSize > peakStorageSize) {
            peakStorageSize = totalSize;
            log.debug("更新峰值存储大小：{} -> {}", formatSize(peakStorageSize), formatSize(totalSize));
        }
    }

    /**
     * 计算存储使用率（基于预设的容量限制）
     * <p>
     * 如果没有设置配额限制，返回基于默认容量（100GB）的使用率
     *
     * @return 使用率百分比（0-100）
     */
    public double getStorageUsagePercentage() {
        long limitBytes = quotaLimit != null ? quotaLimit : 100L * 1024 * 1024 * 1024; // 默认100GB
        long totalBytes = totalSize;

        if (totalBytes <= 0) {
            return 0.0;
        }

        double usage = (totalBytes * 100.0) / limitBytes;
        return Math.min(100.0, usage);
    }

    /**
     * 获取配额使用百分比 - 重新计算并更新配额使用率
     *
     * @return 配额使用百分比，如果无配额限制则返回-1
     */
    public double getQuotaUsagePercentage() {
        if (quotaLimit == null || quotaLimit <= 0) {
            return -1; // 无配额限制
        }
        
        double usage = (totalSize * 100.0) / quotaLimit;
        this.quotaUsagePercentage = Math.min(100.0, usage);
        return this.quotaUsagePercentage;
    }

    /**
     * 检查是否接近容量限制（默认80%）
     *
     * @return true - 接近容量限制，false - 容量充足
     */
    public boolean isNearCapacityLimit() {
        return getStorageUsagePercentage() > 80.0;
    }

    /**
     * 检查是否接近配额限制（默认90%）
     *
     * @return true - 接近配额限制，false - 配额充足
     */
    public boolean isNearQuotaLimit() {
        if (quotaLimit == null) {
            return false; // 无配额限制
        }
        return getQuotaUsagePercentage() > 90.0;
    }

    /**
     * 获取平均文件大小
     *
     * @return 平均文件大小（字节），如果没有文件则返回0
     */
    public double getAverageFileSize() {
        if (totalFiles <= 0) {
            return 0.0;
        }
        return (double) totalSize / totalFiles;
    }

    /**
     * 记录文件上传操作
     * <p>
     * 增加上传统计和更新最近文件时间
     */
    public void recordUpload() {
        this.totalUploads++;
        this.mostRecentFileTime = LocalDateTime.now();
        updatePeakStats();
    }

    /**
     * 记录文件删除操作
     * <p>
     * 增加删除统计
     */
    public void recordDelete() {
        this.totalDeletes++;
    }

    /**
     * 记录存储清理操作
     * <p>
     * 更新最后清理时间
     */
    public void recordCleanup() {
        this.lastCleanupTime = LocalDateTime.now();
    }

    /**
     * 安全地增加文件计数
     * <p>
     * 确保文件数量不为负数
     *
     * @param delta 增量值，可以为负数
     */
    public void incrementTotalFiles(int delta) {
        this.totalFiles = Math.max(0, this.totalFiles + delta);
    }

    /**
     * 安全地增加总大小
     * <p>
     * 确保总大小不为负数
     *
     * @param delta 增量值，可以为负数
     */
    public void incrementTotalSize(long delta) {
        this.totalSize = Math.max(0L, this.totalSize + delta);
    }

    /**
     * 安全地增加分类计数
     * <p>
     * 根据文件类型增加对应的分类计数
     *
     * @param category 文件类型分类
     * @param delta 增量值，可以为负数
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

    /**
     * 更新最大文件信息
     * <p>
     * 比较当前文件大小，如果是最大文件则更新记录
     *
     * @param fileName 文件名
     * @param fileSize 文件大小
     */
    public void updateLargestFileInfo(String fileName, long fileSize) {
        if (largestFileSize == null || fileSize > largestFileSize) {
            this.largestFileSize = fileSize;
            this.largestFileName = fileName;
            log.debug("更新最大文件：{} ({}字节)", fileName, fileSize);
        }
    }

    /**
     * 获取统计摘要 - 生成人类可读的统计摘要
     *
     * @return 统计摘要字符串
     */
    public String getSummary() {
        return String.format(
            "FamilyStorageStats{familyId='%s', totalFiles=%d, totalSize=%s, " +
            "totalImages=%d, totalDocuments=%d, totalVideos=%d, totalAudio=%d, totalOthers=%d, " +
            "storageType='%s', healthy=%s, usage=%.1f%%}",
            familyId, totalFiles, getFormattedTotalSize(),
            totalImages, totalDocuments, totalVideos, totalAudio, totalOthers,
            storageType, storageHealthy, getStorageUsagePercentage()
        );
    }

    /**
     * 获取文件类型分布字符串 - 生成类型分布的可读描述
     *
     * @return 类型分布字符串
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
     *
     * @return true - 有文件，false - 无文件
     */
    public boolean hasFiles() {
        return totalFiles > 0;
    }

    /**
     * 检查存储是否健康
     *
     * @return true - 健康，false - 异常
     */
    public boolean isStorageHealthy() {
        return storageHealthy;
    }

    /**
     * 设置存储健康状态
     *
     * @param healthy 健康状态
     */
    public void setStorageHealthyStatus(boolean healthy) {
        this.storageHealthy = healthy;
        log.info("家庭 {} 存储健康状态更新为：{}", familyId, healthy ? "健康" : "异常");
    }

    /**
     * 获取格式化的总大小 - 友好的文件大小显示
     *
     * @return 格式化的大小字符串
     */
    public String getFormattedTotalSize() {
        return formatSize(totalSize);
    }

    /**
     * 获取格式化的配额限制 - 友好的配额限制显示
     *
     * @return 格式化的配额限制字符串
     */
    public String getFormattedQuotaLimit() {
        return quotaLimit != null ? formatSize(quotaLimit) : "无限制";
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 格式化文件大小 - 将字节数转换为友好的显示格式
     *
     * @param size 文件大小（字节）
     * @return 格式化的大小字符串
     */
    private String formatSize(long size) {
        if (size < 1024) {
            return size + " B";
        } else if (size < 1024 * 1024) {
            return String.format("%.1f KB", size / 1024.0);
        } else if (size < 1024 * 1024 * 1024) {
            return String.format("%.1f MB", size / (1024.0 * 1024.0));
        } else if (size < 1024L * 1024 * 1024 * 1024) {
            return String.format("%.1f GB", size / (1024.0 * 1024.0 * 1024.0));
        } else {
            return String.format("%.1f TB", size / (1024.0 * 1024.0 * 1024.0 * 1024.0));
        }
    }

    // ==================== JPA 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        if (this.totalFiles == 0) {
            this.totalFiles = 0;
        }
        if (this.totalSize == 0L) {
            this.totalSize = 0L;
        }
        if (this.storageHealthy == null) {
            this.storageHealthy = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        // 自动更新配额使用百分比
        if (quotaLimit != null && quotaLimit > 0) {
            getQuotaUsagePercentage();
        }
        
        // 自动计算平均文件大小
        if (totalFiles > 0) {
            this.averageFileSize = getAverageFileSize();
        }
    }
}