package com.haven.storage.knowledge;

import lombok.Data;

import java.util.List;

/**
 * 知识库搜索请求
 *
 * @author HavenButler
 */
@Data
public class KnowledgeSearchRequest {

    private String query;
    private String searchType = "hybrid";
    private Integer topK = 10;
    private Double threshold = 0.7;
    private List<String> tags;
    private String userId;
}