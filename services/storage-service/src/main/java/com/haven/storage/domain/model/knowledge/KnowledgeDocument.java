package com.haven.storage.domain.model.knowledge;

import com.haven.base.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识库文档实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class KnowledgeDocument extends BaseEntity {
    private String documentId;
    private String knowledgeBaseId;
    private String familyId;
    private String title;
    private String content;
    private String sourceUrl;
    private String fileId;
    private String addedBy;
    private List<String> tags;
    private LocalDateTime addedAt;      // 添加时间
}