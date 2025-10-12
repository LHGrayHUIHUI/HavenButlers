package com.haven.storage.vectortag;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 家庭标签统计信息
 */
@Data
public class FamilyTagStats {
    private String familyId;
    private int totalTags;
    private int totalFiles;
    private int uniqueTagCount;
    private Map<String, Integer> tagDistribution;
    private List<String> popularTags;
    private LocalDateTime lastUpdated;
}