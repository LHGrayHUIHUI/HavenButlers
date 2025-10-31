package com.haven.account.repository;

import com.haven.account.model.entity.User;
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
 * 用户数据访问接口
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);

    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);

    /**
     * 根据手机号查找用户
     */
    Optional<User> findByPhone(String phone);

    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);

    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);

    /**
     * 检查手机号是否存在
     */
    boolean existsByPhone(String phone);

    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(String status);

    /**
     * 根据角色查找用户
     */
    @Query("SELECT u FROM User u WHERE u.roles LIKE %:role%")
    List<User> findByRoleContaining(@Param("role") String role);

    /**
     * 查找在指定时间之后注册的用户
     */
    @Query("SELECT u FROM User u WHERE u.createTime >= :since")
    List<User> findUsersRegisteredSince(@Param("since") LocalDateTime since);

    /**
     * 搜索用户（支持用户名、邮箱模糊查询）
     */
    @Query("SELECT u FROM User u WHERE " +
           "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<User> searchUsers(@Param("keyword") String keyword, Pageable pageable);

    /**
     * 根据家庭ID查找用户成员
     */
    @Query("SELECT u FROM User u WHERE u.currentFamilyId = :familyId")
    List<User> findUsersByFamilyId(@Param("familyId") Long familyId);

    /**
     * 统计用户总数
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") String status);

    /**
     * 查找最近登录的用户
     */
    @Query("SELECT u FROM User u WHERE u.updateTime >= :since ORDER BY u.updateTime DESC")
    List<User> findRecentlyActiveUsers(@Param("since") LocalDateTime since, Pageable pageable);
}