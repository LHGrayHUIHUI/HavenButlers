package com.haven.storage.knowledge;

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