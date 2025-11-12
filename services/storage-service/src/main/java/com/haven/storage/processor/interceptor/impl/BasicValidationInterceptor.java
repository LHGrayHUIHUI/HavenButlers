package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.Set;

/**
 * 基础校验拦截器
 * 文件的上传和修改
 * 1.文件是否真实存在的
 * 2.文件的类型
 * 3.文件的名称
 * 4.文件的id是否创建
 * 5.文件的持有人和家庭id的信息
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class BasicValidationInterceptor implements FileProcessInterceptor {
    /**
     * 文件的基础认证的
     *
     * @param context
     * @return
     */
    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain interceptorChain) {
        log.info("【基础校验拦截器】开始执行");
        //数据流的基础验证
        if (context.getBufferedSource() == null || !context.getBufferedSource().isOpen()) {
            log.warn("文件不能为空: traceId={}", context.getTraceId());
            return ProcessResult.fail("文件不能为空");
        }
        long maxSize = 100 * 1024 * 1024L;
        if (context.getFileBasicMetadata().getFileSize() > maxSize) {
            return ProcessResult.fail("文件大小超过限制");
        }

        return interceptorChain.proceed(context);
    }

    /**
     * 文件的创建和修改需要基础认证的
     *
     * @return
     */
    @Override
    public Set<FileOperation> supportedOperations() {
        return EnumSet.of(
                FileOperation.UPLOAD,
                FileOperation.MODIFY
        );
    }


}
