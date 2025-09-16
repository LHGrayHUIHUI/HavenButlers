package com.haven.storage.database;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 数据库健康状态
 */
@Data
public class DatabaseHealthStatus {
    private String projectId;
    private String familyId;
    private boolean healthy;
    private String message;
    private long responseTimeMs;
    private LocalDateTime checkTime;
    private String traceId;
}