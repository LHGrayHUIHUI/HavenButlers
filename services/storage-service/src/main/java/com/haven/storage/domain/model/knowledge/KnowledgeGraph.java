package com.haven.storage.domain.model.knowledge;

import lombok.Data;

/**
 * 知识图谱类
 *
 * @author HavenButler
 */
@Data
public class KnowledgeGraph {

    private String graphId;
    private String knowledgeBaseId;
    private String graphData;
    private String metadata;
}