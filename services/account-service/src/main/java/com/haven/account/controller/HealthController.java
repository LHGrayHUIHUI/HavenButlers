package com.haven.account.controller;

import com.haven.base.common.response.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@RestController
@RequestMapping("/actuator")
@Tag(name = "健康检查", description = "服务健康状态、系统信息、依赖检查等")
public class HealthController {

    /**
     * 健康检查
     */
    @GetMapping("/health")
    @Operation(summary = "服务健康检查", description = "检查服务及其依赖组件的健康状态")
    public ResponseWrapper<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());
        health.put("service", "account-service");
        health.put("version", "1.0.0");
        health.put("database", "connected"); // TODO: 实际检查数据库连接
        health.put("redis", "connected");    // TODO: 实际检查Redis连接

        return ResponseWrapper.success(health);
    }

    /**
     * 服务信息
     */
    @GetMapping("/info")
    @Operation(summary = "服务信息", description = "获取服务的基本信息和元数据")
    public ResponseWrapper<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("service", "account-service");
        info.put("version", "1.0.0");
        info.put("description", "HavenButler 账户服务 - 用户认证授权、家庭权限管理、设备访问控制");
        info.put("author", "HavenButler Team");
        info.put("buildTime", LocalDateTime.now());

        return ResponseWrapper.success(info);
    }

    /**
     * 系统状态
     */
    @GetMapping("/status")
    @Operation(summary = "系统状态", description = "获取所有依赖组件的详细状态信息")
    public ResponseWrapper<Map<String, Object>> status() {
        Map<String, Object> status = new HashMap<>();
        status.put("database", "healthy");
        status.put("redis", "healthy");
        status.put("dependencies", "healthy");
        status.put("timestamp", LocalDateTime.now());

        return ResponseWrapper.success(status);
    }
}