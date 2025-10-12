package com.haven.storage.domain.model.knowledge;

import lombok.Data;

/**
 * 文档分块
 */
@Data
public class DocumentChunk {
    private String chunkId;
    private String documentId;
    private Integer chunkIndex;
    private String content;
    private Integer startOffset;
    private Integer endOffset;
}