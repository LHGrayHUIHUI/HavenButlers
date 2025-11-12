package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 文件存储的
 */
@Component
@Order(40)
@RequiredArgsConstructor
public class StorageOperationInterceptor implements FileProcessInterceptor {

    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        return null;

    }
}
