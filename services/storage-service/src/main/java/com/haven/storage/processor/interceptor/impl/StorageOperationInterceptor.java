package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.domain.model.file.FileStorageInfo;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.operation.storage.StorageOperationRegistry;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import okio.BufferedSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 文件存储的类
 */
@Component
@Order(40)

public class StorageOperationInterceptor implements FileProcessInterceptor {
    // 1. 最佳实践：使用 final 字段和构造函数注入
    private final StorageOperationRegistry storageRegistry;

    public StorageOperationInterceptor(StorageOperationRegistry storageRegistry) {
        this.storageRegistry = storageRegistry;
    }

    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {
        // 1. 获取适配器
        StorageAdapter storageAdapter = storageRegistry.getStorageAdapter(context.getStorageType());

        // 2. 如果是 UPLOAD/MODIFY 操作，先生成存储地址信息
        FileStorageInfo fileStorageInfo = getFileStorageInfo(storageAdapter, context);

        // 3. 执行文件存储操作分派
        ProcessResult storageResult = dispatchFileStorage(storageAdapter, context);

        // 4. 如果存储操作失败，中断责任链并返回失败结果
        if (!storageResult.isSuccess()) {
            return storageResult;
        }

        // 5. 存储操作成功，继续链条的下一个环节 (例如 DatabaseOperationInterceptor)
        return chain.proceed(context);

    }


    /**
     * 根据上下文的操作类型，分派给 StorageAdapter 执行具体的存储操作
     */
    private ProcessResult dispatchFileStorage(StorageAdapter storageAdapter, FileProcessContext context) {
        FileOperation operation = context.getOperationType();
        FileStorageInfo info = context.getFileStorageInfo();

        // 文件ID和家庭ID是下载/删除操作的核心参数
        String fileId = (info != null && info.getFileId() != null) ? info.getFileId() : context.getFileBasicMetadata().getFileId();
        String familyId = context.getFileBasicMetadata().getFamilyId();

        ProcessResult result;

        try {
            switch (operation) {
                case UPLOAD:
                case MODIFY: // 修改操作通常也是上传覆盖
                    // 调用上传接口：boolean uploadFile(FileStorageInfo info, BufferedSource source)
                    boolean uploadSuccess = storageAdapter.uploadFile(info, context.getBufferedSource());
                    if (uploadSuccess) {
                        context.setStage(FileProcessContext.ProcessingStage.FILE_STORED); // 更新阶段
                        result = ProcessResult.success("文件上传/修改成功");
                    } else {
                        result = ProcessResult.failure("文件上传/修改操作失败");
                    }
                    break;

                case DELETE:
                    // 调用删除接口：boolean deleteFile(String fileId, String familyId)
                    boolean deleteSuccess = storageAdapter.deleteFile(fileId, familyId);
                    if (deleteSuccess) {
                        context.setStage(FileProcessContext.ProcessingStage.COMPLETED); // 删除通常是最终操作
                        result = ProcessResult.success("文件删除成功");
                    } else {
                        result = ProcessResult.failure("文件删除操作失败");
                    }
                    break;

                case DOWNLOAD:
                    // 调用下载接口：BufferedSource downloadFile(String fileId, String familyId)
                    BufferedSource downloadedSource = storageAdapter.downloadFile(fileId, familyId);
                    if (downloadedSource != null) {
                        context.setBufferedSource(downloadedSource); // 将下载流放入上下文供后续使用
                        context.setStage(FileProcessContext.ProcessingStage.FILE_DOWNLOADED); // 更新阶段
                        result = ProcessResult.success("文件下载成功");
                    } else {
                        result = ProcessResult.failure("文件下载失败，未找到文件或权限不足");
                    }
                    break;

                default:
                    // 对于 VIEW, SHARE 等不涉及实际存储变动的操作，直接跳过
                    result = ProcessResult.success("当前操作类型无需存储操作，跳过");
                    break;
            }
        } catch (Exception e) {
            context.setErrorMessage(e.getMessage());
            context.setStage(FileProcessContext.ProcessingStage.ROLLED_BACK); // 异常时回滚
            result = ProcessResult.failure("存储操作发生异常，原因: " + e.getMessage());
        }
        return result;
    }
    private FileStorageInfo getFileStorageInfo(StorageAdapter storageAdapter, FileProcessContext context) {
        if (context.getOperationType() == FileOperation.UPLOAD|| context.getOperationType() == FileOperation.MODIFY) {//创建存储地址的方法
            context.setFileStorageInfo(createFileStorageInfo(storageAdapter, context));
        }
        return context.getFileStorageInfo();
    }

    /**
     * 根据存储适配器规则创建文件的存储地址信息
     *
     * @param storageAdapter
     * @param context
     * @return
     * @see StorageAdapter
     */
    private FileStorageInfo createFileStorageInfo(StorageAdapter storageAdapter, FileProcessContext context) {
        String familyId = context.getFileBasicMetadata().getFamilyId();
        String bucketName = storageAdapter.buildFamilyBucketName(familyId);
        String fileName = context.getFileBasicMetadata().getFileName();

        // 假设 FileBasicMetadata 中有文件分类信息
        String fileType = context.getFileBasicMetadata().getFileCategory() != null ?
                context.getFileBasicMetadata().getFileCategory().getCategoryName() : null;

        String filePath = storageAdapter.buildFilePath(familyId, fileType);

        // 使用核心构造方法创建 FileStorageInfo
        FileStorageInfo info = new FileStorageInfo(context.getStorageType(), bucketName, fileName, filePath);

        // 可选：将文件ID设置到 info 中（若之前已生成）
        if (context.getFileBasicMetadata().getFileId() != null) {
            info.setFileId(context.getFileBasicMetadata().getFileId());
        }

        return info;
    }
}
