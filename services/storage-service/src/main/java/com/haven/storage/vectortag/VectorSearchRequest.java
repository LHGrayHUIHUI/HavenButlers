package com.haven.storage.vectortag;

import lombok.Data;

/**
 * 向量搜索请求
 */
@Data
public class VectorSearchRequest {
    private String familyId;
    private String query;
    private Integer topK;
    private Double minSimilarity;
    private String userId;
}