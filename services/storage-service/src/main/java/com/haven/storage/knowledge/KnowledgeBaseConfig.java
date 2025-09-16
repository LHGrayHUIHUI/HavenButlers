package com.haven.storage.knowledge;

import lombok.Data;

/**
 * 知识库配置
 */
@Data
public class KnowledgeBaseConfig {
    private String embeddingModel;
    private Integer chunkSize;
    private Integer vectorDimension;
    private String language;
    private Boolean enableSemanticSearch;
    private Boolean enableKnowledgeGraph;
}