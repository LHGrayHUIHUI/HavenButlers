package com.haven.storage.database;

import lombok.Data;

/**
 * 项目数据库配置
 */
@Data
public class ProjectDatabaseConfig {
    private String projectId;
    private String databaseName;
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String driverClassName;
    private Integer maxConnections;
    private Integer connectionTimeout;
}