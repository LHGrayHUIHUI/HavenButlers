package com.haven.base.repository;

import org.springframework.data.repository.NoRepositoryBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 基础仓储接口
 * 提供标准的数据访问接口定义
 * 具体实现由各个服务的Repository继承
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author HavenButler
 * @version 1.0.0
 */
@NoRepositoryBean
public interface BaseRepository<T, ID> {

    // ==================== 基础CRUD操作 ====================

    /**
     * 保存实体
     */
    T save(T entity);

    /**
     * 根据ID查找实体
     */
    Optional<T> findById(ID id);

    /**
     * 检查实体是否存在
     */
    boolean existsById(ID id);

    /**
     * 查找所有实体
     */
    List<T> findAll();

    /**
     * 根据ID列表查找实体
     */
    List<T> findAllById(Iterable<ID> ids);

    /**
     * 统计实体数量
     */
    long count();

    /**
     * 根据ID删除实体
     */
    void deleteById(ID id);

    /**
     * 删除实体
     */
    void delete(T entity);

    /**
     * 批量删除实体
     */
    void deleteAllById(Iterable<? extends ID> ids);

    /**
     * 删除所有实体
     */
    void deleteAll();

    // ==================== 查询操作 ====================

    /**
     * 根据状态查找实体
     */
    List<T> findByStatus(String status);

    /**
     * 根据状态列表查找实体
     */
    List<T> findByStatusIn(List<String> statuses);

    /**
     * 检查指定状态和ID的实体是否存在
     */
    boolean existsByStatusAndId(String status, ID id);

    /**
     * 统计指定状态的实体数量
     */
    long countByStatus(String status);

    // ==================== 审计相关查询 ====================

    /**
     * 根据创建者查找实体
     */
    List<T> findByCreatedBy(String createdBy);

    /**
     * 根据更新者查找实体
     */
    List<T> findByUpdatedBy(String updatedBy);

    /**
     * 根据创建时间范围查找实体
     */
    List<T> findByCreatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 根据更新时间范围查找实体
     */
    List<T> findByUpdatedTimeBetween(LocalDateTime startTime, LocalDateTime endTime);

    /**
     * 查找指定时间之后创建的实体
     */
    List<T> findByCreatedTimeAfter(LocalDateTime dateTime);

    /**
     * 查找指定时间之后更新的实体
     */
    List<T> findByUpdatedTimeAfter(LocalDateTime dateTime);

    // ==================== 软删除支持（如果实体支持） ====================

    /**
     * 查找未删除的实体
     */
    default List<T> findByActive() {
        // 默认实现：如果实体支持deleted字段，返回未删除的记录
        return findAll();
    }

    /**
     * 检查实体是否存在且未删除
     */
    default boolean existsActiveById(ID id) {
        return existsById(id);
    }

    /**
     * 根据状态查找未删除的实体
     */
    default List<T> findByStatusAndActive(String status) {
        return findByStatus(status);
    }

    /**
     * 根据创建者查找未删除的实体
     */
    default List<T> findByCreatedByAndActive(String createdBy) {
        return findByCreatedBy(createdBy);
    }

    /**
     * 根据创建时间范围查找未删除的实体
     */
    default List<T> findByCreatedTimeBetweenAndActive(LocalDateTime startTime, LocalDateTime endTime) {
        return findByCreatedTimeBetween(startTime, endTime);
    }

    // ==================== 搜索操作 ====================

    /**
     * 根据关键词搜索实体（在常用字段中搜索）
     */
    List<T> searchByKeyword(String keyword);

    /**
     * 根据多个条件查找实体
     */
    List<T> findByConditions(String status, String createdBy);

    // ==================== 统计操作 ====================

    /**
     * 统计未删除的实体数量
     */
    default long countActive() {
        return count();
    }

    /**
     * 根据状态统计未删除的实体数量
     */
    default long countByStatusAndActive(String status) {
        return countByStatus(status);
    }
}