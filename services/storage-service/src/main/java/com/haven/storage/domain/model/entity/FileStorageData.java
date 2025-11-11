package com.haven.storage.domain.model.entity;

import com.haven.storage.domain.model.enums.StorageType;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件存储的相关的数据库映射表的
 * Integer storageType对应的
 *
 * @see StorageType
 */

@Data
public class FileStorageData {
    private Long id;
    private String storageId; // 存储地址唯一标识
    private String fileId; // 关联元数据fileId
    private Integer storageType; // 存储类型（对应枚举code）
    private String familyBucketName;
    private String filePath;
    private String storageConfig; // 数据库存JSON，这里用String接收（或JSONObject）
    private String fullAccessPath;
    private Integer storageStatus; // 0-可用，1-不可用
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
