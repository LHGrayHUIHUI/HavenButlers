package com.haven.storage.validator;

/**
 * 文件大小校验器
 */
class FileSizeValidator implements FileMetadataInterceptor {
    private final long maxSize;

    public FileSizeValidator(long maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public ValidationResult proceed(FileDataChain fileDataChain) {
        return fileDataChain.proceed(fileDataChain.getFileUploadRequest());
    }

//    @Override
//    protected FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata) {
//        if (request.getFileSize() > maxSize) {
//            return FileProcessingResult.fail("文件大小超过限制: " + maxSize + " bytes");
//        }
//        return FileProcessingResult.success();
//    }
}
