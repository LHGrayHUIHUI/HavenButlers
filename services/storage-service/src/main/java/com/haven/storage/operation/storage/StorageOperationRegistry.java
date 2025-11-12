package com.haven.storage.operation.storage;


import com.haven.storage.domain.model.enums.StorageType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 存储命名策略工厂：负责策略的创建和管理
 */
@Component
public class StorageOperationRegistry {
    private final Map<StorageType, StorageOperationFactory> factoryMap;

    // Spring 会自动注入所有实现了 StorageOperationFactory 接口的 Bean
    public StorageOperationRegistry(List<StorageOperationFactory> factories) {
        this.factoryMap = new EnumMap<>(StorageType.class);
        for (StorageOperationFactory factory : factories) {
            // 使用工厂支持的类型作为 Key
            factoryMap.put(factory.getSupportStorageType(), factory);
        }
    }

    public StorageAdapter getStorageAdapter(StorageType storageType) {
        StorageOperationFactory factory = factoryMap.get(storageType);
        if (factory == null) {
            throw new UnsupportedOperationException("Unsupported storage type: " + storageType);
        }
        // 使用工厂创建并返回适配器
        return factory.createStorageOperation();
    }
}
