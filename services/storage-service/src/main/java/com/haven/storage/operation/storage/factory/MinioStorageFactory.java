package com.haven.storage.operation.storage.factory;

import com.haven.storage.domain.model.enums.StorageType;
import com.haven.storage.operation.storage.StorageAdapter;
import com.haven.storage.operation.storage.StorageOperationFactory;
import com.haven.storage.operation.storage.adapter.MinioStorageAdapter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class MinioStorageFactory implements StorageOperationFactory {
    private final MinioStorageAdapter minioStorageAdapter;

    @Override
    public StorageAdapter createStorageOperation() {
        return minioStorageAdapter;
    }

    @Override
    public StorageType getSupportStorageType() {
        return minioStorageAdapter.getStorageType();
    }
}
