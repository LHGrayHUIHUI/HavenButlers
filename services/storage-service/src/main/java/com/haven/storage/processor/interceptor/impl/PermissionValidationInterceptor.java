package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.extern.slf4j.Slf4j;

/**
 * 权限验证流程
 */
@Slf4j
public class PermissionValidationInterceptor implements FileProcessInterceptor {


    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        log.info("【权限校验拦截器】开始执行，操作类型: {}", context.getOperationType());

        // 根据不同操作类型进行权限验证
        // TODO: 实际项目中实现具体的权限检查逻辑

        log.info("【权限校验拦截器】校验通过");
        return chain.proceed(context);
    }
}
