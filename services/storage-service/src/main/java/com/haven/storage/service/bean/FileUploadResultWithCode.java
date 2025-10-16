package com.haven.storage.service.bean;

import com.haven.storage.domain.model.file.FileMetadata;
import com.haven.storage.service.FileStorageService;

/**
 * 文件上传结果封装类
 * <p>
 * 统一文件上传处理的结果返回类型
 */
public record FileUploadResultWithCode(boolean success, FileMetadata fileMetadata) {

    public static FileUploadResultWithCode success(FileMetadata fileMetadata) {
        return new FileUploadResultWithCode(true, fileMetadata);
    }

    public static FileUploadResultWithCode failure() {
        return new FileUploadResultWithCode(false, null);
    }
}
