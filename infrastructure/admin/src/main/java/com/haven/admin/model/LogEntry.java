package com.haven.admin.model;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * 日志条目模型
 * 表示一条日志记录
 *
 * @author HavenButler
 */
@Data
@Builder
public class LogEntry {

    /**
     * 日志ID
     */
    private Long id;

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 实例ID
     */
    private String instanceId;

    /**
     * 日志级别
     */
    private String level;

    /**
     * 日志消息
     */
    private String message;

    /**
     * 时间戳
     */
    private Instant timestamp;

    /**
     * 线程名称
     */
    private String threadName;

    /**
     * 记录器名称
     */
    private String loggerName;

    /**
     * 链路追踪ID
     */
    private String traceId;

    /**
     * 异常信息
     */
    private String exception;

    /**
     * MDC上下文信息
     */
    private String mdcContext;

    /**
     * 日志来源（文件名和行号）
     */
    private String source;

    /**
     * 自定义标签
     */
    private String tags;
}