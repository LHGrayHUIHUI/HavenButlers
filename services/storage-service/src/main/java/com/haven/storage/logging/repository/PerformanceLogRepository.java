package com.haven.storage.logging.repository;

import com.haven.storage.logging.entity.PerformanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 性能日志数据访问层
 * 提供性能指标的存储和统计分析功能
 *
 * @author HavenButler
 */
@Repository
public interface PerformanceLogRepository extends JpaRepository<PerformanceLog, Long> {

    /**
     * 查询性能统计数据
     */
    @Query("SELECT " +
           "pl.serviceType as serviceType, " +
           "pl.metricName as metricName, " +
           "AVG(pl.metricValue) as avgValue, " +
           "MIN(pl.metricValue) as minValue, " +
           "MAX(pl.metricValue) as maxValue, " +
           "COUNT(pl) as sampleCount " +
           "FROM PerformanceLog pl WHERE " +
           "(:serviceType IS NULL OR pl.serviceType = :serviceType) AND " +
           "(:metricName IS NULL OR pl.metricName = :metricName) AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.serviceType, pl.metricName " +
           "ORDER BY pl.serviceType, pl.metricName")
    List<Object[]> findPerformanceStats(@Param("serviceType") String serviceType,
                                       @Param("metricName") String metricName,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询平均指标值
     */
    @Query("SELECT AVG(pl.metricValue) FROM PerformanceLog pl WHERE " +
           "pl.metricName = :metricName AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime")
    Double findAvgMetricByTimeRange(@Param("metricName") String metricName,
                                   @Param("startTime") LocalDateTime startTime,
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 查询最新的性能指标
     */
    @Query("SELECT pl FROM PerformanceLog pl WHERE " +
           "pl.serviceType = :serviceType AND " +
           "pl.metricName = :metricName " +
           "ORDER BY pl.createdAt DESC")
    List<PerformanceLog> findLatestMetrics(@Param("serviceType") String serviceType,
                                          @Param("metricName") String metricName,
                                          org.springframework.data.domain.Pageable pageable);

    /**
     * 查询超过阈值的性能事件
     */
    @Query("SELECT pl FROM PerformanceLog pl WHERE " +
           "pl.isThresholdExceeded = true AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY pl.createdAt DESC")
    List<PerformanceLog> findThresholdViolations(@Param("startTime") LocalDateTime startTime,
                                                @Param("endTime") LocalDateTime endTime);

    /**
     * 按服务类型统计性能指标
     */
    @Query("SELECT pl.serviceType, pl.metricName, " +
           "AVG(pl.metricValue) as avgValue, " +
           "COUNT(pl) as sampleCount, " +
           "COUNT(CASE WHEN pl.isThresholdExceeded = true THEN 1 END) as violationCount " +
           "FROM PerformanceLog pl WHERE " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.serviceType, pl.metricName " +
           "ORDER BY pl.serviceType, avgValue DESC")
    List<Object[]> findServicePerformanceOverview(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查询性能趋势数据(按小时分组)
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m-%d %H:00:00') as hour, " +
           "pl.serviceType, " +
           "pl.metricName, " +
           "AVG(pl.metricValue) as avgValue, " +
           "MAX(pl.metricValue) as maxValue, " +
           "COUNT(pl) as sampleCount " +
           "FROM PerformanceLog pl WHERE " +
           "pl.serviceType = :serviceType AND " +
           "pl.metricName = :metricName AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m-%d %H:00:00'), pl.serviceType, pl.metricName " +
           "ORDER BY hour")
    List<Object[]> findPerformanceTrends(@Param("serviceType") String serviceType,
                                        @Param("metricName") String metricName,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询实例性能对比
     */
    @Query("SELECT pl.instanceId, " +
           "AVG(pl.metricValue) as avgValue, " +
           "MIN(pl.metricValue) as minValue, " +
           "MAX(pl.metricValue) as maxValue, " +
           "STDDEV(pl.metricValue) as stdDev " +
           "FROM PerformanceLog pl WHERE " +
           "pl.serviceType = :serviceType AND " +
           "pl.metricName = :metricName AND " +
           "pl.instanceId IS NOT NULL AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.instanceId " +
           "ORDER BY avgValue DESC")
    List<Object[]> findInstancePerformanceComparison(@Param("serviceType") String serviceType,
                                                    @Param("metricName") String metricName,
                                                    @Param("startTime") LocalDateTime startTime,
                                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询连接池性能统计
     */
    @Query("SELECT " +
           "pl.serviceType, " +
           "AVG(CASE WHEN pl.metricName = 'connection_pool_active' THEN pl.metricValue END) as avgActive, " +
           "AVG(CASE WHEN pl.metricName = 'connection_pool_idle' THEN pl.metricValue END) as avgIdle, " +
           "MAX(CASE WHEN pl.metricName = 'connection_pool_active' THEN pl.metricValue END) as maxActive " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName IN ('connection_pool_active', 'connection_pool_idle') AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.serviceType")
    List<Object[]> findConnectionPoolStats(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查询响应时间百分位数
     */
    @Query(value = "SELECT " +
           "PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY metric_value) as p50, " +
           "PERCENTILE_CONT(0.75) WITHIN GROUP (ORDER BY metric_value) as p75, " +
           "PERCENTILE_CONT(0.90) WITHIN GROUP (ORDER BY metric_value) as p90, " +
           "PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY metric_value) as p95, " +
           "PERCENTILE_CONT(0.99) WITHIN GROUP (ORDER BY metric_value) as p99 " +
           "FROM performance_logs " +
           "WHERE service_type = :serviceType " +
           "AND metric_name = 'response_time' " +
           "AND created_at BETWEEN :startTime AND :endTime",
           nativeQuery = true)
    Object[] findResponseTimePercentiles(@Param("serviceType") String serviceType,
                                        @Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查询QPS统计
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m-%d %H:%i:00') as minute, " +
           "pl.serviceType, " +
           "SUM(pl.metricValue) as totalRequests " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName = 'qps' AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m-%d %H:%i:00'), pl.serviceType " +
           "ORDER BY minute")
    List<Object[]> findQPSStats(@Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询错误率统计
     */
    @Query("SELECT " +
           "pl.serviceType, " +
           "AVG(CASE WHEN pl.metricName = 'error_rate' THEN pl.metricValue END) as avgErrorRate, " +
           "MAX(CASE WHEN pl.metricName = 'error_rate' THEN pl.metricValue END) as maxErrorRate " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName = 'error_rate' AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.serviceType " +
           "ORDER BY avgErrorRate DESC")
    List<Object[]> findErrorRateStats(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询系统资源使用情况
     */
    @Query("SELECT " +
           "pl.instanceId, " +
           "pl.hostname, " +
           "AVG(CASE WHEN pl.metricName = 'cpu_usage' THEN pl.metricValue END) as avgCpuUsage, " +
           "AVG(CASE WHEN pl.metricName = 'memory_usage' THEN pl.metricValue END) as avgMemoryUsage, " +
           "AVG(CASE WHEN pl.metricName = 'disk_usage' THEN pl.metricValue END) as avgDiskUsage " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName IN ('cpu_usage', 'memory_usage', 'disk_usage') AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.instanceId, pl.hostname " +
           "ORDER BY avgCpuUsage DESC")
    List<Object[]> findSystemResourceUsage(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);

    /**
     * 查询性能异常事件
     */
    @Query("SELECT pl FROM PerformanceLog pl WHERE " +
           "pl.metricName = :metricName AND " +
           "pl.metricValue > (" +
           "   SELECT AVG(p.metricValue) + 2 * STDDEV(p.metricValue) " +
           "   FROM PerformanceLog p " +
           "   WHERE p.metricName = :metricName " +
           "   AND p.createdAt BETWEEN :baselineStart AND :baselineEnd" +
           ") AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY pl.metricValue DESC")
    List<PerformanceLog> findPerformanceAnomalies(@Param("metricName") String metricName,
                                                  @Param("baselineStart") LocalDateTime baselineStart,
                                                  @Param("baselineEnd") LocalDateTime baselineEnd,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询容量规划数据
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m') as month, " +
           "pl.serviceType, " +
           "pl.metricName, " +
           "AVG(pl.metricValue) as avgValue, " +
           "MAX(pl.metricValue) as maxValue, " +
           "MIN(pl.metricValue) as minValue " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName IN ('connection_pool_active', 'memory_usage', 'disk_usage', 'qps') AND " +
           "pl.createdAt >= :startTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', pl.createdAt, '%Y-%m'), pl.serviceType, pl.metricName " +
           "ORDER BY month DESC")
    List<Object[]> findCapacityPlanningData(@Param("startTime") LocalDateTime startTime);

    /**
     * 清理过期性能日志
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM PerformanceLog pl WHERE pl.createdAt < :cutoffTime")
    int deleteByCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 批量插入性能日志(用于批量写入优化)
     */
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO performance_logs (service_type, metric_name, metric_value, metric_unit, instance_id, created_at) " +
                   "VALUES (:serviceType, :metricName, :metricValue, :metricUnit, :instanceId, :createdAt)",
           nativeQuery = true)
    void batchInsert(@Param("serviceType") String serviceType,
                    @Param("metricName") String metricName,
                    @Param("metricValue") Double metricValue,
                    @Param("metricUnit") String metricUnit,
                    @Param("instanceId") String instanceId,
                    @Param("createdAt") LocalDateTime createdAt);

    /**
     * 查询SLA达成率
     */
    @Query("SELECT " +
           "pl.serviceType, " +
           "COUNT(CASE WHEN pl.metricValue <= :slaThreshold THEN 1 END) * 100.0 / COUNT(pl) as slaCompliance " +
           "FROM PerformanceLog pl WHERE " +
           "pl.metricName = 'response_time' AND " +
           "pl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY pl.serviceType")
    List<Object[]> findSLACompliance(@Param("slaThreshold") double slaThreshold,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);
}