package com.haven.storage.vectortag;

import lombok.Data;
import java.util.List;

/**
 * 文件向量匹配结果
 */
@Data
public class FileVectorMatch {
    private String fileId;
    private Double similarity;
    private VectorTag matchedTag;
    private List<VectorTag> allTags;
}