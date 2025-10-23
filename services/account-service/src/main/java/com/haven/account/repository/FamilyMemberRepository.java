package com.haven.account.repository;

import com.haven.account.entity.FamilyMember;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 家庭成员数据访问接口
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Repository
public interface FamilyMemberRepository extends JpaRepository<FamilyMember, Long> {

    /**
     * 根据家庭ID查找所有成员
     */
    List<FamilyMember> findByFamilyId(Long familyId);

    /**
     * 根据用户ID查找所有家庭成员关系
     */
    List<FamilyMember> findByUserId(Long userId);

    /**
     * 查找用户在指定家庭中的成员关系
     */
    Optional<FamilyMember> findByFamilyIdAndUserId(Long familyId, Long userId);

    /**
     * 查找用户在所有活跃家庭中的成员关系
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.userId = :userId AND fm.status = 'ACTIVE'")
    List<FamilyMember> findActiveMembershipsByUserId(@Param("userId") Long userId);

    /**
     * 根据家庭ID和角色查找成员
     */
    List<FamilyMember> findByFamilyIdAndRole(Long familyId, String role);

    /**
     * 查找家庭管理员
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.familyId = :familyId AND fm.role = 'ADMIN' AND fm.status = 'ACTIVE'")
    List<FamilyMember> findFamilyAdmins(@Param("familyId") Long familyId);

    /**
     * 统计家庭活跃成员数量
     */
    @Query("SELECT COUNT(fm) FROM FamilyMember fm WHERE fm.familyId = :familyId AND fm.status = 'ACTIVE'")
    long countActiveMembersByFamilyId(@Param("familyId") Long familyId);

    /**
     * 查找指定角色的家庭成员
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.role = :role AND fm.status = 'ACTIVE'")
    Page<FamilyMember> findMembersByRole(@Param("role") String role, Pageable pageable);

    /**
     * 检查用户是否为家庭管理员
     */
    @Query("SELECT COUNT(fm) > 0 FROM FamilyMember fm WHERE fm.familyId = :familyId AND fm.userId = :userId AND fm.role = 'ADMIN' AND fm.status = 'ACTIVE'")
    boolean isFamilyAdmin(@Param("familyId") Long familyId, @Param("userId") Long userId);

    /**
     * 查找最近加入的成员
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.familyId = :familyId ORDER BY fm.joinedAt DESC")
    List<FamilyMember> findRecentlyJoinedMembers(@Param("familyId") Long familyId, Pageable pageable);

    /**
     * 查找需要权限验证的成员关系
     */
    @Query("SELECT fm FROM FamilyMember fm WHERE fm.status = 'ACTIVE' AND " +
           "(fm.familyId = :familyId OR fm.userId = :userId)")
    List<FamilyMember> findMembersForPermissionCheck(@Param("familyId") Long familyId, @Param("userId") Long userId);
}