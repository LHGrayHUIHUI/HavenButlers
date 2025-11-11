package com.haven.storage.strategy.storage.factory;



import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.strategy.storage.StorageNamingStrategy;
import com.haven.storage.strategy.storage.impl.LocalStorageNamingStrategy;
import com.haven.storage.strategy.storage.impl.MinIOStorageNamingStrategy;
import com.haven.storage.strategy.storage.impl.S3StorageNamingStrategy;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储命名策略工厂：负责策略的创建和管理
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class StorageNamingStrategyFactory {

    // 缓存策略实例（单例）
    private static final Map<StorageType, StorageNamingStrategy> STRATEGY_MAP = new HashMap<>();

    // 初始化策略映射
    static {
        STRATEGY_MAP.put(StorageType.LOCAL, new LocalStorageNamingStrategy());
        STRATEGY_MAP.put(StorageType.MINIO, new MinIOStorageNamingStrategy());
        STRATEGY_MAP.put(StorageType.CLOUD_S3, new S3StorageNamingStrategy());
        // 新增存储类型时，只需在此处注册新策略
    }

    /**
     * 根据存储类型获取策略
     */
    public static StorageNamingStrategy getStrategy(StorageType storageType) {
        StorageNamingStrategy strategy = STRATEGY_MAP.get(storageType);
        if (strategy == null) {
            throw new IllegalArgumentException("不支持的存储类型：" + storageType);
        }
        return strategy;
    }
}
