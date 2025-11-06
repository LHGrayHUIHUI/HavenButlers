package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;
import okio.BufferedSource;

/**
 * 视频文件的类型的校验
 */

public class VideoValidationStrategy implements FileProcessingStrategy {
    @Override
    public FileProcessingResult validate(FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        return FileProcessingResult.success();
    }
}
