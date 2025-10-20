package com.haven.storage.repository;

import com.haven.storage.domain.model.file.FileMetadata;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文件元数据Repository
 * <p>
 * 提供FileMetadata实体的数据库操作接口，支持：
 * - 基础CRUD操作
 * - 复杂查询条件
 * - 分页查询
 * - 统计查询
 * <p>
 * 💡 使用规范：
 * - 使用命名查询提高可读性
 * - 复杂查询使用@Query注解
 * - 批量操作优化性能
 *
 * @author HavenButler
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, String> {

    /**
     * 根据家庭ID查找未删除的文件列表
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1 ORDER BY f.createTime DESC")
    List<FileMetadata> findActiveFilesByFamily(@Param("familyId") String familyId);

    /**
     * 根据家庭ID查找所有文件（包含已删除）
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId ORDER BY f.createTime DESC")
    List<FileMetadata> findAllFilesByFamily(@Param("familyId") String familyId);

    /**
     * 根据用户ID和家庭ID查找文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.ownerId = :userId AND f.deleted != 1 ORDER BY f.createTime DESC")
    List<FileMetadata> findFilesByFamilyAndOwner(@Param("familyId") String familyId, @Param("userId") String userId);

    /**
     * 根据文件类型查找文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.fileType LIKE :fileType AND f.deleted != 1 ORDER BY f.createTime DESC")
    List<FileMetadata> findFilesByFamilyAndType(@Param("familyId") String familyId, @Param("fileType") String fileType);

    /**
     * 根据文件夹路径查找文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.folderPath = :folderPath AND f.deleted != 1 ORDER BY f.createTime DESC")
    List<FileMetadata> findFilesByFamilyAndPath(@Param("familyId") String familyId, @Param("folderPath") String folderPath);

    /**
     * 搜索文件（文件名、描述、标签）
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1 AND " +
           "(LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "EXISTS (SELECT 1 FROM f.tags t WHERE LOWER(t) LIKE LOWER(CONCAT('%', :keyword, '%')))) " +
           "ORDER BY f.createTime DESC")
    Page<FileMetadata> searchFiles(@Param("familyId") String familyId, @Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计家庭文件数量
     */
    @Query("SELECT COUNT(f) FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1")
    long countActiveFilesByFamily(@Param("familyId") String familyId);

    /**
     * 统计家庭文件总大小
     */
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1")
    long sumFileSizeByFamily(@Param("familyId") String familyId);

    /**
     * 按文件类型统计家庭文件数量
     */
    @Query("SELECT f.fileType, COUNT(f) as count FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1 " +
           "GROUP BY f.fileType ORDER BY count DESC")
    List<Object[]> countFilesByTypeByFamily(@Param("familyId") String familyId);

    /**
     * 查找指定时间范围内上传的文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1 " +
           "AND f.createTime BETWEEN :startTime AND :endTime ORDER BY f.createTime DESC")
    List<FileMetadata> findFilesByFamilyAndTime(@Param("familyId") String familyId,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    /**
     * 查找用户的文件（包含已删除）
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.ownerId = :userId ORDER BY f.createTime DESC")
    List<FileMetadata> findAllFilesByFamilyAndOwner(@Param("familyId") String familyId, @Param("userId") String userId);

    /**
     * 获取用户最近访问的文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.familyId = :familyId AND f.ownerId = :userId AND f.deleted != 1 " +
           "ORDER BY f.lastAccessTime DESC")
    List<FileMetadata> findRecentlyAccessedFiles(@Param("familyId") String familyId, @Param("userId") String userId, Pageable pageable);

    /**
     * 获取需要OCR处理的文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.deleted != 1 AND " +
           "(f.contentType LIKE '%image%' OR f.contentType LIKE '%application/pdf%') " +
           "AND NOT EXISTS (SELECT 1 FROM f.tags t WHERE t = 'ocr_processed') " +
           "ORDER BY f.createTime ASC")
    List<FileMetadata> findFilesForOcr();

    /**
     * 根据文件ID和家庭ID查找未删除的文件
     */
    @Query("SELECT f FROM FileMetadata f WHERE f.fileId = :fileId AND f.familyId = :familyId AND f.deleted != 1")
    Optional<FileMetadata> findActiveFileByIdAndFamily(@Param("fileId") String fileId, @Param("familyId") String familyId);

    /**
     * 批量软删除文件
     */
    @Query("UPDATE FileMetadata f SET f.deleted = 1, f.updateTime = :updateTime " +
           "WHERE f.fileId IN :fileIds")
    int batchSoftDelete(@Param("fileIds") List<String> list, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 根据文件ID软删除文件
     */
    @Query("UPDATE FileMetadata f SET f.deleted = 1, f.updateTime = :updateTime " +
           "WHERE f.fileId = :fileId")
    int softDeleteById(@Param("fileId") String fileId, @Param("updateTime") LocalDateTime updateTime);

    /**
     * 批量更新访问统计
     */
    @Query("UPDATE FileMetadata f SET f.accessCount = f.accessCount + 1, f.lastAccessTime = :accessTime " +
           "WHERE f.fileId = :fileId")
    int incrementAccessCount(@Param("fileId") String fileId, @Param("accessTime") LocalDateTime accessTime);

    /**
     * 检查文件是否存在且未被删除
     */
    @Query("SELECT COUNT(f) > 0 FROM FileMetadata f WHERE f.fileId = :fileId AND f.deleted != 1")
    boolean existsActiveFileById(@Param("fileId") String fileId);

    /**
     * 查找重复文件（基于文件大小和上传时间）
     */
    @Query(value = "SELECT f1.* FROM file_metadata f1 " +
           "INNER JOIN file_metadata f2 ON f1.file_size = f2.file_size " +
           "WHERE f1.family_id = :familyId AND f2.family_id = :familyId " +
           "AND f1.file_id != f2.file_id AND f1.create_time BETWEEN f2.create_time - INTERVAL '1 minute' AND f2.create_time + INTERVAL '1 minute'",
           nativeQuery = true)
    List<FileMetadata> findDuplicateFiles(@Param("familyId") String familyId);

    /**
     * 获取家庭的文件夹结构
     */
    @Query("SELECT DISTINCT f.folderPath FROM FileMetadata f WHERE f.familyId = :familyId AND f.deleted != 1 " +
           "AND f.folderPath IS NOT NULL ORDER BY f.folderPath")
    List<String> findFolderPathsByFamily(@Param("familyId") String familyId);

    // ==================== 聚合统计查询方法 ====================

    /**
     * 聚合统计家庭文件基础信息
     * <p>
     * 单次查询获取总文件数、总大小、最大文件信息等基础统计数据
     * 避免多次数据库查询，提升性能
     *
     * @param familyId 家庭ID
     * @return 聚合统计结果
     */
    @Query(value = """
        SELECT
            COUNT(f.file_id) as totalFiles,
            COALESCE(SUM(f.file_size), 0) as totalSize,
            COALESCE(MAX(f.file_size), 0) as largestFileSize,
            (SELECT f2.original_name FROM file_metadata f2
             WHERE f2.family_id = :familyId AND f2.deleted != 1
             ORDER BY f2.file_size DESC LIMIT 1) as largestFileName,
            MAX(f.create_time) as mostRecentFileTime
        FROM file_metadata f
        WHERE f.family_id = :familyId AND f.deleted != 1
        """, nativeQuery = true)
    Object[] aggregateBasicStatsByFamily(@Param("familyId") String familyId);

    
    /**
     * 按文件分类聚合统计文件数量
     * <p>
     * 基于文件MIME类型进行分类统计（图片、文档、视频、音频、其他）
     * 使用数据库级分类，减少Java代码处理负担
     *
     * @param familyId 家庭ID
     * @return 分类统计数组 [category, count]
     */
    @Query(value = """
        SELECT
            CASE
                WHEN f.content_type LIKE 'image%' THEN 'image'
                WHEN f.content_type LIKE 'video%' THEN 'video'
                WHEN f.content_type LIKE 'audio%' THEN 'audio'
                WHEN f.content_type LIKE 'application/pdf' OR
                     f.content_type LIKE 'text%' OR
                     f.content_type LIKE 'application/msword' OR
                     f.content_type LIKE 'application/vnd.openxmlformats-officedocument%' THEN 'document'
                ELSE 'others'
            END as category,
            COUNT(f.file_id) as count
        FROM file_metadata f
        WHERE f.family_id = :familyId AND f.deleted != 1
        GROUP BY category
        ORDER BY count DESC
        """, nativeQuery = true)
    List<Object[]> aggregateFilesByCategory(@Param("familyId") String familyId);

    /**
     * 批量获取多个家庭的统计概要
     * <p>
     * 用于管理员视图或批量统计分析
     *
     * @param familyIds 家庭ID列表
     * @return 统计概要数组 [familyId, totalFiles, totalSize]
     */
    @Query("SELECT f.familyId, COUNT(f) as fileCount, COALESCE(SUM(f.fileSize), 0) as totalSize " +
           "FROM FileMetadata f WHERE f.familyId IN :familyIds AND f.deleted != 1 " +
           "GROUP BY f.familyId")
    List<Object[]> batchAggregateStatsByFamilies(@Param("familyIds") List<String> familyIds);
}