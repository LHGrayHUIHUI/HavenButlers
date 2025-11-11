package com.haven.storage.processor.interceptor;

import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;

@FunctionalInterface
public interface FileInterceptorChain {
    ProcessResult proceed(FileProcessContext context);
}
