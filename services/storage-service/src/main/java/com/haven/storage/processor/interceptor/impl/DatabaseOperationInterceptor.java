package com.haven.storage.processor.interceptor.impl;

import com.haven.storage.domain.model.enums.FileOperation;
import com.haven.storage.domain.model.file.ProcessResult;
import com.haven.storage.operation.database.DatabaseOperationStrategy;
import com.haven.storage.operation.database.DatabaseStrategyRegistry;
import com.haven.storage.processor.context.FileProcessContext;
import com.haven.storage.processor.interceptor.FileInterceptorChain;
import com.haven.storage.processor.interceptor.FileProcessInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Set;
@Component
@Order(60) // 确保在文件存储操作之后执行
@RequiredArgsConstructor // 使用构造函数注入
public class DatabaseOperationInterceptor implements FileProcessInterceptor {
    private final DatabaseStrategyRegistry registry;

    @Override
    public ProcessResult intercept(FileProcessContext context, FileInterceptorChain chain) {

        FileOperation operation = context.getOperationType();

        // 1. 从注册表获取对应操作的策略
        DatabaseOperationStrategy strategy = registry.getStrategy(operation);

        // 2. 执行策略
        ProcessResult result = strategy.execute(context);

        // 3. 如果策略执行失败，则中断责任链
        if (!result.isSuccess()) {
            return result;
        }

        // 4. 数据库操作成功，继续下一个拦截器
        return chain.proceed(context);
    }

    @Override
    public Set<FileOperation> supportedOperations() {
        return FileProcessInterceptor.super.supportedOperations();
    }
}
