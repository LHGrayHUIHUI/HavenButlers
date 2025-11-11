package com.haven.storage.domain.model.enums;

import lombok.Getter;

/**
 * 存储类型枚举（支持扩展）
 */
@Getter
public enum StorageType {
    LOCAL("本地存储", 0),
    MINIO("MinIO存储", 1),
    CLOUD_S3("AWS S3存储", 2),
    CLOUD_OSS("阿里云OSS存储", 3),
    CLOUD_COS("腾讯云COS存储", 4),
    OTHER("其他存储", 5);

    private final String desc;
    private final int minvalue;

    StorageType(String desc, int minvalue) {
        this.desc = desc;
        this.minvalue = minvalue;
    }

    public static StorageType getByCode(int minvalue) {
        for (StorageType type : values()) {
            if (type.minvalue == minvalue) {
                return type;
            }
        }
        return null;
    }


}