package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;

/**
 * 文件大小校验器
 */
class FileSizeValidator extends FileValidator {
    private final long maxSize;

    public FileSizeValidator(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    protected FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata) {
        if (request.getFileSize() > maxSize) {
            return FileProcessingResult.fail("文件大小超过限制: " + maxSize + " bytes");
        }
        return FileProcessingResult.success();
    }
}
