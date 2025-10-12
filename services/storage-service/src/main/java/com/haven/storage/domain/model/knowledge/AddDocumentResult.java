package com.haven.storage.domain.model.knowledge;

import com.haven.storage.domain.model.knowledge.KnowledgeDocument;
import lombok.Data;

/**
 * 添加文档结果
 */
@Data
public class AddDocumentResult {
    private boolean success;
    private KnowledgeDocument document;
    private int vectorCount;
    private String errorMessage;
    private String traceId;

    public static AddDocumentResult success(KnowledgeDocument document, int vectorCount, String traceId) {
        AddDocumentResult result = new AddDocumentResult();
        result.success = true;
        result.document = document;
        result.vectorCount = vectorCount;
        result.traceId = traceId;
        return result;
    }

    public static AddDocumentResult error(String errorMessage, String traceId) {
        AddDocumentResult result = new AddDocumentResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.traceId = traceId;
        return result;
    }
}