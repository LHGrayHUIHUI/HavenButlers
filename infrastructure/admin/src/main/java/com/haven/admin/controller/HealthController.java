package com.haven.admin.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haven.admin.model.ServiceOverview;
import com.haven.admin.service.HealthSnapshotService;
import com.haven.admin.service.SimpleCacheService;
import com.haven.admin.common.AdminResponse;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 健康监控控制器
 *
 * 提供服务健康状态的快速查询接口
 * 支持实时流式更新（SSE）
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/service")
@RequiredArgsConstructor
public class HealthController {

    private final HealthSnapshotService healthSnapshotService;
    private final SimpleCacheService cacheService;
    private final ObjectMapper objectMapper;

    /**
     * SSE连接管理
     */
    private final ConcurrentHashMap<String, SseEmitter> sseConnections = new ConcurrentHashMap<>();
    private final ScheduledExecutorService sseExecutor = Executors.newScheduledThreadPool(2);

    /**
     * 获取所有服务健康概览
     *
     * @param status 可选的状态过滤器（UP, DEGRADED, DOWN）
     * @param search 可选的服务名搜索关键词
     * @return 服务健康概览列表
     */
    @GetMapping("/overview")
    public AdminResponse<List<ServiceOverview>> getServiceOverview(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String search) {

        try {
            // 为过滤查询添加缓存（缓存3秒，减少重复计算）
            String cacheKey = String.format("overview_filtered_%s_%s",
                    status != null ? status : "all",
                    search != null ? search : "all");

            List<ServiceOverview> filteredOverviews = cacheService.computeIfAbsent(
                    cacheKey,
                    3,
                    () -> {
                        List<ServiceOverview> overviews = healthSnapshotService.getAllServiceOverview();

                        return overviews.stream()
                                .filter(overview -> {
                                    // 状态过滤
                                    if (status != null && !status.isEmpty()) {
                                        if (!status.equalsIgnoreCase(overview.getStatus())) {
                                            return false;
                                        }
                                    }
                                    // 名称搜索过滤
                                    if (search != null && !search.isEmpty()) {
                                        if (!overview.getServiceName().toLowerCase()
                                                .contains(search.toLowerCase())) {
                                            return false;
                                        }
                                    }
                                    return true;
                                })
                                .sorted((a, b) -> {
                                    // 排序：DOWN > DEGRADED > UP，同状态按名称排序
                                    int statusOrder = getStatusOrder(a.getStatus()) - getStatusOrder(b.getStatus());
                                    if (statusOrder != 0) {
                                        return statusOrder;
                                    }
                                    return a.getServiceName().compareTo(b.getServiceName());
                                })
                                .collect(Collectors.toList());
                    }
            );

            log.debug("返回过滤后的健康概览，条目数: {}", filteredOverviews.size());
            return AdminResponse.success(filteredOverviews);

        } catch (Exception e) {
            log.error("获取服务健康概览失败", e);
            return AdminResponse.error(500, "获取服务健康状态失败: " + e.getMessage(), (List<ServiceOverview>) null);
        }
    }

    /**
     * 获取单个服务的健康概览
     *
     * @param serviceName 服务名称
     * @return 服务健康概览
     */
    @GetMapping("/overview/{serviceName}")
    public AdminResponse<ServiceOverview> getServiceOverview(@PathVariable String serviceName) {
        try {
            ServiceOverview overview = healthSnapshotService.getServiceOverview(serviceName);
            if (overview == null) {
                return AdminResponse.error(404, "服务不存在或尚未收集健康数据: " + serviceName, (ServiceOverview) null);
            }

            return AdminResponse.success(overview);
        } catch (Exception e) {
            log.error("获取服务 {} 健康概览失败", serviceName, e);
            return AdminResponse.error(500, "获取服务健康状态失败: " + e.getMessage(), (ServiceOverview) null);
        }
    }

    /**
     * SSE流式健康状态更新
     *
     * 客户端可以通过此接口接收实时的健康状态更新，避免频繁轮询
     *
     * @return SSE事件流
     */
    @GetMapping(value = "/stream/health", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamHealthUpdates() {
        String connectionId = "health_" + System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(300000L); // 5分钟超时

        // 设置连接事件处理
        emitter.onCompletion(() -> {
            sseConnections.remove(connectionId);
            log.debug("SSE连接完成: {}", connectionId);
        });

        emitter.onTimeout(() -> {
            sseConnections.remove(connectionId);
            log.debug("SSE连接超时: {}", connectionId);
        });

        emitter.onError((throwable) -> {
            sseConnections.remove(connectionId);
            log.warn("SSE连接错误: {}", connectionId, throwable);
        });

        // 保存连接
        sseConnections.put(connectionId, emitter);

        // 立即发送当前状态
        try {
            List<ServiceOverview> currentOverviews = healthSnapshotService.getAllServiceOverview();
            String data = objectMapper.writeValueAsString(currentOverviews);
            emitter.send(SseEmitter.event()
                    .name("health_update")
                    .data(data)
                    .id(String.valueOf(System.currentTimeMillis())));

            log.debug("建立SSE连接: {}，发送初始数据", connectionId);
        } catch (Exception e) {
            log.error("发送SSE初始数据失败", e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ignored) {
            }
            sseConnections.remove(connectionId);
        }

        // 如果这是第一个连接，启动定时推送
        if (sseConnections.size() == 1) {
            startSseHealthUpdates();
        }

        return emitter;
    }

    /**
     * 启动SSE健康状态定时推送
     */
    private final java.util.concurrent.atomic.AtomicBoolean sseSchedulerStarted = new java.util.concurrent.atomic.AtomicBoolean(false);

    private void startSseHealthUpdates() {
        if (!sseSchedulerStarted.compareAndSet(false, true)) {
            return; // 已启动过调度器
        }
        sseExecutor.scheduleWithFixedDelay(() -> {
            if (sseConnections.isEmpty()) {
                return; // 没有活跃连接，跳过推送
            }

            try {
                List<ServiceOverview> overviews = healthSnapshotService.getAllServiceOverview();
                String data = objectMapper.writeValueAsString(overviews);

                // 向所有连接推送数据
                sseConnections.forEach((connectionId, emitter) -> {
                    try {
                        emitter.send(SseEmitter.event()
                                .name("health_update")
                                .data(data)
                                .id(String.valueOf(System.currentTimeMillis())));
                    } catch (IOException e) {
                        log.debug("SSE推送失败，移除连接: {}", connectionId);
                        sseConnections.remove(connectionId);
                        try {
                            emitter.complete();
                        } catch (Exception ignored) {
                        }
                    }
                });

                log.debug("SSE健康状态推送完成，活跃连接数: {}", sseConnections.size());
            } catch (Exception e) {
                log.error("SSE健康状态推送失败", e);
            }
        }, 5, 5, TimeUnit.SECONDS); // 每5秒推送一次
    }

    /**
     * 获取SSE连接统计
     */
    @GetMapping("/stream/stats")
    public AdminResponse<SseStats> getSseStats() {
        SseStats stats = new SseStats();
        stats.setActiveConnections(sseConnections.size());
        return AdminResponse.success(stats);
    }

    /**
     * 获取状态排序权重
     */
    private int getStatusOrder(String status) {
        switch (status) {
            case "DOWN":
                return 0; // 最高优先级
            case "DEGRADED":
                return 1;
            case "UP":
                return 2; // 最低优先级
            default:
                return 3;
        }
    }

    /**
     * 优雅关闭 SSE 资源
     *
     * 在应用关闭时：
     * 1. 关闭所有活跃的SSE连接
     * 2. 优雅关闭线程池，等待任务完成
     */
    @PreDestroy
    public void shutdown() {
        log.info("正在关闭 SSE 服务，活跃连接数: {}", sseConnections.size());

        // 1. 关闭所有 SSE 连接
        sseConnections.forEach((connectionId, emitter) -> {
            try {
                emitter.complete();
                log.debug("SSE 连接已关闭: {}", connectionId);
            } catch (Exception e) {
                log.warn("关闭 SSE 连接失败: {}", connectionId, e);
            }
        });
        sseConnections.clear();

        // 2. 优雅关闭线程池
        sseExecutor.shutdown();
        try {
            if (!sseExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                log.warn("SSE 线程池未能在 10 秒内完成，强制关闭");
                sseExecutor.shutdownNow();

                // 等待强制关闭完成
                if (!sseExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.error("SSE 线程池强制关闭失败");
                }
            } else {
                log.info("SSE 线程池已优雅关闭");
            }
        } catch (InterruptedException e) {
            log.error("SSE 线程池关闭被中断", e);
            sseExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * SSE连接统计
     */
    public static class SseStats {
        private int activeConnections;

        public int getActiveConnections() {
            return activeConnections;
        }

        public void setActiveConnections(int activeConnections) {
            this.activeConnections = activeConnections;
        }
    }
}
