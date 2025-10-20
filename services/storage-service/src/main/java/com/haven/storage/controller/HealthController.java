package com.haven.storage.controller;

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
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> healthData = new HashMap<>();
        healthData.put("serviceName", "storage-service");
        healthData.put("status", "UP");
        healthData.put("version", "1.0.0");
        healthData.put("timestamp", System.currentTimeMillis());

        Map<String, Object> response = new HashMap<>();
        response.put("code", 0);
        response.put("message", "服务健康");
        response.put("data", healthData);
        response.put("success", true);
        response.put("timestamp", System.currentTimeMillis());
        return response;
    }

    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "storage-service");
        response.put("description", "HavenButler存储服务");
        response.put("version", "1.0.0");
        return response;
    }
}