package com.haven.admin.repository;

import com.haven.admin.entity.MonitoringDataEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 监控数据仓库接口
 *
 * @author HavenButler
 */
@Repository
public interface MonitoringDataRepository extends JpaRepository<MonitoringDataEntity, Long> {

    /**
     * 根据服务名称查询监控数据
     */
    List<MonitoringDataEntity> findByServiceName(String serviceName);

    /**
     * 根据服务名称和指标名称查询监控数据
     */
    List<MonitoringDataEntity> findByServiceNameAndMetricName(String serviceName, String metricName);

    /**
     * 根据时间范围查询监控数据
     */
    @Query("SELECT m FROM MonitoringDataEntity m WHERE m.timestamp >= :startTime AND m.timestamp <= :endTime")
    List<MonitoringDataEntity> findByTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 根据服务名称和时间范围查询监控数据
     */
    @Query("SELECT m FROM MonitoringDataEntity m WHERE m.serviceName = :serviceName AND m.timestamp >= :startTime AND m.timestamp <= :endTime")
    List<MonitoringDataEntity> findByServiceNameAndTimeRange(@Param("serviceName") String serviceName,
                                                             @Param("startTime") Instant startTime,
                                                             @Param("endTime") Instant endTime);

    /**
     * 根据服务名称、指标名称和时间范围查询监控数据
     */
    @Query("SELECT m FROM MonitoringDataEntity m WHERE m.serviceName = :serviceName AND m.metricName = :metricName AND m.timestamp >= :startTime AND m.timestamp <= :endTime")
    List<MonitoringDataEntity> findByServiceNameAndMetricNameAndTimeRange(@Param("serviceName") String serviceName,
                                                                         @Param("metricName") String metricName,
                                                                         @Param("startTime") Instant startTime,
                                                                         @Param("endTime") Instant endTime);

    /**
     * 查询最新的监控数据
     */
    @Query("SELECT m FROM MonitoringDataEntity m WHERE m.serviceName = :serviceName AND m.metricName = :metricName ORDER BY m.timestamp DESC")
    List<MonitoringDataEntity> findLatestByServiceAndMetric(@Param("serviceName") String serviceName,
                                                            @Param("metricName") String metricName);

    /**
     * 删除指定时间之前的数据（数据清理）
     */
    @Modifying
    @Query("DELETE FROM MonitoringDataEntity m WHERE m.timestamp < :cutoffTime")
    int deleteDataBefore(@Param("cutoffTime") Instant cutoffTime);

    /**
     * 获取指定服务的指标数量统计
     */
    @Query("SELECT m.metricName, COUNT(m) FROM MonitoringDataEntity m WHERE m.serviceName = :serviceName GROUP BY m.metricName")
    List<Object[]> getMetricCountByService(@Param("serviceName") String serviceName);

    /**
     * 获取所有服务列表
     */
    @Query("SELECT DISTINCT m.serviceName FROM MonitoringDataEntity m")
    List<String> findAllServiceNames();

    /**
     * 获取所有指标名称列表
     */
    @Query("SELECT DISTINCT m.metricName FROM MonitoringDataEntity m")
    List<String> findAllMetricNames();
}