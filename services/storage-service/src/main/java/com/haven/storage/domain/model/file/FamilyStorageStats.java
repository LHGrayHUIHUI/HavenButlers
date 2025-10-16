package com.haven.storage.domain.model.file;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 家庭存储统计信息
 */
@Data
@Entity
@Table(name = "family_storage_stats", indexes = {
    @Index(name = "idx_family_id_stats", columnList = "family_id", unique = true),
    @Index(name = "idx_last_updated", columnList = "last_updated")
})
public class FamilyStorageStats {

    @Id
    @Column(name = "family_id", length = 50, nullable = false)
    private String familyId;

    @Column(name = "total_files", nullable = false)
    private int totalFiles;

    @Column(name = "total_size", nullable = false)
    private long totalSize;

    @ElementCollection
    @CollectionTable(name = "family_stats_by_type", joinColumns = @JoinColumn(name = "family_id"))
    @MapKeyColumn(name = "file_type", length = 100)
    @Column(name = "file_count")
    private Map<String, Integer> filesByType;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    // 存储适配器相关信息
    @Column(name = "storage_type", length = 50)
    private String storageType;     // 当前存储类型

    @Column(name = "storage_healthy", nullable = false)
    private boolean storageHealthy; // 存储健康状态

    // 额外的统计信息
    @Column(name = "total_images")
    private Integer totalImages;

    @Column(name = "total_documents")
    private Integer totalDocuments;

    @Column(name = "total_videos")
    private Integer totalVideos;

    @Column(name = "total_others")
    private Integer totalOthers;

    @Column(name = "largest_file_size")
    private Long largestFileSize;

    @Column(name = "largest_file_name", length = 255)
    private String largestFileName;

    @Column(name = "most_recent_file_time")
    private LocalDateTime mostRecentFileTime;
}