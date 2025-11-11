package com.haven.storage.validator;

import com.haven.storage.domain.model.file.FileBasicMetadata;
import com.haven.storage.domain.model.file.FileProcessingResult;
import lombok.Setter;
import okio.BufferedSource;

/**
 * 责任链的方式做文件的校验的 适合所有项目中所有校验的方式
 * 责任链+策略模式+建造者模式
 */
@Setter
public abstract class FileValidator implements FileMetadataInterceptor {

    protected FileValidator next;

    /**
     * 这个方法是作为认证的
     * @param fileBasicMetadata
     * @param bufferedSource
     * @return
     */
    public FileProcessingResult validate(FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        FileProcessingResult result = proceed(fileBasicMetadata);
        if (!result.valid() || next == null) {
            return result;
        }
        return doNext(next, fileBasicMetadata, bufferedSource);
    }

    /**
     * 重写这个方法可以获取执行后的返回的参数的
     *
     * @param next
     * @param fileBasicMetadata
     * @param bufferedSource
     * @return
     */
    protected FileProcessingResult doNext(FileValidator next, FileBasicMetadata fileBasicMetadata, BufferedSource bufferedSource) {
        return next.validate(fileBasicMetadata, bufferedSource);
    }

    protected abstract FileProcessingResult proceed(FileBasicMetadata fileBasicMetadata);
}
