package com.haven.storage.operation.database;

import com.haven.storage.domain.model.enums.FileOperation;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class DatabaseStrategyRegistry {
    private final Map<FileOperation, DatabaseOperationStrategy> strategyMap;

    // Spring 自动注入所有 DatabaseOperationStrategy 接口的实现类
    public DatabaseStrategyRegistry(List<DatabaseOperationStrategy> strategies) {
        this.strategyMap = new EnumMap<>(FileOperation.class);
        for (DatabaseOperationStrategy strategy : strategies) {
            // 使用策略支持的操作类型作为 Key
            strategyMap.put(strategy.getSupportOperation(), strategy);
        }
    }

    public DatabaseOperationStrategy getStrategy(FileOperation operation) {
        // 如果找不到策略，对于某些操作（如VIEW, SHARE），可以返回一个空操作策略
        DatabaseOperationStrategy strategy = strategyMap.get(operation);
        if (strategy == null) {
            // 可以返回一个默认的 NoOpStrategy (不执行任何操作)
            return new NoOpDatabaseStrategy();
        }
        return strategy;
    }
}
