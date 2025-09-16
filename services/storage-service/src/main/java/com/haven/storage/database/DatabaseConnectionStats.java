package com.haven.storage.database;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据库连接统计信息
 */
@Data
public class DatabaseConnectionStats {
    private String projectId;
    private int totalConnections;
    private int activeConnections;
    private LocalDateTime createdAt;
    private LocalDateTime lastAccessTime;
}