package com.haven.storage.controller;

import com.haven.base.annotation.TraceLog;
import com.haven.base.common.response.ResponseWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 *
 * @author HavenButler
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "健康检查", description = "服务健康状态检查接口")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查存储服务的健康状态")
    @TraceLog(value = "健康检查", module = "health", type = "HEALTH_CHECK")
    public ResponseWrapper<Map<String, Object>> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("serviceName", "storage-service");
        healthData.put("status", "UP");
        healthData.put("version", "1.0.0");
        healthData.put("timestamp", System.currentTimeMillis());

        return ResponseWrapper.success("服务健康", healthData);
    }

    @GetMapping("/info")
    @Operation(summary = "服务信息", description = "获取存储服务的基本信息")
    @TraceLog(value = "服务信息", module = "health", type = "SERVICE_INFO")
    public ResponseWrapper<Map<String, Object>> info() {
        Map<String, Object> serviceInfo = new HashMap<>();
        serviceInfo.put("service", "storage-service");
        serviceInfo.put("description", "HavenButler存储服务");
        serviceInfo.put("version", "1.0.0");

        return ResponseWrapper.success("服务信息", serviceInfo);
    }
}