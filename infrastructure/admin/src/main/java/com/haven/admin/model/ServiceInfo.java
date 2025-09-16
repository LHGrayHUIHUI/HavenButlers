package com.haven.admin.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 服务信息
 */
@Data
public class ServiceInfo {

    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 服务状态
     */
    private String status;

    /**
     * 实例总数
     */
    private Integer instanceCount;

    /**
     * 健康实例数
     */
    private Integer healthyCount;

    /**
     * 服务版本
     */
    private String version;

    /**
     * 服务描述
     */
    private String description;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 实例列表
     */
    private List<Instance> instances;

    /**
     * 服务实例信息
     */
    @Data
    public static class Instance {

        /**
         * 实例ID
         */
        private String instanceId;

        /**
         * 主机地址
         */
        private String host;

        /**
         * 端口
         */
        private Integer port;

        /**
         * 是否健康
         */
        private boolean healthy;

        /**
         * 启动时间
         */
        private LocalDateTime startTime;

        /**
         * 元数据
         */
        private Map<String, String> metadata;

        /**
         * CPU使用率
         */
        private Double cpuUsage;

        /**
         * 内存使用率
         */
        private Double memoryUsage;

        /**
         * 磁盘使用率
         */
        private Double diskUsage;
    }
}