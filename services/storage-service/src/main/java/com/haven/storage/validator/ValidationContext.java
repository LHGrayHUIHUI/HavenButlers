package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileUploadRequest;
import com.haven.storage.model.bean.FileBasicMetadata;
import com.haven.storage.model.bean.FileProcessingResult;
import okio.BufferedSource;

/**
 * 策略上下文
 */
public class ValidationContext {
    private FileProcessingStrategy strategy;

    public void setStrategy(FileProcessingStrategy strategy) {
        this.strategy = strategy;
    }

    public FileProcessingResult execute(FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        return strategy.validate(fileBasicMetadata, bufferedSource);
    }
}
