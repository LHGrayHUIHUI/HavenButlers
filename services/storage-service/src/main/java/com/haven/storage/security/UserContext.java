package com.haven.storage.security;

import com.haven.base.common.constants.SystemConstants;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * 用户上下文工具类 - 优化版本
 * <p>
 * 核心功能：
 * - 线程安全的用户信息管理
 * - 支持同步和异步场景
 * - 自动清理机制防止内存泄漏
 * - 增强的安全性验证
 * - 丰富的上下文操作方法
 * - 与充血模型完美集成
 *
 * @author HavenButler
 * @version 2.0
 */
@Slf4j
@Component
public final class UserContext {

    // ==================== 常量定义 ====================
    public static final String USER_ID_HEADER = SystemConstants.USER_ID_HEADER;
    public static final String FAMILY_ID_HEADER = SystemConstants.FAMILY_ID_HEADER;
    public static final String USER_NAME_HEADER = SystemConstants.USER_NAME_HEADER;
    public static final String USER_ROLE_HEADER = SystemConstants.USER_ROLE_HEADER;
    public static final String USER_PERMISSIONS_HEADER = SystemConstants.USER_PERMISSIONS_HEADER;
    public static final String TRACE_ID_HEADER = SystemConstants.TRACE_ID_HEADER;

    // ==================== 线程安全存储 ====================
    private static final ThreadLocal<UserInfo> USER_INFO_HOLDER = new InheritableThreadLocal<>();
    private static final ThreadLocal<String> TRACE_ID_HOLDER = new InheritableThreadLocal<>();

    // 降级加载标记（用于统计和监控）
    private static final ThreadLocal<Boolean> FALLBACK_LOADED = new ThreadLocal<>();

    // 私有构造函数，防止实例化
    private UserContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // ==================== 核心访问方法 ====================

    /**
     * 获取当前用户信息（推荐使用）
     * 优先从ThreadLocal获取
     *
     * 注意：正常情况下应该由 UserContextInterceptor 在请求开始时设置用户上下文
     * 只有在拦截器未覆盖的场景（如 WebSocket、消息队列等）才需要自动加载
     */
    public static Optional<UserInfo> getCurrentUserInfo() {
        UserInfo userInfo = USER_INFO_HOLDER.get();
        if (userInfo != null) {
            return Optional.of(userInfo);
        }

        // 降级方案：尝试从当前请求加载（仅用于拦截器未覆盖的场景）
        Optional<UserInfo> fallbackUserInfo = loadFromCurrentRequest();
        fallbackUserInfo.ifPresent(info -> {
            log.warn("用户上下文未在拦截器中设置，使用降级方案从请求头加载: {}",
                    info.toSummaryString());
            USER_INFO_HOLDER.set(info); // 设置到 ThreadLocal，避免重复加载
        });

        return fallbackUserInfo;
    }

    /**
     * 获取当前用户信息（必须存在，否则抛出异常）
     * 用于必须认证的场景
     */
    public static UserInfo requireCurrentUserInfo() {
        return getCurrentUserInfo()
                .orElseThrow(() -> new IllegalStateException("用户未认证，无法获取用户信息"));
    }

    /**
     * 获取当前用户ID
     */
    @Nullable
    public static String getCurrentUserId() {
        return getCurrentUserInfo().map(UserInfo::userId).orElse(null);
    }

    /**
     * 获取当前用户ID（必须存在）
     */
    public static String requireCurrentUserId() {
        return requireCurrentUserInfo().userId();
    }

    /**
     * 获取当前家庭ID
     */
    @Nullable
    public static String getCurrentFamilyId() {
        return getCurrentUserInfo().map(UserInfo::familyId).orElse(null);
    }

    /**
     * 获取当前家庭ID（必须存在）
     */
    public static String requireCurrentFamilyId() {
        String familyId = getCurrentFamilyId();
        if (familyId == null) {
            throw new IllegalStateException("当前用户未关联家庭");
        }
        return familyId;
    }

    /**
     * 获取当前用户名
     */
    @Nullable
    public static String getCurrentUserName() {
        return getCurrentUserInfo().map(UserInfo::userName).orElse(null);
    }

    /**
     * 获取当前TraceID
     */
    @Nullable
    public static String getCurrentTraceId() {
        return TRACE_ID_HOLDER.get();
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUserInfo().map(UserInfo::hasValidUserId).orElse(false);
    }

    /**
     * 检查用户是否有权限访问指定家庭
     */
    public static boolean hasFamilyAccess(String targetFamilyId) {
        return getCurrentUserInfo()
                .map(userInfo -> userInfo.belongsToFamily(targetFamilyId))
                .orElse(false);
    }

    /**
     * 检查当前用户是否为指定用户
     */
    public static boolean isCurrentUser(String userId) {
        return getCurrentUserInfo()
                .map(userInfo -> userInfo.userId().equals(userId))
                .orElse(false);
    }

    /**
     * 获取用户信息摘要（用于日志）
     */
    public static String getUserSummary() {
        return getCurrentUserInfo()
                .map(UserInfo::toSummaryString)
                .orElse("No authenticated user");
    }

    // ==================== ThreadLocal 管理方法 ====================

    /**
     * 设置用户信息到当前线程上下文
     */
    public static void setCurrentUser(UserInfo userInfo) {
        if (userInfo != null && userInfo.hasValidUserId()) {
            USER_INFO_HOLDER.set(userInfo);
            log.debug("设置用户上下文: {}", userInfo.toSummaryString());
        } else {
            log.warn("尝试设置无效的用户信息: {}", userInfo);
        }
    }

    /**
     * 设置用户信息（仅userId）
     */
    public static void setCurrentUser(String userId) {
        setCurrentUser(UserInfo.of(userId));
    }

    /**
     * 设置用户信息（userId + familyId）
     */
    public static void setCurrentUser(String userId, String familyId) {
        setCurrentUser(UserInfo.of(userId, familyId));
    }

    /**
     * 设置TraceID到当前线程上下文
     */
    public static void setCurrentTraceId(String traceId) {
        if (StringUtils.hasText(traceId)) {
            TRACE_ID_HOLDER.set(traceId);
            log.debug("设置TraceID: {}", traceId);
        }
    }

    /**
     * 清理当前线程上下文（防止内存泄漏）
     * 必须在请求结束时调用
     */
    public static void clear() {
        UserInfo userInfo = USER_INFO_HOLDER.get();
        if (userInfo != null) {
            log.debug("清理用户上下文: {}", userInfo.toSummaryString());
        }

        USER_INFO_HOLDER.remove();
        TRACE_ID_HOLDER.remove();
    }

    /**
     * 强制清理（用于异常场景）
     */
    public static void forceClear() {
        USER_INFO_HOLDER.remove();
        TRACE_ID_HOLDER.remove();
    }

    // ==================== 异步支持方法 ====================

    /**
     * 在异步任务中传递用户上下文
     */
    public static <T> CompletableFuture<T> runWithUserContext(Supplier<T> task) {
        UserInfo currentUserInfo = USER_INFO_HOLDER.get();
        String currentTraceId = TRACE_ID_HOLDER.get();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // 在异步线程中设置上下文
                if (currentUserInfo != null) {
                    USER_INFO_HOLDER.set(currentUserInfo);
                }
                if (currentTraceId != null) {
                    TRACE_ID_HOLDER.set(currentTraceId);
                }

                return task.get();
            } finally {
                // 清理异步线程的上下文
                USER_INFO_HOLDER.remove();
                TRACE_ID_HOLDER.remove();
            }
        });
    }

    /**
     * 在异步任务中传递用户上下文（无返回值）
     */
    public static CompletableFuture<Void> runWithUserContext(Runnable task) {
        return runWithUserContext(() -> {
            task.run();
            return null;
        });
    }

    /**
     * 带用户上下文的Supplier包装器
     */
    public static <T> Supplier<T> withUserContext(Supplier<T> supplier) {
        UserInfo currentUserInfo = USER_INFO_HOLDER.get();
        String currentTraceId = TRACE_ID_HOLDER.get();

        return () -> {
            UserInfo originalUser = USER_INFO_HOLDER.get();
            String originalTrace = TRACE_ID_HOLDER.get();

            try {
                USER_INFO_HOLDER.set(currentUserInfo);
                TRACE_ID_HOLDER.set(currentTraceId);
                return supplier.get();
            } finally {
                USER_INFO_HOLDER.set(originalUser);
                TRACE_ID_HOLDER.set(originalTrace);
            }
        };
    }

    /**
     * 带用户上下文的Runnable包装器
     */
    public static Runnable withUserContext(Runnable runnable) {
        UserInfo currentUserInfo = USER_INFO_HOLDER.get();
        String currentTraceId = TRACE_ID_HOLDER.get();

        return () -> {
            UserInfo originalUser = USER_INFO_HOLDER.get();
            String originalTrace = TRACE_ID_HOLDER.get();

            try {
                USER_INFO_HOLDER.set(currentUserInfo);
                TRACE_ID_HOLDER.set(currentTraceId);
                runnable.run();
            } finally {
                USER_INFO_HOLDER.set(originalUser);
                TRACE_ID_HOLDER.set(originalTrace);
            }
        };
    }

    /**
     * 带用户上下文的Function包装器
     */
    public static <T, R> Function<T, R> withUserContext(Function<T, R> function) {
        UserInfo currentUserInfo = USER_INFO_HOLDER.get();
        String currentTraceId = TRACE_ID_HOLDER.get();

        return (input) -> {
            UserInfo originalUser = USER_INFO_HOLDER.get();
            String originalTrace = TRACE_ID_HOLDER.get();

            try {
                USER_INFO_HOLDER.set(currentUserInfo);
                TRACE_ID_HOLDER.set(currentTraceId);
                return function.apply(input);
            } finally {
                USER_INFO_HOLDER.set(originalUser);
                TRACE_ID_HOLDER.set(originalTrace);
            }
        };
    }

    // ==================== 请求头处理方法 ====================

    /**
     * 从HTTP请求加载用户信息
     */
    public static Optional<UserInfo> loadFromRequest(HttpServletRequest request) {
        if (request == null) {
            return Optional.empty();
        }

        String userId = extractHeader(request, USER_ID_HEADER);
        if (!StringUtils.hasText(userId)) {
            log.debug("请求头中未找到用户ID");
            return Optional.empty();
        }

        String familyId = extractHeader(request, FAMILY_ID_HEADER);
        String userName = extractHeader(request, USER_NAME_HEADER);

        try {
            UserInfo userInfo = new UserInfo(userId, familyId, userName);
            log.debug("从请求头加载用户信息: {}", userInfo.toSummaryString());
            return Optional.of(userInfo);
        } catch (IllegalArgumentException e) {
            log.warn("用户信息格式无效: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 从当前请求加载并设置用户上下文
     */
    public static boolean loadAndSetFromCurrentRequest() {
        return loadFromCurrentRequest()
                .map(userInfo -> {
                    setCurrentUser(userInfo);
                    return true;
                })
                .orElse(false);
    }

    /**
     * 从当前请求加载用户信息（降级方案）
     *
     * 仅用于以下场景：
     * 1. WebSocket 连接
     * 2. 某些特殊的过滤器/拦截器执行顺序问题
     * 3. 测试环境中模拟请求
     *
     * 正常业务请求应该已经由 UserContextInterceptor 设置了用户上下文
     */
    private static Optional<UserInfo> loadFromCurrentRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return Optional.empty();
        }

        return loadFromRequest(request);
    }

    /**
     * 提取并验证请求头
     */
    private static String extractHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 获取当前HTTP请求对象
     *
     * 注意：此方法仅作为降级方案使用
     * 正常情况下应该由拦截器在请求开始时设置用户上下文
     */
    @Nullable
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (IllegalStateException e) {
            log.debug("不在HTTP请求上下文中: {}", e.getMessage());
            return null;
        }
    }

    // ==================== 安全验证方法 ====================

    /**
     * 验证当前用户是否已认证
     *
     * @throws IllegalStateException 如果用户未认证
     */
    public static void requireAuthentication() {
        if (!isAuthenticated()) {
            throw new IllegalStateException("用户未认证");
        }
    }

    /**
     * 验证当前用户是否有权限访问指定家庭
     *
     * @throws SecurityException 如果用户无权限
     */
    public static void requireFamilyAccess(String targetFamilyId) {
        requireAuthentication();

        if (!hasFamilyAccess(targetFamilyId)) {
            log.warn("用户无权访问指定家庭: current={}, required={}",
                    getCurrentFamilyId(), targetFamilyId);
            throw new SecurityException("无权访问指定家庭资源");
        }
    }

    /**
     * 验证当前用户是否为指定用户
     *
     * @throws SecurityException 如果不是指定用户
     */
    public static void requireUser(String userId) {
        requireAuthentication();

        if (!isCurrentUser(userId)) {
            log.warn("用户身份验证失败: current={}, required={}",
                    getCurrentUserId(), userId);
            throw new SecurityException("无权执行该操作");
        }
    }

    /**
     * 验证当前用户是否有权限执行操作
     */
    public static boolean validateUserPermission(String resourceFamilyId, String requiredPermission) {
        if (!isAuthenticated()) {
            log.warn("用户未认证，拒绝访问");
            return false;
        }

        if (!hasFamilyAccess(resourceFamilyId)) {
            log.warn("用户无权访问指定家庭: current={}, required={}",
                    getCurrentFamilyId(), resourceFamilyId);
            return false;
        }

        // 这里可以扩展更多权限验证逻辑
        return true;
    }

    /**
     * 安全地执行需要用户认证的操作
     */
    public static <T> Optional<T> executeWithAuth(Supplier<T> operation) {
        if (!isAuthenticated()) {
            log.warn("用户未认证，操作被拒绝");
            return Optional.empty();
        }

        try {
            return Optional.ofNullable(operation.get());
        } catch (Exception e) {
            log.error("执行认证操作时发生异常", e);
            return Optional.empty();
        }
    }

    /**
     * 执行需要认证的操作（抛出异常而非返回Optional）
     */
    public static <T> T executeRequiringAuth(Supplier<T> operation) {
        requireAuthentication();
        return operation.get();
    }

    /**
     * 在临时用户上下文中执行操作
     */
    public static <T> T executeAsUser(UserInfo tempUser, Supplier<T> operation) {
        UserInfo originalUser = USER_INFO_HOLDER.get();
        String originalTrace = TRACE_ID_HOLDER.get();

        try {
            USER_INFO_HOLDER.set(tempUser);
            log.debug("临时切换用户上下文: {}", tempUser.toSummaryString());
            return operation.get();
        } finally {
            USER_INFO_HOLDER.set(originalUser);
            TRACE_ID_HOLDER.set(originalTrace);
            log.debug("恢复原用户上下文");
        }
    }

    /**
     * 在临时用户上下文中执行操作（无返回值）
     */
    public static void executeAsUser(UserInfo tempUser, Runnable operation) {
        executeAsUser(tempUser, () -> {
            operation.run();
            return null;
        });
    }

    /**
     * 使用当前用户信息执行操作（如果存在）
     */
    public static <T> Optional<T> ifAuthenticated(Function<UserInfo, T> operation) {
        return getCurrentUserInfo().map(operation);
    }

    /**
     * 使用当前用户信息执行操作（如果存在，无返回值）
     */
    public static void ifAuthenticated(Consumer<UserInfo> operation) {
        getCurrentUserInfo().ifPresent(operation);
    }

    // ==================== 调试和监控方法 ====================

    /**
     * 获取当前上下文状态信息（用于调试）
     */
    public static String getContextStatus() {
        UserInfo userInfo = USER_INFO_HOLDER.get();
        String traceId = TRACE_ID_HOLDER.get();

        return String.format("UserContext{user=%s, traceId=%s, thread=%s}",
                userInfo != null ? userInfo.toSummaryString() : "null",
                traceId != null ? traceId : "null",
                Thread.currentThread().getName());
    }

    /**
     * 检查ThreadLocal是否存在内存泄漏风险
     */
    public static boolean checkMemoryLeakRisk() {
        return USER_INFO_HOLDER.get() != null || TRACE_ID_HOLDER.get() != null;
    }

    /**
     * 获取ThreadLocal中的用户信息（不自动加载）
     */
    public static Optional<UserInfo> getFromThreadLocal() {
        return Optional.ofNullable(USER_INFO_HOLDER.get());
    }

    /**
     * 打印当前上下文状态（用于调试）
     */
    public static void printContextStatus() {
        log.info("=== User Context Status ===");
        log.info("Context: {}", getContextStatus());
        log.info("Authenticated: {}", isAuthenticated());
        log.info("Memory Leak Risk: {}", checkMemoryLeakRisk());
        log.info("===========================");
    }
}