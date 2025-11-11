package com.haven.storage.processor.interceptor;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;

import java.util.EnumSet;
import java.util.Set;

/**
 * 拦截器接口
 */

public interface FileProcessInterceptor {
    ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain);

    /**
     * 暂时无业务逻辑适配可以暂时先写一个 100
     *
     * @return
     */
    default int getOrder() {
        return 100;
    }

    default String getName() {
        return this.getClass().getSimpleName();
    }

    default Set<FileOperation> supportedOperations() {
        return EnumSet.allOf(FileOperation.class);
    }
}
