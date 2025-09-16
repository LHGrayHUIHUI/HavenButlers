package com.haven.storage.vectortag;

import lombok.Data;
import java.util.List;

/**
 * 向量标签生成结果
 */
@Data
public class VectorTagResult {
    private boolean success;
    private List<VectorTag> vectorTags;
    private String errorMessage;
    private String traceId;
    private int generatedTagCount;

    public static VectorTagResult success(List<VectorTag> vectorTags, String traceId) {
        VectorTagResult result = new VectorTagResult();
        result.success = true;
        result.vectorTags = vectorTags;
        result.generatedTagCount = vectorTags.size();
        result.traceId = traceId;
        return result;
    }

    public static VectorTagResult error(String errorMessage, String traceId) {
        VectorTagResult result = new VectorTagResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.traceId = traceId;
        return result;
    }
}