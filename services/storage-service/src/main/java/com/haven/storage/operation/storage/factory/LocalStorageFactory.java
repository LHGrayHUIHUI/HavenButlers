package com.haven.storage.operation.storage.factory;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.operation.storage.StorageOperationFactory;
import com.haven.storage.operation.storage.adapter.LocalStorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


/**
 * 存储的抽象工厂类
 */
@Component
@RequiredArgsConstructor
public class LocalStorageFactory implements StorageOperationFactory {
    private final LocalStorageAdapter localStorageAdapter;

    @Override
    public StorageAdapter createStorageOperation() {
        return localStorageAdapter;
    }

    @Override
    public StorageType getSupportStorageType() {
        return localStorageAdapter.getStorageType();
    }
}
