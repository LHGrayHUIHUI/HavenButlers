package com.haven.admin.controller;

import com.haven.admin.model.ServiceInfo;
import com.haven.admin.model.ServiceMetrics;
import com.haven.admin.service.ServiceManageService;
import com.haven.admin.service.AdminNacosServiceManager;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.model.dto.PageRequest;
import com.haven.base.model.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
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
    private final AdminNacosServiceManager adminNacosServiceManager;

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

    // =============== Nacos专用API ===============

    /**
     * 获取Nacos中的所有服务名称
     */
    @GetMapping("/nacos/services")
    public ResponseWrapper<List<String>> getNacosServices() {
        List<String> services = adminNacosServiceManager.getAllServiceNames();
        return ResponseWrapper.success(services);
    }

    /**
     * 获取指定服务的所有实例（Nacos）
     */
    @GetMapping("/nacos/{serviceName}/instances")
    public ResponseWrapper<List<ServiceInstance>> getServiceInstances(@PathVariable String serviceName) {
        List<ServiceInstance> instances = adminNacosServiceManager.getServiceInstances(serviceName);
        return ResponseWrapper.success(instances);
    }

    /**
     * 获取服务详细信息（包含Nacos特有信息）
     */
    @GetMapping("/nacos/{serviceName}/details")
    public ResponseWrapper<Map<String, Object>> getNacosServiceDetails(@PathVariable String serviceName) {
        Map<String, Object> details = adminNacosServiceManager.getServiceDetails(serviceName);
        return ResponseWrapper.success(details);
    }

    /**
     * 获取Nacos服务健康状态
     */
    @GetMapping("/nacos/{serviceName}/health")
    public ResponseWrapper<Map<String, Object>> getNacosServiceHealth(@PathVariable String serviceName) {
        Map<String, Object> health = adminNacosServiceManager.getServiceHealth(serviceName);
        return ResponseWrapper.success(health);
    }

    /**
     * 获取系统整体健康状态
     */
    @GetMapping("/nacos/system/health")
    public ResponseWrapper<Map<String, Object>> getSystemHealth() {
        Map<String, Object> systemHealth = adminNacosServiceManager.getSystemHealth();
        return ResponseWrapper.success(systemHealth);
    }

    /**
     * 临时下线服务实例（维护模式）
     */
    @PostMapping("/nacos/{serviceName}/deregister")
    public ResponseWrapper<Boolean> deregisterInstance(
            @PathVariable String serviceName,
            @RequestParam String ip,
            @RequestParam int port) {
        boolean result = adminNacosServiceManager.deregisterInstance(serviceName, ip, port);
        if (result) {
            log.info("临时下线服务实例成功: {}:{}:{}", serviceName, ip, port);
        } else {
            log.error("临时下线服务实例失败: {}:{}:{}", serviceName, ip, port);
        }
        return ResponseWrapper.success(result);
    }

    /**
     * 重新上线服务实例
     */
    @PostMapping("/nacos/{serviceName}/register")
    public ResponseWrapper<Boolean> registerInstance(
            @PathVariable String serviceName,
            @RequestParam String ip,
            @RequestParam int port) {
        boolean result = adminNacosServiceManager.registerInstance(serviceName, ip, port);
        if (result) {
            log.info("重新上线服务实例成功: {}:{}:{}", serviceName, ip, port);
        } else {
            log.error("重新上线服务实例失败: {}:{}:{}", serviceName, ip, port);
        }
        return ResponseWrapper.success(result);
    }
}