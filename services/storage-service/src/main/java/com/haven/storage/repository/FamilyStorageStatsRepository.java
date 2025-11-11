package com.haven.storage.repository;

import com.haven.storage.domain.model.entity.FamilyStorageStats;
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

    /**
     * 更新分类统计字段
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET s.totalImages = :totalImages, s.totalDocuments = :totalDocuments, " +
           "s.totalVideos = :totalVideos, s.totalAudio = :totalAudio, s.totalOthers = :totalOthers, " +
           "s.lastUpdated = :updateTime WHERE s.familyId = :familyId")
    int updateCategoryStatsByFamily(@Param("familyId") String familyId,
                                   @Param("totalImages") int totalImages,
                                   @Param("totalDocuments") int totalDocuments,
                                   @Param("totalVideos") int totalVideos,
                                   @Param("totalAudio") int totalAudio,
                                   @Param("totalOthers") int totalOthers,
                                   @Param("updateTime") LocalDateTime updateTime);

    /**
     * 增加特定分类的文件数量
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET " +
           "CASE " +
           "WHEN :category = 'image' THEN s.totalImages = s.totalImages + :delta " +
           "WHEN :category = 'document' THEN s.totalDocuments = s.totalDocuments + :delta " +
           "WHEN :category = 'video' THEN s.totalVideos = s.totalVideos + :delta " +
           "WHEN :category = 'audio' THEN s.totalAudio = s.totalAudio + :delta " +
           "ELSE s.totalOthers = s.totalOthers + :delta " +
           "END, " +
           "s.lastUpdated = :updateTime " +
           "WHERE s.familyId = :familyId")
    int incrementCategoryCountByFamily(@Param("familyId") String familyId,
                                     @Param("category") String category,
                                     @Param("delta") int delta,
                                     @Param("updateTime") LocalDateTime updateTime);

    /**
     * 更新峰值统计
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET " +
           "s.peakFileCount = GREATEST(s.peakFileCount, :currentFileCount), " +
           "s.peakStorageSize = GREATEST(s.peakStorageSize, :currentStorageSize), " +
           "s.lastUpdated = :updateTime " +
           "WHERE s.familyId = :familyId")
    int updatePeakStatsByFamily(@Param("familyId") String familyId,
                               @Param("currentFileCount") int currentFileCount,
                               @Param("currentStorageSize") long currentStorageSize,
                               @Param("updateTime") LocalDateTime updateTime);

    /**
     * 获取存储空间不足的家庭
     */
    @Query("SELECT s FROM FamilyStorageStats s WHERE s.totalSize > :threshold ORDER BY s.totalSize DESC")
    java.util.List<FamilyStorageStats> findFamiliesNearCapacityLimit(@Param("threshold") long threshold);

    /**
     * 获取不活跃的家庭（长时间未更新）
     */
    @Query("SELECT s FROM FamilyStorageStats s WHERE s.lastUpdated < :inactiveThreshold ORDER BY s.lastUpdated ASC")
    java.util.List<FamilyStorageStats> findInactiveFamilies(@Param("inactiveThreshold") LocalDateTime inactiveThreshold);

    /**
     * 批量更新家庭统计的上传和删除计数
     */
    @Modifying
    @Query("UPDATE FamilyStorageStats s SET " +
           "s.totalUploads = s.totalUploads + :uploadDelta, " +
           "s.totalDeletes = s.totalDeletes + :deleteDelta, " +
           "s.lastUpdated = :updateTime " +
           "WHERE s.familyId = :familyId")
    int updateOperationCountsByFamily(@Param("familyId") String familyId,
                                    @Param("uploadDelta") int uploadDelta,
                                    @Param("deleteDelta") int deleteDelta,
                                    @Param("updateTime") LocalDateTime updateTime);
}