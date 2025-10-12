package com.haven.storage.vectortag;

import lombok.Data;
import java.util.List;

/**
 * 向量搜索结果
 */
@Data
public class VectorSearchResult {
    private String familyId;
    private String query;
    private List<FileVectorMatch> matches;
    private int totalMatches;
    private String traceId;
}