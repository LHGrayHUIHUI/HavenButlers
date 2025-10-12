package com.haven.storage.domain.model.vectortag;

import com.haven.base.model.entity.BaseEntity;
import com.haven.storage.domain.model.vectortag.TagType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 向量标签实体
 * 继承BaseEntity获得通用字段和方法
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VectorTag extends BaseEntity {
    private String tagId;
    private String fileId;
    private String familyId;
    private String tagName;
    private List<Double> tagVector;
    private Double similarityScore;
    private TagType tagType;
    private String userId;
}