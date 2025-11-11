package com.haven.storage.validator;

import com.haven.storage.domain.model.entity.FileMetadata;
import com.haven.storage.domain.model.file.FileUploadRequest;

/**
 * 拦截器的继承的类
 */
public interface FileMetadataInterceptor {
    ValidationResult interceptor(FileInterceptorChain fileDataChain);

    /**
     * 拦截器的包装类的基类
     */
    interface FileInterceptorChain {
    }

    /***
     * 这个是文件校验的拦截器的包装类的
     */
    interface ValidatorInterceptorChain extends FileInterceptorChain {
        /**
         * 获取需要检验的文件数据
         *
         * @return
         */
        FileUploadRequest getFileUploadRequest();

        /**
         * 执行下一步的操作
         *
         * @param fileUploadRequest
         * @return
         */
        ValidationResult proceed(FileUploadRequest fileUploadRequest);
    }

    /**
     * 文件数据校验处理的基类的
     */
    interface FileDataChain extends ValidatorInterceptorChain {
        FileMetadata getFileMetadata();

        ValidationResult proceed(FileMetadata fileMetadata);
    }
}
