package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.file.FileProcessingResult;
import okio.BufferedSource;

import java.util.regex.Pattern;

/**
 * 文件名校验器
 */
class FileNameValidator extends FileValidator {
    private final Pattern pattern = Pattern.compile("^[a-zA-Z0-9._-]+$");

    @Override
    protected FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata) {
        String fileName = fileBasicMetadata.getFileName();
        if (fileName == null || fileName.isEmpty()) {
            return FileProcessingResult.fail("文件名不能为空");
        }
        if (!pattern.matcher(fileName).matches()) {
            return FileProcessingResult.fail("文件名包含非法字符");
        }
        return FileProcessingResult.success();
    }

    @Override
    protected FileProcessingResult doNext(FileValidator next, FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        FileProcessingResult fileProcessingResult = super.doNext(next, fileBasicMetadata, bufferedSource);

        return fileProcessingResult;
    }

    @Override
    public ValidationResult proceed(FileMetadataInterceptor fileMetadataInterceptor) {
        ValidationResult proceed = fileMetadataInterceptor.proceed(next);
        return proceed;
    }
}
