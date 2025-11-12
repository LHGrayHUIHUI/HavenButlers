package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import com.haven.storage.operation.storage.StorageOperationRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * 存储路径生成拦截器
 * 1. 上传文件{@code FileOperation.UPLOAD} 生成文件的存储路径的
 * 2. 修改 查看 分享都需要通过数据库去查询或者通过传入的文件地址来使用的。
 */
@Component
@Order(30)
@RequiredArgsConstructor
public class StoragePathGenerationInterceptor implements FileProcessInterceptor {
    // 1. Inject the registry
    private final StorageOperationRegistry storageRegistry;

    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        if (context.getOperationType() == FileOperation.UPLOAD) {
            context.setFileStorageInfo(createFileStorageInfo(context));
        }
        return chain.proceed(context);
    }


    /**
     * 根据规则创建文件的地址
     *
     * @param context
     * @return
     * @see StorageAdapter
     */
    private FileStorageInfo createFileStorageInfo(FileProcessContext context) {
        StorageAdapter strategy = storageRegistry.getStorageAdapter(context.getStorageType());
        StorageType storageType = context.getStorageType();
        String bucketName = strategy.buildFamilyBucketName(context.getFileBasicMetadata().getFamilyId());
        String fileName = context.getFileBasicMetadata().getFileName();
        String filePath = strategy.buildFilePath(context.getFileBasicMetadata().getFamilyId(), context.getFileBasicMetadata().getFileCategory().getCategoryName());
        return new FileStorageInfo(storageType, bucketName, fileName, filePath);
    }


    @Override
    public Set<FileOperation> supportedOperations() {
        return FileProcessInterceptor.super.supportedOperations();
    }
}
