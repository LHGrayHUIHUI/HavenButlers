package com.haven.storage.domain.model.file;

import lombok.Data;

import java.io.InputStream;

/**
 * 文件下载结果 - 支持流式传输Ø
 */
@Data
public class FileDownloadResult {
    private boolean success;
    private byte[] fileContent;      // 兼容旧的下载方式
    private InputStream inputStream;      // 兼容旧的下载方式
    private FileMetadata fileMetadata; // 新增：文件元数据
    private String errorMessage;
    private String traceId;
    private String fileName;         // 文件名（兼容性）
    private String contentType;      // 内容类型（兼容性）

    // ========== 兼容性方法 ==========

    public static FileDownloadResult success(byte[] fileContent, String fileName, String contentType, String traceId) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = true;
        result.fileContent = fileContent;
        result.traceId = traceId;
        result.fileName = fileName;
        result.contentType = contentType;

        return result;
    }

    public static FileDownloadResult success(InputStream inputStream, String fileName, String contentType, String traceId) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = true;
        result.inputStream = inputStream;
        result.traceId = traceId;
        result.fileName = fileName;
        result.contentType = contentType;

        return result;
    }

    // ========== 错误结果方法 ==========

    public static FileDownloadResult error(String errorMessage, String traceId) {
        FileDownloadResult result = failure(errorMessage);
        result.traceId = traceId;
        return result;
    }

    public static FileDownloadResult failure(String errorMessage) {
        FileDownloadResult result = new FileDownloadResult();
        result.success = false;
        result.errorMessage = errorMessage;
        return result;
    }

    // ========== 便捷方法 ==========


    /**
     * 获取有效的文件名（优先使用元数据中的原始文件名）
     */
    public String getEffectiveFileName() {
        if (fileMetadata != null && fileMetadata.getOriginalFileName() != null) {
            return fileMetadata.getOriginalFileName();
        }
        return fileName;
    }
}