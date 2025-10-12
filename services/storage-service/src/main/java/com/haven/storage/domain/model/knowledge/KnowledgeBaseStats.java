package com.haven.storage.domain.model.knowledge;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 知识库统计信息
 *
 * @author HavenButler
 */
@Data
public class KnowledgeBaseStats {

    private String knowledgeBaseId;
    private Integer documentCount;
    private Integer vectorCount;
    private LocalDateTime lastUpdated;
    private Map<String, Integer> tagStats;
}