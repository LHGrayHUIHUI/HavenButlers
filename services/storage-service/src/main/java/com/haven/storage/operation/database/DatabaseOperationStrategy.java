package com.haven.storage.operation.database;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.processor.context.FileProcessContext;

public interface DatabaseOperationStrategy {
    /**
     * 执行具体的数据库操作
     * @param context 文件处理上下文
     * @return 操作结果
     */
    ProcessResult execute(FileProcessContext context);

    /**
     * 获取当前策略支持的文件操作类型
     */
    FileOperation getSupportOperation();
}
