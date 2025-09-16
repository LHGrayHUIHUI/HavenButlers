package com.haven.storage.database;

import com.haven.base.model.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据库连接信息实体
 * 继承BaseEntity获得通用字段和方法
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DatabaseConnectionInfo extends BaseEntity {
    private String projectId;
    private String familyId;
    private String projectName;
    private String databaseType;
    private String jdbcUrl;
    private String username;
    private String password;
    private String driverClassName;
    private Integer maxConnections;
    private Integer connectionTimeout;
}