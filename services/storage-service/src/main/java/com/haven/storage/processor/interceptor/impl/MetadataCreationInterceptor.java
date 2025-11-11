package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.extern.slf4j.Slf4j;

import java.util.EnumSet;
import java.util.Set;

/**
 * 元数据创建拦截器 我理解应该不需要当前拦截器，
 * 因为在上传的操作可以在数据库的操作的使用重新生成这元数据对象
 * 修改的话直接是查询数据库操作的
 *
 */
@Deprecated
@Slf4j
public class MetadataCreationInterceptor implements FileProcessInterceptor {
    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        log.info("【元数据创建拦截器】开始执行");

//        if (context.hasMetadata()) {
//            return chain.proceed(context);
//        }
        return chain.proceed(context);
    }

    @Override
    public Set<FileOperation> supportedOperations() {
        return EnumSet.of(FileOperation.UPLOAD);
    }
}
