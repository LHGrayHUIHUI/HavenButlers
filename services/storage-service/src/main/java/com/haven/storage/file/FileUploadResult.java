package com.haven.storage.file;

import lombok.Data;

/**
 * 文件上传结果
 */
@Data
public class FileUploadResult {
    private boolean success;
    private FileMetadata fileMetadata;
    private String errorMessage;
    private String traceId;

    public static FileUploadResult success(FileMetadata metadata, String traceId) {
        FileUploadResult result = new FileUploadResult();
        result.success = true;
        result.fileMetadata = metadata;
        result.traceId = traceId;
        return result;
    }

    public static FileUploadResult error(String errorMessage, String traceId) {
        FileUploadResult result = new FileUploadResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.traceId = traceId;
        return result;
    }
}