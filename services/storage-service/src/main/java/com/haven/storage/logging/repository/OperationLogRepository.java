package com.haven.storage.logging.repository;

import com.haven.storage.logging.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志数据访问层
 * 提供操作日志的CRUD操作和统计查询
 *
 * @author HavenButler
 */
@Repository
public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {

    /**
     * 多条件查询操作日志(分页)
     */
    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "(:familyId IS NULL OR ol.familyId = :familyId) AND " +
           "(:serviceType IS NULL OR ol.serviceType = :serviceType) AND " +
           "(:clientIP IS NULL OR ol.clientIP = :clientIP) AND " +
           "(:startTime IS NULL OR ol.createdAt >= :startTime) AND " +
           "(:endTime IS NULL OR ol.createdAt <= :endTime)")
    Page<OperationLog> findByConditions(@Param("familyId") String familyId,
                                       @Param("serviceType") String serviceType,
                                       @Param("clientIP") String clientIP,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime,
                                       Pageable pageable);

    /**
     * 按家庭ID和时间范围统计操作数量
     */
    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE " +
           "ol.familyId = :familyId AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime")
    long countByFamilyIdAndTimeRange(@Param("familyId") String familyId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 按时间范围统计操作数量
     */
    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE " +
           "ol.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按状态和时间范围统计操作数量
     */
    @Query("SELECT COUNT(ol) FROM OperationLog ol WHERE " +
           "ol.resultStatus = :status AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime")
    long countByStatusAndTimeRange(@Param("status") String status,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 按服务类型统计操作数量(最近N天)
     */
    @Query("SELECT ol.serviceType, COUNT(ol) as count FROM OperationLog ol WHERE " +
           "ol.createdAt >= :startTime " +
           "GROUP BY ol.serviceType " +
           "ORDER BY count DESC")
    List<Object[]> countByServiceTypeAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 按家庭ID统计操作数量(Top 10)
     */
    @Query("SELECT ol.familyId, COUNT(ol) as count FROM OperationLog ol WHERE " +
           "ol.createdAt >= :startTime " +
           "GROUP BY ol.familyId " +
           "ORDER BY count DESC")
    List<Object[]> countByFamilyIdAfter(@Param("startTime") LocalDateTime startTime);

    /**
     * 查询响应时间统计
     */
    @Query("SELECT " +
           "AVG(ol.executionTimeMs) as avgTime, " +
           "MIN(ol.executionTimeMs) as minTime, " +
           "MAX(ol.executionTimeMs) as maxTime, " +
           "COUNT(ol) as totalCount " +
           "FROM OperationLog ol WHERE " +
           "ol.serviceType = :serviceType AND " +
           "ol.executionTimeMs IS NOT NULL AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime")
    Object[] findResponseTimeStats(@Param("serviceType") String serviceType,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询错误率统计
     */
    @Query("SELECT " +
           "COUNT(CASE WHEN ol.resultStatus = 'FAILED' THEN 1 END) as failedCount, " +
           "COUNT(CASE WHEN ol.resultStatus = 'SUCCESS' THEN 1 END) as successCount, " +
           "COUNT(ol) as totalCount " +
           "FROM OperationLog ol WHERE " +
           "ol.serviceType = :serviceType AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime")
    Object[] findErrorRateStats(@Param("serviceType") String serviceType,
                               @Param("startTime") LocalDateTime startTime,
                               @Param("endTime") LocalDateTime endTime);

    /**
     * 查询慢查询(执行时间超过阈值)
     */
    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "ol.executionTimeMs > :threshold AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY ol.executionTimeMs DESC")
    List<OperationLog> findSlowOperations(@Param("threshold") int threshold,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         Pageable pageable);

    /**
     * 按操作类型统计
     */
    @Query("SELECT ol.operationType, COUNT(ol) as count FROM OperationLog ol WHERE " +
           "ol.familyId = :familyId AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.operationType " +
           "ORDER BY count DESC")
    List<Object[]> countByOperationTypeAndFamilyId(@Param("familyId") String familyId,
                                                  @Param("startTime") LocalDateTime startTime,
                                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询活跃IP统计
     */
    @Query("SELECT ol.clientIP, COUNT(ol) as count FROM OperationLog ol WHERE " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.clientIP " +
           "ORDER BY count DESC")
    List<Object[]> countByClientIP(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime,
                                  Pageable pageable);

    /**
     * 按小时统计操作数量(用于绘制趋势图)
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', ol.createdAt, '%Y-%m-%d %H:00:00') as hour, " +
           "COUNT(ol) as count " +
           "FROM OperationLog ol WHERE " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', ol.createdAt, '%Y-%m-%d %H:00:00') " +
           "ORDER BY hour")
    List<Object[]> countByHour(@Param("startTime") LocalDateTime startTime,
                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查询指定用户的操作历史
     */
    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "ol.userId = :userId AND " +
           "ol.familyId = :familyId " +
           "ORDER BY ol.createdAt DESC")
    List<OperationLog> findByUserIdAndFamilyId(@Param("userId") String userId,
                                              @Param("familyId") String familyId,
                                              Pageable pageable);

    /**
     * 查询高风险操作
     */
    @Query("SELECT ol FROM OperationLog ol WHERE " +
           "ol.resultStatus = 'BLOCKED' AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY ol.createdAt DESC")
    List<OperationLog> findBlockedOperations(@Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 清理过期日志
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM OperationLog ol WHERE ol.createdAt < :cutoffTime")
    int deleteByCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查询数据库使用情况统计
     */
    @Query("SELECT ol.databaseName, ol.tableOrCollection, COUNT(ol) as count " +
           "FROM OperationLog ol WHERE " +
           "ol.familyId = :familyId AND " +
           "ol.databaseName IS NOT NULL AND " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.databaseName, ol.tableOrCollection " +
           "ORDER BY count DESC")
    List<Object[]> findDatabaseUsageStats(@Param("familyId") String familyId,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查询异常操作模式(同一IP短时间内大量操作)
     */
    @Query("SELECT ol.clientIP, COUNT(ol) as count " +
           "FROM OperationLog ol WHERE " +
           "ol.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY ol.clientIP " +
           "HAVING COUNT(ol) > :threshold " +
           "ORDER BY count DESC")
    List<Object[]> findSuspiciousActivity(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime,
                                         @Param("threshold") long threshold);

    /**
     * 按月份统计存储大小(用于容量规划)
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', ol.createdAt, '%Y-%m') as month, " +
           "COUNT(ol) as logCount, " +
           "AVG(LENGTH(ol.operationContent)) as avgContentSize " +
           "FROM OperationLog ol WHERE " +
           "ol.createdAt >= :startTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', ol.createdAt, '%Y-%m') " +
           "ORDER BY month DESC")
    List<Object[]> findStorageGrowthStats(@Param("startTime") LocalDateTime startTime);
}