package com.haven.admin.controller;

import com.haven.admin.model.LogEntry;
import com.haven.admin.model.PageRequest;
import com.haven.admin.model.PageResponse;
import com.haven.admin.model.Result;
import com.haven.admin.service.LogAggregationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 * 提供日志查询、统计和聚合功能
 *
 * @author HavenButler
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@Tag(name = "日志管理", description = "日志查询、统计和聚合相关接口")
public class LogController {

    @Autowired
    private LogAggregationService logAggregationService;

    /**
     * 搜索日志
     */
    @GetMapping("/search")
    @Operation(summary = "搜索日志", description = "根据条件搜索各服务的日志")
    public ResponseEntity<Result<PageResponse<LogEntry>>> searchLogs(
            @Parameter(description = "服务名称") @RequestParam(required = false) String serviceName,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "链路追踪ID") @RequestParam(required = false) String traceId,
            @Parameter(description = "关键字") @RequestParam(required = false) String keyword,
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {

        try {
            PageRequest pageRequest = PageRequest.builder()
                    .page(page)
                    .size(size)
                    .build();

            PageResponse<LogEntry> logs = logAggregationService.searchLogs(
                    serviceName, level, traceId, keyword, startTime, endTime, pageRequest);

            return ResponseEntity.ok(Result.success(logs));

        } catch (Exception e) {
            log.error("搜索日志失败", e);
            return ResponseEntity.ok(Result.failure("搜索日志失败: " + e.getMessage()));
        }
    }

    /**
     * 根据traceId查询日志
     */
    @GetMapping("/trace/{traceId}")
    @Operation(summary = "根据traceId查询日志", description = "查询指定链路追踪ID的所有日志")
    public ResponseEntity<Result<List<LogEntry>>> getLogsByTraceId(
            @Parameter(description = "链路追踪ID") @PathVariable String traceId) {

        try {
            List<LogEntry> logs = logAggregationService.getLogsByTraceId(traceId);
            return ResponseEntity.ok(Result.success(logs));

        } catch (Exception e) {
            log.error("根据traceId查询日志失败: traceId={}", traceId, e);
            return ResponseEntity.ok(Result.failure("查询日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取日志统计信息
     */
    @GetMapping("/statistics")
    @Operation(summary = "获取日志统计信息", description = "获取指定时间范围内的日志统计")
    public ResponseEntity<Result<Map<String, Object>>> getLogStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {

        try {
            Map<String, Object> statistics = logAggregationService.getLogStatistics(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));

        } catch (Exception e) {
            log.error("获取日志统计信息失败", e);
            return ResponseEntity.ok(Result.failure("获取统计信息失败: " + e.getMessage()));
        }
    }

    /**
     * 手动添加日志条目
     */
    @PostMapping("/add")
    @Operation(summary = "添加日志条目", description = "手动添加一条日志记录")
    public ResponseEntity<Result<String>> addLogEntry(@RequestBody LogEntry logEntry) {
        try {
            logAggregationService.addLogEntry(logEntry);
            return ResponseEntity.ok(Result.success("日志添加成功"));

        } catch (Exception e) {
            log.error("添加日志失败", e);
            return ResponseEntity.ok(Result.failure("添加日志失败: " + e.getMessage()));
        }
    }

    /**
     * 获取服务日志概览
     */
    @GetMapping("/overview")
    @Operation(summary = "获取日志概览", description = "获取所有服务的日志数量概览")
    public ResponseEntity<Result<Map<String, Object>>> getLogOverview() {
        try {
            Map<String, Object> statistics = logAggregationService.getLogStatistics(null, null);
            return ResponseEntity.ok(Result.success(statistics));

        } catch (Exception e) {
            log.error("获取日志概览失败", e);
            return ResponseEntity.ok(Result.failure("获取概览失败: " + e.getMessage()));
        }
    }

    /**
     * 获取日志级别分布
     */
    @GetMapping("/levels")
    @Operation(summary = "获取日志级别分布", description = "获取各服务不同级别日志的分布情况")
    public ResponseEntity<Result<Map<String, Object>>> getLogLevelDistribution(
            @Parameter(description = "服务名称") @RequestParam(required = false) String serviceName,
            @Parameter(description = "时间范围（小时）") @RequestParam(defaultValue = "24") int hours) {

        try {
            LocalDateTime endTime = LocalDateTime.now();
            LocalDateTime startTime = endTime.minusHours(hours);

            Map<String, Object> statistics = logAggregationService.getLogStatistics(startTime, endTime);
            return ResponseEntity.ok(Result.success(statistics));

        } catch (Exception e) {
            log.error("获取日志级别分布失败", e);
            return ResponseEntity.ok(Result.failure("获取分布失败: " + e.getMessage()));
        }
    }
}