package com.haven.storage.domain.model.knowledge;

import lombok.Data;

import java.util.Map;

/**
 * 搜索结果项
 *
 * @author HavenButler
 */
@Data
public class SearchResultItem {

    private String documentId;
    private String chunkId;
    private String content;
    private Double similarity;
    private Map<String, Object> metadata;
}