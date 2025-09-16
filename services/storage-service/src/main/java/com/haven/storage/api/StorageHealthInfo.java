package com.haven.storage.api;

import lombok.Data;

/**
 * 存储服务健康检查信息
 */
@Data
public class StorageHealthInfo {
    private String status;
    private String serviceName;
    private String version;
    private Long timestamp;
}