package com.haven.storage.domain.model.file;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 家庭存储统计信息
 */
@Data
public class FamilyStorageStats {
    private String familyId;
    private int totalFiles;
    private long totalSize;
    private Map<String, Integer> filesByType;
    private LocalDateTime lastUpdated;

    // 存储适配器相关信息
    private String storageType;     // 当前存储类型
    private boolean storageHealthy; // 存储健康状态
}