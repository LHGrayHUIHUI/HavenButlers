package com.haven.admin.controller;

import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.admin.service.ServiceManageService;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.model.dto.PageRequest;
import com.haven.base.model.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class ServiceManageController {

    private final ServiceManageService serviceManageService;

    /**
     * 获取所有服务列表
     */
    @GetMapping("/list")
    public ResponseWrapper<List<ServiceInfo>> getServiceList() {
        List<ServiceInfo> services = serviceManageService.getAllServices();
        return ResponseWrapper.success(services);
    }

    /**
     * 获取服务详情
     */
    @GetMapping("/{serviceName}")
    public ResponseWrapper<ServiceInfo> getServiceDetail(@PathVariable String serviceName) {
        ServiceInfo serviceInfo = serviceManageService.getServiceDetail(serviceName);
        return ResponseWrapper.success(serviceInfo);
    }

    /**
     * 获取服务健康状态
     */
    @GetMapping("/{serviceName}/health")
    public ResponseWrapper<Map<String, Object>> getServiceHealth(@PathVariable String serviceName) {
        Map<String, Object> health = serviceManageService.getServiceHealth(serviceName);
        return ResponseWrapper.success(health);
    }

    /**
     * 获取服务指标
     */
    @GetMapping("/{serviceName}/metrics")
    public ResponseWrapper<ServiceMetrics> getServiceMetrics(
            @PathVariable String serviceName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        ServiceMetrics metrics = serviceManageService.getServiceMetrics(serviceName, startTime, endTime);
        return ResponseWrapper.success(metrics);
    }

    /**
     * 重启服务
     */
    @PostMapping("/{serviceName}/restart")
    public ResponseWrapper<Void> restartService(@PathVariable String serviceName) {
        serviceManageService.restartService(serviceName);
        log.info("重启服务: {}", serviceName);
        return ResponseWrapper.success();
    }

    /**
     * 停止服务
     */
    @PostMapping("/{serviceName}/stop")
    public ResponseWrapper<Void> stopService(@PathVariable String serviceName) {
        serviceManageService.stopService(serviceName);
        log.info("停止服务: {}", serviceName);
        return ResponseWrapper.success();
    }

    /**
     * 启动服务
     */
    @PostMapping("/{serviceName}/start")
    public ResponseWrapper<Void> startService(@PathVariable String serviceName) {
        serviceManageService.startService(serviceName);
        log.info("启动服务: {}", serviceName);
        return ResponseWrapper.success();
    }

    /**
     * 获取服务日志
     */
    @GetMapping("/{serviceName}/logs")
    public ResponseWrapper<PageResponse<String>> getServiceLogs(
            @PathVariable String serviceName,
            @RequestParam(defaultValue = "INFO") String level,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "100") Integer size) {
        PageRequest pageRequest = new PageRequest();
        pageRequest.setPage(page);
        pageRequest.setSize(size);
        PageResponse<String> logs = serviceManageService.getServiceLogs(serviceName, level, pageRequest);
        return ResponseWrapper.success(logs);
    }

    /**
     * 更新服务配置
     */
    @PutMapping("/{serviceName}/config")
    public ResponseWrapper<Void> updateServiceConfig(
            @PathVariable String serviceName,
            @RequestBody Map<String, Object> config) {
        serviceManageService.updateServiceConfig(serviceName, config);
        log.info("更新服务配置: {}, 配置: {}", serviceName, config);
        return ResponseWrapper.success();
    }

    /**
     * 获取服务依赖关系
     */
    @GetMapping("/dependencies")
    public ResponseWrapper<Map<String, List<String>>> getServiceDependencies() {
        Map<String, List<String>> dependencies = serviceManageService.getServiceDependencies();
        return ResponseWrapper.success(dependencies);
    }

    /**
     * 服务健康检查
     */
    @PostMapping("/health-check")
    public ResponseWrapper<Map<String, String>> healthCheck() {
        Map<String, String> result = serviceManageService.performHealthCheck();
        return ResponseWrapper.success(result);
    }
}