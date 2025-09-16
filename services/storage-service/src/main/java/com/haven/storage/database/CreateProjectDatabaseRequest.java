package com.haven.storage.database;

import lombok.Data;

/**
 * 创建项目数据库请求
 */
@Data
public class CreateProjectDatabaseRequest {
    private String projectId;
    private String familyId;
    private String projectName;
    private String databaseType;
    private String creatorUserId;
}