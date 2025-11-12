package com.haven.storage.operation.storage;

import com.haven.storage.domain.model.enums.StorageType;

public interface StorageOperationFactory {
    StorageAdapter createStorageOperation();
    StorageType getSupportStorageType();
}
