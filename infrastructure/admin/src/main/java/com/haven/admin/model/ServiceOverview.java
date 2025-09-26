package com.haven.admin.model;

import lombok.Data;

/**
 * 服务健康概览数据模型
 *
 * 用于快速展示服务的整体健康状态和关键指标
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Data
public class ServiceOverview {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务整体状态：UP, DEGRADED, DOWN
     */
    private String status;

    /**
     * 实例总数
     */
    private int instanceCount;

    /**
     * 健康实例数
     */
    private int healthyCount;

    /**
     * 平均CPU使用率（百分比）
     */
    private double cpuUsageAvg;

    /**
     * 平均内存使用量（MB）
     */
    private double memoryUsageAvg;

    /**
     * 请求速率（请求/秒）- 所有实例总和
     */
    private double requestRate;

    /**
     * 错误率（百分比）- 所有实例平均
     */
    private double errorRate;

    /**
     * 获取健康状态描述
     */
    public String getHealthDescription() {
        return String.format("%d/%d", healthyCount, instanceCount);
    }

    /**
     * 获取状态颜色（用于前端显示）
     */
    public String getStatusColor() {
        switch (status) {
            case "UP":
                return "green";
            case "DEGRADED":
                return "orange";
            case "DOWN":
                return "red";
            default:
                return "gray";
        }
    }

    /**
     * 判断是否健康
     */
    public boolean isHealthy() {
        return "UP".equals(status);
    }

    /**
     * 判断是否降级
     */
    public boolean isDegraded() {
        return "DEGRADED".equals(status);
    }

    /**
     * 判断是否宕机
     */
    public boolean isDown() {
        return "DOWN".equals(status);
    }
}