package com.haven.base.service;

import com.haven.base.cache.CacheService;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.exception.BaseException;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.lang.reflect.Method;

/**
 * 基础服务类
 * 提供通用的异常处理、缓存管理等功能
 * 具体的CRUD操作由子类通过Repository实现
 *
 * @param <T> 实体类型
 * @param <ID> 主键类型
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component
public abstract class BaseService<T, ID> {

    @Autowired(required = false)
    protected CacheService cacheService;

    /**
     * 获取缓存键前缀（子类可重写）
     */
    protected String getCachePrefix() {
        return this.getClass().getSimpleName() + ":";
    }

    /**
     * 获取缓存TTL（子类可重写）
     */
    protected long getCacheTTL() {
        return 3600L; // 1小时
    }

    // ==================== 通用操作模板 ====================

    /**
     * 执行操作并统一异常处理
     */
    protected <R> R executeWithLogging(String operation, Supplier<R> supplier) {
        String traceId = TraceIdUtil.getCurrent();
        try {
            log.debug("开始执行操作: operation={}, traceId={}", operation, traceId);
            R result = supplier.get();
            log.debug("操作执行成功: operation={}, traceId={}", operation, traceId);
            return result;
        } catch (Exception e) {
            log.error("操作执行失败: operation={}, traceId={}, error={}", operation, traceId, e.getMessage(), e);

            if (e instanceof BaseException) {
                throw (BaseException) e;
            } else if (e instanceof IllegalArgumentException) {
                throw new BaseException(ErrorCode.SYSTEM_ERROR.getCode(), e.getMessage());
            } else {
                throw new BaseException(ErrorCode.SYSTEM_ERROR.getCode(), "操作失败: " + e.getMessage());
            }
        }
    }

    /**
     * 执行无返回值操作并统一异常处理
     */
    protected void executeVoidWithLogging(String operation, Runnable runnable) {
        executeWithLogging(operation, () -> {
            runnable.run();
            return null;
        });
    }

    /**
     * 反射调用Repository方法
     */
    @SuppressWarnings("unchecked")
    protected <R> R invokeRepositoryMethod(String methodName, Object... args) {
        try {
            Object repository = getRepository();
            Class<?>[] paramTypes = new Class[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
                // 处理基本类型
                if (args[i] instanceof Integer) paramTypes[i] = int.class;
                else if (args[i] instanceof Long) paramTypes[i] = long.class;
                else if (args[i] instanceof Boolean) paramTypes[i] = boolean.class;
            }

            Method method = repository.getClass().getMethod(methodName, paramTypes);
            return (R) method.invoke(repository, args);
        } catch (Exception e) {
            throw new RuntimeException("反射调用Repository方法失败: " + methodName, e);
        }
    }

    // ==================== 缓存操作 ====================

    /**
     * 获取缓存键
     */
    protected String getCacheKey(ID id) {
        return getCachePrefix() + id;
    }

    /**
     * 从缓存获取实体
     */
    @SuppressWarnings("unchecked")
    protected Optional<T> getFromCache(ID id) {
        if (cacheService == null) {
            return Optional.empty();
        }

        String cacheKey = getCacheKey(id);
        Optional<T> cached = cacheService.get(cacheKey, getEntityType());
        return cached;
    }

    /**
     * 将实体放入缓存
     */
    protected void putToCache(ID id, T entity) {
        if (cacheService != null) {
            String cacheKey = getCacheKey(id);
            Duration ttl = Duration.ofSeconds(getCacheTTL());
            cacheService.set(cacheKey, entity, ttl);
        }
    }

    /**
     * 从缓存删除实体
     */
    protected void removeFromCache(ID id) {
        if (cacheService != null) {
            String cacheKey = getCacheKey(id);
            cacheService.delete(cacheKey);
        }
    }

    /**
     * 清除所有相关缓存
     */
    protected void clearCacheIfAvailable() {
        // 简化实现，后续可根据具体缓存服务扩展
        logDebug("清除缓存", "缓存功能需要具体实现");
    }

    // ==================== 日志记录 ====================

    /**
     * 记录操作日志
     */
    protected void logOperation(String operation, Object... params) {
        String traceId = TraceIdUtil.getCurrent();
        log.info("操作日志: operation={}, params={}, traceId={}", operation, params, traceId);
    }

    /**
     * 记录调试信息
     */
    protected void logDebug(String operation, String message) {
        String traceId = TraceIdUtil.getCurrent();
        log.debug("调试信息: operation={}, message={}, traceId={}", operation, message, traceId);
    }

    /**
     * 记录警告信息
     */
    protected void logWarn(String operation, String message) {
        String traceId = TraceIdUtil.getCurrent();
        log.warn("警告信息: operation={}, message={}, traceId={}", operation, message, traceId);
    }

    /**
     * 记录错误信息
     */
    protected void logError(String operation, String message, Throwable throwable) {
        String traceId = TraceIdUtil.getCurrent();
        log.error("错误信息: operation={}, message={}, traceId={}", operation, message, traceId, throwable);
    }

    // ==================== 工具方法 ====================

    /**
     * 检查字符串是否为空
     */
    protected boolean isEmpty(String str) {
        return !StringUtils.hasText(str);
    }

    /**
     * 检查集合是否为空
     */
    protected boolean isEmpty(List<?> list) {
        return list == null || list.isEmpty();
    }

    /**
     * 检查对象是否为空
     */
    protected boolean isEmpty(Object obj) {
        return obj == null;
    }

    /**
     * 验证参数不为空
     */
    protected void requireNonNull(Object obj, String message) {
        if (obj == null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 验证字符串不为空
     */
    protected void requireNonEmpty(String str, String message) {
        if (!StringUtils.hasText(str)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 获取实体类型（用于缓存）
     */
    @SuppressWarnings("unchecked")
    private Class<T> getEntityType() {
        // 通过反射获取泛型类型
        try {
            java.lang.reflect.ParameterizedType superClass =
                (java.lang.reflect.ParameterizedType) getClass().getGenericSuperclass();
            return (Class<T>) superClass.getActualTypeArguments()[0];
        } catch (Exception e) {
            log.warn("无法获取实体类型，缓存功能可能受影响", e);
            return null;
        }
    }

    /**
     * 获取Repository实例（子类需要实现）
     */
    protected abstract Object getRepository();
}