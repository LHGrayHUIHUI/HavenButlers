package com.haven.storage.domain.model.knowledge;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库搜索结果
 *
 * @author HavenButler
 */
@Data
public class KnowledgeSearchResult {

    private String knowledgeBaseId;
    private String query;
    private List<SearchResultItem> results;
    private Integer totalResults;
    private Long searchTime;
    private String traceId;
}