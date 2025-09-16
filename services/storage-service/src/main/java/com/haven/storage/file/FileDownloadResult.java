package com.haven.storage.file;

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

    public static FileDownloadResult success(byte[] content, FileMetadata metadata, String traceId) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = true;
        result.fileContent = content;
        result.fileMetadata = metadata;
        result.traceId = traceId;
        return result;
    }

    public static FileDownloadResult error(String errorMessage, String traceId) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = false;
        result.errorMessage = errorMessage;
        result.traceId = traceId;
        return result;
    }
}