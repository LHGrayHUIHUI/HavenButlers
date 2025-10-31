package com.haven.base.performance;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;

/**
 * 性能测试结果
 *
 * @author HavenButler
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceTestResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 测试名称
     */
    private String testName;

    /**
     * 测试开始时间
     */
    private LocalDateTime startTime;

    /**
     * 测试结束时间
     */
    private LocalDateTime endTime;

    /**
     * 总执行时间（毫秒）
     */
    private long totalExecutionTimeMs;

    /**
     * 总请求数
     */
    private long totalRequests;

    /**
     * 成功请求数
     */
    private long successfulRequests;

    /**
     * 失败请求数
     */
    private long failedRequests;

    /**
     * 平均响应时间（毫秒）
     */
    private double averageResponseTimeMs;

    /**
     * 最小响应时间（毫秒）
     */
    private long minResponseTimeMs;

    /**
     * 最大响应时间（毫秒）
     */
    private long maxResponseTimeMs;

    /**
     * P50响应时间（毫秒）
     */
    private double p50ResponseTimeMs;

    /**
     * P95响应时间（毫秒）
     */
    private double p95ResponseTimeMs;

    /**
     * P99响应时间（毫秒）
     */
    private double p99ResponseTimeMs;

    /**
     * 吞吐量（请求/秒）
     */
    private double throughput;

    /**
     * 错误率（百分比）
     */
    private double errorRate;

    /**
     * 使用的线程数
     */
    private int threadCount;

    /**
     * 并发连接数
     */
    private int concurrentConnections;

    /**
     * 测试指标详情
     */
    private Map<String, Object> metrics;

    /**
     * 系统资源使用情况
     */
    private SystemResourceUsage resourceUsage;

    /**
     * 性能测试评级
     */
    private PerformanceGrade grade;

    /**
     * 测试建议
     */
    private List<String> recommendations;

    /**
     * 是否通过性能基准
     */
    private boolean passedBenchmark;

    /**
     * 系统资源使用情况
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemResourceUsage {
        /**
         * CPU使用率（百分比）
         */
        private double cpuUsagePercent;

        /**
         * 内存使用量（MB）
         */
        private long memoryUsageMB;

        /**
         * 堆内存使用量（MB）
         */
        private long heapMemoryUsageMB;

        /**
         * 非堆内存使用量（MB）
         */
        private long nonHeapMemoryUsageMB;

        /**
         * GC次数
         */
        private long gcCount;

        /**
         * GC总时间（毫秒）
         */
        private long gcTimeMs;

        /**
         * 线程数
         */
        private int threadCount;

        /**
         * 加载类数量
         */
        private int loadedClassCount;
    }

    /**
     * 性能等级
     */
    public enum PerformanceGrade {
        EXCELLENT("优秀"),
        GOOD("良好"),
        ACCEPTABLE("可接受"),
        POOR("差"),
        CRITICAL("严重");

        private final String description;

        PerformanceGrade(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 计算成功率
     */
    public double getSuccessRate() {
        if (totalRequests == 0) return 0.0;
        return (double) successfulRequests / totalRequests * 100;
    }

    /**
     * 计算QPS（每秒请求数）
     */
    public double getQPS() {
        if (totalExecutionTimeMs == 0) return 0.0;
        return (double) totalRequests / (totalExecutionTimeMs / 1000.0);
    }

    /**
     * 添加指标
     */
    public void addMetric(String key, Object value) {
        if (metrics == null) {
            metrics = new HashMap<>();
        }
        metrics.put(key, value);
    }

    /**
     * 添加建议
     */
    public void addRecommendation(String recommendation) {
        if (recommendations == null) {
            recommendations = new ArrayList<>();
        }
        recommendations.add(recommendation);
    }

    /**
     * 获取测试摘要
     */
    public String getSummary() {
        return String.format(
            "性能测试结果: %s\n" +
            "总请求数: %d, 成功: %d, 失败: %d\n" +
            "平均响应时间: %.2fms\n" +
            "P95响应时间: %.2fms\n" +
            "吞吐量: %.2f req/s\n" +
            "错误率: %.2f%%\n" +
            "性能等级: %s",
            testName, totalRequests, successfulRequests, failedRequests,
            averageResponseTimeMs, p95ResponseTimeMs, throughput, errorRate,
            grade != null ? grade.getDescription() : "未评级"
        );
    }
}