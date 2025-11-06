package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;
import okio.BufferedSource;

import java.util.Set;

/**
 * 文件类型校验器
 * 1.文件类型的校验
 * 2. {@FileUploadRequest} 根据文件的类型
 */
class FileTypeValidator extends FileValidator {
    private final Set<String> allowedTypes;

    public FileTypeValidator(Set<String> allowedTypes) {
        this.allowedTypes = allowedTypes;
    }

    @Override
    protected FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata) {
        String fileType = fileBasicMetadata.getFileFormat();
        if (!allowedTypes.contains(fileType)) {
            return FileProcessingResult.fail("不支持的文件类型: " + fileType);
        }
        return FileProcessingResult.success();
    }
}
