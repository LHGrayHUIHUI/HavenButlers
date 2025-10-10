package com.haven.admin.repository;

import com.haven.admin.entity.AlertHistoryEntity;
import com.haven.admin.model.AlertRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * 告警历史仓库接口
 *
 * @author HavenButler
 */
@Repository
public interface AlertHistoryRepository extends JpaRepository<AlertHistoryEntity, Long> {

    /**
     * 根据服务名称查询告警历史
     */
    List<AlertHistoryEntity> findByServiceName(String serviceName);

    /**
     * 根据告警级别查询告警历史
     */
    List<AlertHistoryEntity> findByLevel(AlertRule.AlertLevel level);

    /**
     * 根据状态查询告警历史
     */
    List<AlertHistoryEntity> findByStatus(String status);

    /**
     * 根据告警ID查询告警历史
     */
    List<AlertHistoryEntity> findByAlertId(Long alertId);

    /**
     * 根据时间范围查询告警历史
     */
    @Query("SELECT a FROM AlertHistoryEntity a WHERE a.triggerTime >= :startTime AND a.triggerTime <= :endTime")
    List<AlertHistoryEntity> findByTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 根据服务名称和时间范围查询告警历史
     */
    @Query("SELECT a FROM AlertHistoryEntity a WHERE a.serviceName = :serviceName AND a.triggerTime >= :startTime AND a.triggerTime <= :endTime")
    List<AlertHistoryEntity> findByServiceNameAndTimeRange(@Param("serviceName") String serviceName,
                                                          @Param("startTime") Instant startTime,
                                                          @Param("endTime") Instant endTime);

    /**
     * 根据服务名称、告警级别和时间范围查询告警历史
     */
    @Query("SELECT a FROM AlertHistoryEntity a WHERE a.serviceName = :serviceName AND a.level = :level AND a.triggerTime >= :startTime AND a.triggerTime <= :endTime")
    List<AlertHistoryEntity> findByServiceNameAndLevelAndTimeRange(@Param("serviceName") String serviceName,
                                                                   @Param("level") AlertRule.AlertLevel level,
                                                                   @Param("startTime") Instant startTime,
                                                                   @Param("endTime") Instant endTime);

    /**
     * 统计指定时间范围内的告警数量（按级别分组）
     */
    @Query("SELECT a.level, COUNT(a) FROM AlertHistoryEntity a WHERE a.triggerTime >= :startTime AND a.triggerTime <= :endTime GROUP BY a.level")
    List<Object[]> countAlertsByLevelInTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 统计指定时间范围内的告警数量（按服务分组）
     */
    @Query("SELECT a.serviceName, COUNT(a) FROM AlertHistoryEntity a WHERE a.triggerTime >= :startTime AND a.triggerTime <= :endTime GROUP BY a.serviceName")
    List<Object[]> countAlertsByServiceInTimeRange(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    /**
     * 获取最新的告警记录
     */
    @Query("SELECT a FROM AlertHistoryEntity a ORDER BY a.triggerTime DESC")
    List<AlertHistoryEntity> findLatestAlerts();

    /**
     * 删除指定时间之前的告警历史记录（数据清理）
     */
    @Modifying
    @Query("DELETE FROM AlertHistoryEntity a WHERE a.triggerTime < :cutoffTime")
    int deleteAlertsBefore(@Param("cutoffTime") Instant cutoffTime);

    /**
     * 获取未处理的告警数量
     */
    @Query("SELECT COUNT(a) FROM AlertHistoryEntity a WHERE a.status = 'PENDING'")
    long countPendingAlerts();

    /**
     * 获取指定服务的未处理告警数量
     */
    @Query("SELECT COUNT(a) FROM AlertHistoryEntity a WHERE a.serviceName = :serviceName AND a.status = 'PENDING'")
    long countPendingAlertsByService(@Param("serviceName") String serviceName);
}