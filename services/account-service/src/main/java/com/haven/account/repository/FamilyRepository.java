package com.haven.account.repository;

import com.haven.account.entity.Family;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 家庭数据访问接口
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {

    /**
     * 根据所有者ID查找家庭
     */
    List<Family> findByOwnerId(Long ownerId);

    /**
     * 根据名称查找家庭
     */
    List<Family> findByNameContainingIgnoreCase(String name);

    /**
     * 检查家庭名称是否已存在（排除指定ID）
     */
    @Query("SELECT COUNT(f) > 0 FROM Family f WHERE f.name = :name AND f.id != :excludeId")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("excludeId") Long excludeId);

    /**
     * 根据状态查找家庭
     */
    List<Family> findByStatus(String status);

    /**
     * 查找用户参与的所有家庭
     */
    @Query("SELECT f FROM Family f WHERE f.id IN " +
           "(SELECT fm.familyId FROM FamilyMember fm WHERE fm.userId = :userId AND fm.status = 'ACTIVE')")
    List<Family> findFamiliesByUserId(@Param("userId") Long userId);

    /**
     * 查找用户作为所有者的家庭
     */
    @Query("SELECT f FROM Family f WHERE f.ownerId = :userId AND f.status = 'ACTIVE'")
    Optional<Family> findOwnerFamilyByUserId(@Param("userId") Long userId);

    /**
     * 搜索家庭（支持名称、描述模糊查询）
     */
    @Query("SELECT f FROM Family f WHERE " +
           "LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Family> searchFamilies(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 统计家庭总数
     */
    @Query("SELECT COUNT(f) FROM Family f WHERE f.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * 查找最近创建的家庭
     */
    @Query("SELECT f FROM Family f ORDER BY f.createdAt DESC")
    List<Family> findRecentlyCreatedFamilies(Pageable pageable);

    /**
     * 根据成员数量筛选家庭
     */
    @Query("SELECT f FROM Family f WHERE " +
           "(SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.familyId = f.id AND fm.status = 'ACTIVE') = :memberCount")
    List<Family> findFamiliesByMemberCount(@Param("memberCount") int memberCount);
}