package com.haven.storage.knowledge;

import com.haven.base.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 知识库实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeBase extends BaseEntity {
    private String knowledgeBaseId;
    private String familyId;
    private String name;
    private String description;
    private String category;
    private String createdBy;
    private Integer documentCount;
    private Integer vectorCount;
    private KnowledgeBaseConfig config;
}