package com.haven.storage.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.util.StringUtils;

import java.util.Optional;

/**
 * 用户上下文工具类
 *
 * 从网关注入的请求头中获取用户信息。
 * 建议在请求前置处理器中将用户信息加载到 ThreadLocal 中，确保线程安全。
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class UserContext {

    // ------------------ 定义常量 ------------------
    public static final String USER_ID_HEADER = "X-User-ID";
    public static final String FAMILY_ID_HEADER = "X-Family-ID";
    public static final String USER_NAME_HEADER = "X-User-Name";

    // ------------------ 线程安全存储 ------------------
    /**
     * 使用 ThreadLocal 存储用户信息，确保线程安全，特别是在异步或线程池环境下。
     */
    private static final ThreadLocal<UserInfo> USER_INFO_HOLDER = new ThreadLocal<>();

    // ------------------ 公共访问方法 ------------------

    /**
     * 获取当前完整的用户信息对象
     * 推荐使用此方法获取信息，以避免多次访问请求头
     */
    public static Optional<UserInfo> getCurrentUserInfo() {
        // 尝试从 ThreadLocal 获取
        UserInfo userInfo = USER_INFO_HOLDER.get();
        if (userInfo != null) {
            return Optional.of(userInfo);
        }

        // 如果 ThreadLocal 中没有，则尝试从当前请求头加载 (仅限同步调用)
        return loadUserInfoFromRequest();
    }

    /**
     * 获取当前用户ID
     */
    @Nullable
    public static String getCurrentUserId() {
        return getCurrentUserInfo().map(UserInfo::userId).orElse(null);
    }

    /**
     * 获取当前家庭ID
     */
    @Nullable
    public static String getCurrentFamilyId() {
        return getCurrentUserInfo().map(UserInfo::familyId).orElse(null);
    }

    /**
     * 获取当前用户名
     */
    @Nullable
    public static String getCurrentUserName() {
        return getCurrentUserInfo().map(UserInfo::userName).orElse(null);
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        return getCurrentUserInfo().isEmpty() ||
                !StringUtils.hasText(getCurrentUserInfo().get().userId());
    }

    /**
     * 构建用户信息摘要（用于日志）
     */
    public static String getUserSummary() {
        return getCurrentUserInfo()
                .map(UserInfo::toSummaryString)
                .orElse("No authenticated user.");
    }

    // ------------------ ThreadLocal 管理方法 ------------------

    /**
     * (在请求前置处理器/Filter中调用) 存储用户信息到 ThreadLocal
     */
    public static void set(UserInfo userInfo) {
        if (userInfo != null) {
            USER_INFO_HOLDER.set(userInfo);
        }
    }

    /**
     * (在请求后置处理器/Filter中调用) 清理 ThreadLocal，防止内存泄漏和线程污染
     */
    public static void clear() {
        USER_INFO_HOLDER.remove();
    }


    // ------------------ 内部辅助方法 ------------------

    /**
     * 从当前请求头中加载用户信息，并创建 UserInfo 对象
     */
    private static Optional<UserInfo> loadUserInfoFromRequest() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return Optional.empty();
        }

        String userId = getHeader(request, USER_ID_HEADER);
        String familyId = getHeader(request, FAMILY_ID_HEADER);
        String userName = getHeader(request, USER_NAME_HEADER);

        if (StringUtils.hasText(userId)) {
            UserInfo userInfo = new UserInfo(userId, familyId, userName);
            // 首次加载后可以存储到 ThreadLocal，但更推荐在 Filter 中统一处理
            // USER_INFO_HOLDER.set(userInfo);
            return Optional.of(userInfo);
        }

        return Optional.empty();
    }

    /**
     * 从请求头获取指定字段值
     */
    @Nullable
    private static String getHeader(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    /**
     * 获取当前请求对象 (依赖 Spring 的 RequestContextHolder)
     */
    @Nullable
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (IllegalStateException e) {
            // 捕获不在 HTTP 请求上下文中的异常，如异步线程
            return null;
        }
    }

  }