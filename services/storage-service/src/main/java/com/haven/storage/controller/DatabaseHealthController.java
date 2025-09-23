package com.haven.storage.controller;

import com.haven.base.common.response.ResponseWrapper;
import com.haven.storage.service.DatabaseHealthService;
import com.haven.storage.service.DatabaseHealthService.DatabaseStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据库健康状态控制器
 * 提供数据库和存储系统的健康状态查询接口
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/storage/health")
@RequiredArgsConstructor
public class DatabaseHealthController {

    private final DatabaseHealthService databaseHealthService;

    /**
     * 获取所有数据库状态
     */
    @GetMapping("/databases")
    public ResponseWrapper<List<DatabaseStatus>> getAllDatabaseStatus() {
        try {
            List<DatabaseStatus> statusList = databaseHealthService.getAllDatabaseStatus();
            log.info("查询数据库状态，总计{}个数据库", statusList.size());
            return ResponseWrapper.success(statusList);
        } catch (Exception e) {
            log.error("获取数据库状态失败", e);
            return ResponseWrapper.error(500, "获取数据库状态失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取指定数据库状态
     */
    @GetMapping("/databases/{name}")
    public ResponseWrapper<DatabaseStatus> getDatabaseStatus(@PathVariable String name) {
        try {
            DatabaseStatus status = databaseHealthService.getDatabaseStatus(name);
            if (status == null) {
                return ResponseWrapper.error(404, "未找到名称为 " + name + " 的数据库", null);
            }
            return ResponseWrapper.success(status);
        } catch (Exception e) {
            log.error("获取数据库{}状态失败", name, e);
            return ResponseWrapper.error(500, "获取状态失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取数据库健康状态摘要
     */
    @GetMapping("/summary")
    public ResponseWrapper<Map<String, Object>> getHealthSummary() {
        try {
            Map<String, Object> summary = databaseHealthService.getHealthSummary();
            return ResponseWrapper.success(summary);
        } catch (Exception e) {
            log.error("获取健康摘要失败", e);
            return ResponseWrapper.error(500, "获取摘要失败: " + e.getMessage(), null);
        }
    }

    /**
     * 手动触发健康检查
     */
    @PostMapping("/check")
    public ResponseWrapper<String> triggerHealthCheck() {
        try {
            log.info("手动触发数据库健康检查");
            databaseHealthService.checkDatabaseHealth();
            return ResponseWrapper.success("健康检查已触发");
        } catch (Exception e) {
            log.error("触发健康检查失败", e);
            return ResponseWrapper.error(500, "触发健康检查失败: " + e.getMessage(), null);
        }
    }

    /**
     * 获取数据库连接信息
     * 仅返回配置的连接信息，不包含敏感信息
     */
    @GetMapping("/connections")
    public ResponseWrapper<Map<String, Map<String, Object>>> getConnectionInfo() {
        Map<String, Map<String, Object>> connections = Map.of(
            "PostgreSQL", Map.of(
                "host", "postgres",
                "port", 5432,
                "database", "smarthome",
                "description", "关系型数据库，用于结构化数据存储"
            ),
            "MongoDB", Map.of(
                "host", "mongodb",
                "port", 27017,
                "database", "smarthome",
                "description", "NoSQL数据库，用于文档和日志存储"
            ),
            "Redis", Map.of(
                "host", "redis",
                "port", 6379,
                "description", "缓存数据库，用于会话和热数据缓存"
            ),
            "MinIO", Map.of(
                "endpoint", "http://minio:9000",
                "console", "http://minio:9001",
                "bucket", "smarthome",
                "description", "对象存储，用于文件和媒体存储"
            )
        );

        return ResponseWrapper.success(connections);
    }
}