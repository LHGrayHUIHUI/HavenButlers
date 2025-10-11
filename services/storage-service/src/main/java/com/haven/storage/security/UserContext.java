package com.haven.storage.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 用户上下文工具类
 *
 * 从网关注入的请求头中获取用户信息
 * 提供线程安全的用户信息访问方法
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class UserContext {

    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String FAMILY_ID_HEADER = "X-Family-ID";
    private static final String USER_NAME_HEADER = "X-User-Name";

    /**
     * 获取当前用户ID
     */
    public static String getCurrentUserId() {
        return getHeader(USER_ID_HEADER);
    }

    /**
     * 获取当前家庭ID
     */
    public static String getCurrentFamilyId() {
        return getHeader(FAMILY_ID_HEADER);
    }

    /**
     * 获取当前用户名
     */
    public static String getCurrentUserName() {
        return getHeader(USER_NAME_HEADER);
    }

    /**
     * 检查当前用户是否已认证
     */
    public static boolean isAuthenticated() {
        String userId = getCurrentUserId();
        return userId != null && !userId.trim().isEmpty();
    }

    /**
     * 构建用户信息摘要（用于日志）
     */
    public static String getUserSummary() {
        StringBuilder summary = new StringBuilder();

        String userId = getCurrentUserId();
        if (userId != null) {
            summary.append("userId=").append(userId);
        }

        String familyId = getCurrentFamilyId();
        if (familyId != null) {
            summary.append(", familyId=").append(familyId);
        }

        String userName = getCurrentUserName();
        if (userName != null) {
            summary.append(", userName=").append(userName);
        }

        return summary.toString();
    }

    /**
     * 从请求头获取指定字段值
     */
    private static String getHeader(String headerName) {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String value = request.getHeader(headerName);
                if (value != null && !value.trim().isEmpty()) {
                    return value;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("获取请求头失败: header={}", headerName, e);
            return null;
        }
    }

    /**
     * 获取当前请求对象
     */
    private static HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.error("获取当前请求失败", e);
            return null;
        }
    }
}