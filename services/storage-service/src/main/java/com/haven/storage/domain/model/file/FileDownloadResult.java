package com.haven.storage.domain.model.file;

import lombok.Data;

/**
 * 文件下载结果
 */
@Data
public class FileDownloadResult {
    private boolean success;
    private byte[] fileContent;
    private FileMetadata fileMetadata;
    private String errorMessage;
    private String traceId;
    private String fileName;         // 文件名
    private String contentType;      // 内容类型

    public static FileDownloadResult success(byte[] content, FileMetadata metadata, String traceId) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = true;
        result.fileContent = content;
        result.fileMetadata = metadata;
        result.traceId = traceId;
        if (metadata != null) {
            result.fileName = metadata.getOriginalName();
            result.contentType = metadata.getContentType();
        }
        return result;
    }

    public static FileDownloadResult error(String errorMessage, String traceId) {
        FileDownloadResult result = failure(errorMessage);
        result.traceId = traceId;
        return result;
    }

    public static FileDownloadResult failure(String s) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = false;
        result.errorMessage = s;
        return result;
    }
}