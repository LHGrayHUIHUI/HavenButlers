package com.haven.storage.operation.storage.factory;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.operation.storage.StorageOperationFactory;
import com.haven.storage.operation.storage.adapter.OssStorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OssStorageFactory implements StorageOperationFactory {
    private final OssStorageAdapter ossStorageAdapter;
    @Override
    public StorageAdapter createStorageOperation() {
        return ossStorageAdapter;
    }

    @Override
    public StorageType getSupportStorageType() {
        return ossStorageAdapter.getStorageType();
    }
}
