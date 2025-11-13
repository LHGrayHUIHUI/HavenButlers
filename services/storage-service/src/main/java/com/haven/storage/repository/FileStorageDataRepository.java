package com.haven.storage.repository;

import com.haven.storage.domain.model.entity.FileStorageData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文件存储数据Repository
 * <p>
 * 提供FileStorageData实体的数据库操作接口，支持：
 * - 基础CRUD操作
 * - 存储类型查询
 * - 家庭存储桶查询
 * - 存储状态管理
 * <p>
 * 💡 使用规范：
 * - 使用命名查询提高可读性
 * - 复杂查询使用@Query注解
 * - 批量操作优化性能
 *
 * @author HavenButler
 */
@Repository
public interface FileStorageDataRepository extends JpaRepository<FileStorageData, Long> {

    /**
     * 根据存储ID查找存储数据
     */
    Optional<FileStorageData> findByStorageId(String storageId);

    /**
     * 根据文件ID查找存储数据
     */
    Optional<FileStorageData> findByFileId(String fileId);

    /**
     * 根据存储类型查找存储数据
     */
    List<FileStorageData> findByStorageType(Integer storageType);

    /**
     * 根据家庭存储桶名称查找存储数据
     */
    List<FileStorageData> findByFamilyBucketName(String familyBucketName);

    /**
     * 根据存储状态查找存储数据
     */
    List<FileStorageData> findByStorageStatus(Integer storageStatus);

    /**
     * 根据家庭ID和存储类型查找存储数据
     */
    @Query("SELECT f FROM FileStorageData f WHERE f.familyBucketName LIKE CONCAT('%', :familyId, '%') AND f.storageType = :storageType")
    List<FileStorageData> findByFamilyAndStorageType(@Param("familyId") String familyId, @Param("storageType") Integer storageType);

    /**
     * 查找活跃状态的存储数据
     */
    @Query("SELECT f FROM FileStorageData f WHERE f.storageStatus = 1 ORDER BY f.createTime DESC")
    List<FileStorageData> findActiveStorageData();

    /**
     * 统计家庭存储使用量
     */
    @Query("SELECT COUNT(f), COALESCE(SUM(f.fileSize), 0) FROM FileStorageData f WHERE f.familyBucketName LIKE CONCAT('%', :familyId, '%') AND f.storageStatus = 1")
    Object[] countFamilyStorageUsage(@Param("familyId") String familyId);

    /**
     * 根据文件ID删除存储数据
     */
    void deleteByFileId(String fileId);

    /**
     * 检查存储ID是否存在
     */
    boolean existsByStorageId(String storageId);

    /**
     * 检查文件ID是否存在存储数据
     */
    boolean existsByFileId(String fileId);
}