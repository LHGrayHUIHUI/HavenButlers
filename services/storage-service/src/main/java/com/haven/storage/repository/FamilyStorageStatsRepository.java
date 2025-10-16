package com.haven.storage.repository;

import com.haven.storage.domain.model.file.FamilyStorageStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 家庭存储统计Repository
 * <p>
 * 提供家庭存储统计数据的数据库操作接口
 *
 * @author HavenButler
 */
@Repository
public interface FamilyStorageStatsRepository extends JpaRepository<FamilyStorageStats, String> {

    /**
     * 根据家庭ID查找统计信息
     */
    Optional<FamilyStorageStats> findByFamilyId(String familyId);

    /**
     * 检查家庭统计信息是否存在
     */
    boolean existsByFamilyId(String familyId);

    /**
     * 更新家庭文件数量
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalFiles = :totalFiles, s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int updateTotalFilesByFamily(@Param("familyId") String familyId, @Param("totalFiles") int totalFiles, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新家庭存储总大小
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalSize = :totalSize, s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int updateTotalSizeByFamily(@Param("familyId") String familyId, @Param("totalSize") long totalSize, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 批量更新统计信息
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalFiles = :totalFiles, s.totalSize = :totalSize, s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int updateStatsByFamily(@Param("familyId") String familyId, @Param("totalFiles") int totalFiles,
                           @Param("totalSize") long totalSize, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新存储健康状态
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.storageHealthy = :healthy, s.storageType = :storageType, s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int updateStorageHealthByFamily(@Param("familyId") String familyId, @Param("healthy") boolean healthy,
                                   @Param("storageType") String storageType, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 增加文件数量
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalFiles = s.totalFiles + 1, s.totalSize = s.totalSize + :fileSize, s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int incrementFilesByFamily(@Param("familyId") String familyId, @Param("fileSize") long fileSize, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 减少文件数量
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalFiles = s.totalFiles - 1, s.totalSize = s.totalSize - :fileSize, s.lastUpdated = :updateTime WHERE s.familyId = :familyId AND s.totalFiles > 0")
    int decrementFilesByFamily(@Param("familyId") String familyId, @Param("fileSize") long fileSize, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 获取最近更新的家庭统计列表
     */
    @Query("SELECT s FROM FamilyStorageStats s ORDER BY s.lastUpdated DESC")
    java.util.List<FamilyStorageStats> findRecentlyUpdatedStats();

    /**
     * 获取存储使用量最高的家庭
     */
    @Query("SELECT s FROM FamilyStorageStats s WHERE s.totalSize > 0 ORDER BY s.totalSize DESC")
    java.util.List<FamilyStorageStats> findTopFamiliesByStorageSize();

    /**
     * 根据存储类型查找家庭
     */
    @Query("SELECT s FROM FamilyStorageStats s WHERE s.storageType = :storageType ORDER BY s.totalSize DESC")
    java.util.List<FamilyStorageStats> findFamiliesByStorageType(@Param("storageType") String storageType);
}