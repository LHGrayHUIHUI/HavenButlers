package com.haven.storage.knowledge;

import lombok.Data;

/**
 * 创建知识库请求
 */
@Data
public class CreateKnowledgeBaseRequest {
    private String familyId;
    private String name;
    private String description;
    private String category;
    private String creatorUserId;
}