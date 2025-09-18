package com.haven.admin.controller;

import com.haven.admin.service.AdminHealthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简化的管理控制器
 * 提供基础的管理功能
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    @Autowired
    private AdminHealthService adminHealthService;

    /**
     * 获取系统健康状态
     */
    @GetMapping("/health")
    public Map<String, Object> getHealth() {
        log.info("获取系统健康状态");
        return adminHealthService.getSystemHealth();
    }

    /**
     * 获取系统指标
     */
    @GetMapping("/metrics")
    public Map<String, Object> getMetrics() {
        log.info("获取系统指标");
        return adminHealthService.getSystemMetrics();
    }

    /**
     * 获取服务状态
     */
    @GetMapping("/services")
    public Map<String, Object> getServices() {
        log.info("获取服务状态");
        return adminHealthService.getServicesStatus();
    }
}