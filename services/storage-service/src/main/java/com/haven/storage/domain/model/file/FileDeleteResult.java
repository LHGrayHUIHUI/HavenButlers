package com.haven.storage.domain.model.file;

import lombok.Data;

/**
 * 文件删除结果
 */
@Data
public class FileDeleteResult {
    private boolean success;
    private String deletedFileName;
    private String errorMessage;
    private String traceId;

    public static FileDeleteResult success(String fileName, String traceId) {
        FileDeleteResult result = new FileDeleteResult();
        result.success = true;
        result.deletedFileName = fileName;
        result.traceId = traceId;
        return result;
    }

    public static FileDeleteResult error(String errorMessage, String traceId) {
        FileDeleteResult result = failure(errorMessage);
        result.traceId = traceId;
        return result;
    }

    /**
     * 创建失败结果（适配器模式用）
     */
    public static FileDeleteResult failure(String errorMessage) {
        FileDeleteResult result = new FileDeleteResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }
}