package com.haven.storage.logging.repository;

import com.haven.storage.logging.entity.SecurityLog;
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
import java.util.Map;

/**
 * 安全日志数据访问层
 * 提供安全事件日志的CRUD操作和安全分析查询
 *
 * @author HavenButler
 */
@Repository
public interface SecurityLogRepository extends JpaRepository<SecurityLog, Long> {

    /**
     * 按时间范围统计安全事件数量
     */
    @Query("SELECT COUNT(sl) FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime")
    long countByTimeRange(@Param("startTime") LocalDateTime startTime,
                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按家庭ID和时间范围统计安全事件数量
     */
    @Query("SELECT COUNT(sl) FROM SecurityLog sl WHERE " +
           "(:familyId IS NULL OR sl.familyId = :familyId) AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime")
    long countByFamilyIdAndTimeRange(@Param("familyId") String familyId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询安全事件统计(按事件类型分组)
     */
    @Query("SELECT sl.eventType, sl.riskLevel, COUNT(sl) as count " +
           "FROM SecurityLog sl WHERE " +
           "(:familyId IS NULL OR sl.familyId = :familyId) AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY sl.eventType, sl.riskLevel " +
           "ORDER BY count DESC")
    List<Object[]> findSecurityStats(@Param("familyId") String familyId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    /**
     * 查询高风险安全事件
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.riskLevel IN ('HIGH', 'CRITICAL') AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findHighRiskEvents(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime,
                                        Pageable pageable);

    /**
     * 按IP地址统计安全事件(识别攻击源)
     */
    @Query("SELECT sl.clientIP, " +
           "COUNT(sl) as totalEvents, " +
           "COUNT(CASE WHEN sl.riskLevel = 'HIGH' THEN 1 END) as highRiskEvents, " +
           "COUNT(CASE WHEN sl.riskLevel = 'CRITICAL' THEN 1 END) as criticalEvents, " +
           "MAX(sl.createdAt) as lastEventTime " +
           "FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY sl.clientIP " +
           "HAVING COUNT(sl) > :threshold " +
           "ORDER BY totalEvents DESC")
    List<Object[]> findSuspiciousIPs(@Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime,
                                    @Param("threshold") long threshold);

    /**
     * 查询认证失败事件
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.eventType = 'AUTH_FAILED' AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findAuthFailures(@Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime);

    /**
     * 查询访问拒绝事件
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.eventType = 'ACCESS_DENIED' AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findAccessDenials(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询被拦截的危险操作
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.eventType = 'DANGEROUS_OPERATION_BLOCKED' AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findBlockedOperations(@Param("startTime") LocalDateTime startTime,
                                           @Param("endTime") LocalDateTime endTime);

    /**
     * 按服务类型统计安全事件
     */
    @Query("SELECT sl.serviceType, sl.eventType, COUNT(sl) as count " +
           "FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY sl.serviceType, sl.eventType " +
           "ORDER BY count DESC")
    List<Object[]> countByServiceTypeAndEventType(@Param("startTime") LocalDateTime startTime,
                                                 @Param("endTime") LocalDateTime endTime);

    /**
     * 查询未处理的安全事件
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.isHandled = false AND " +
           "sl.riskLevel IN ('HIGH', 'CRITICAL') " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findUnhandledEvents(Pageable pageable);

    /**
     * 按小时统计安全事件趋势
     */
    @Query("SELECT " +
           "FUNCTION('DATE_FORMAT', sl.createdAt, '%Y-%m-%d %H:00:00') as hour, " +
           "sl.riskLevel, " +
           "COUNT(sl) as count " +
           "FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('DATE_FORMAT', sl.createdAt, '%Y-%m-%d %H:00:00'), sl.riskLevel " +
           "ORDER BY hour")
    List<Object[]> findSecurityTrends(@Param("startTime") LocalDateTime startTime,
                                     @Param("endTime") LocalDateTime endTime);

    /**
     * 查询重复安全事件(同一IP在短时间内的相同事件)
     */
    @Query("SELECT sl.clientIP, sl.eventType, COUNT(sl) as count, " +
           "MIN(sl.createdAt) as firstOccurrence, " +
           "MAX(sl.createdAt) as lastOccurrence " +
           "FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY sl.clientIP, sl.eventType " +
           "HAVING COUNT(sl) >= :minOccurrences " +
           "ORDER BY count DESC")
    List<Object[]> findRepeatedSecurityEvents(@Param("startTime") LocalDateTime startTime,
                                             @Param("endTime") LocalDateTime endTime,
                                             @Param("minOccurrences") int minOccurrences);

    /**
     * 查询地理位置安全统计
     */
    @Query("SELECT sl.geoLocation, COUNT(sl) as count, " +
           "COUNT(CASE WHEN sl.riskLevel IN ('HIGH', 'CRITICAL') THEN 1 END) as highRiskCount " +
           "FROM SecurityLog sl WHERE " +
           "sl.geoLocation IS NOT NULL AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY sl.geoLocation " +
           "ORDER BY count DESC")
    List<Object[]> findGeoSecurityStats(@Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    /**
     * 查询安全事件响应时间统计
     */
    @Query("SELECT " +
           "AVG(TIMESTAMPDIFF(MINUTE, sl.createdAt, sl.handledAt)) as avgResponseMinutes, " +
           "MIN(TIMESTAMPDIFF(MINUTE, sl.createdAt, sl.handledAt)) as minResponseMinutes, " +
           "MAX(TIMESTAMPDIFF(MINUTE, sl.createdAt, sl.handledAt)) as maxResponseMinutes, " +
           "COUNT(sl) as totalHandled " +
           "FROM SecurityLog sl WHERE " +
           "sl.isHandled = true AND " +
           "sl.handledAt IS NOT NULL AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime")
    Object[] findResponseTimeStats(@Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询特定用户的安全事件
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.userId = :userId AND " +
           "(:familyId IS NULL OR sl.familyId = :familyId) " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findByUserId(@Param("userId") String userId,
                                  @Param("familyId") String familyId,
                                  Pageable pageable);

    /**
     * 查询特定IP的安全事件历史
     */
    @Query("SELECT sl FROM SecurityLog sl WHERE " +
           "sl.clientIP = :clientIP " +
           "ORDER BY sl.createdAt DESC")
    List<SecurityLog> findByClientIP(@Param("clientIP") String clientIP,
                                    Pageable pageable);

    /**
     * 标记事件为已处理
     */
    @Modifying
    @Transactional
    @Query("UPDATE SecurityLog sl SET " +
           "sl.isHandled = true, " +
           "sl.handledBy = :handledBy, " +
           "sl.handledAt = :handledAt, " +
           "sl.handlingNotes = :notes " +
           "WHERE sl.id = :id")
    int markAsHandled(@Param("id") Long id,
                     @Param("handledBy") String handledBy,
                     @Param("handledAt") LocalDateTime handledAt,
                     @Param("notes") String notes);

    /**
     * 批量标记事件为已处理
     */
    @Modifying
    @Transactional
    @Query("UPDATE SecurityLog sl SET " +
           "sl.isHandled = true, " +
           "sl.handledBy = :handledBy, " +
           "sl.handledAt = :handledAt " +
           "WHERE sl.id IN :ids")
    int batchMarkAsHandled(@Param("ids") List<Long> ids,
                          @Param("handledBy") String handledBy,
                          @Param("handledAt") LocalDateTime handledAt);

    /**
     * 清理过期安全日志
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SecurityLog sl WHERE sl.createdAt < :cutoffTime")
    int deleteByCreatedAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * 查询安全评分(基于事件类型和风险等级)
     */
    @Query("SELECT " +
           "(:familyId) as familyId, " +
           "COUNT(CASE WHEN sl.riskLevel = 'CRITICAL' THEN 1 END) * 10 + " +
           "COUNT(CASE WHEN sl.riskLevel = 'HIGH' THEN 1 END) * 5 + " +
           "COUNT(CASE WHEN sl.riskLevel = 'MEDIUM' THEN 1 END) * 2 + " +
           "COUNT(CASE WHEN sl.riskLevel = 'LOW' THEN 1 END) * 1 as securityScore " +
           "FROM SecurityLog sl WHERE " +
           "(:familyId IS NULL OR sl.familyId = :familyId) AND " +
           "sl.createdAt BETWEEN :startTime AND :endTime")
    Object[] calculateSecurityScore(@Param("familyId") String familyId,
                                  @Param("startTime") LocalDateTime startTime,
                                  @Param("endTime") LocalDateTime endTime);

    /**
     * 查询安全事件热力图数据(按小时和事件类型)
     */
    @Query("SELECT " +
           "FUNCTION('HOUR', sl.createdAt) as hour, " +
           "FUNCTION('DAYOFWEEK', sl.createdAt) as dayOfWeek, " +
           "sl.eventType, " +
           "COUNT(sl) as count " +
           "FROM SecurityLog sl WHERE " +
           "sl.createdAt BETWEEN :startTime AND :endTime " +
           "GROUP BY FUNCTION('HOUR', sl.createdAt), FUNCTION('DAYOFWEEK', sl.createdAt), sl.eventType " +
           "ORDER BY dayOfWeek, hour")
    List<Object[]> findSecurityHeatmapData(@Param("startTime") LocalDateTime startTime,
                                          @Param("endTime") LocalDateTime endTime);
}