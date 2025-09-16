package com.haven.storage.vectortag;

import lombok.Data;

/**
 * 生成向量标签请求
 */
@Data
public class GenerateVectorTagRequest {
    private String fileId;
    private String familyId;
    private String content;
    private String fileName;
    private String fileType;
    private String userId;
}