package com.haven.account.security;

import com.haven.base.utils.TraceIdUtil;
import com.haven.base.common.constants.SystemConstants;
import com.haven.base.security.JwtUtils;
import com.haven.base.web.filter.AbstractAuthFilter;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 账户服务专用认证过滤器
 * 继承AbstractAuthFilter，提供账户服务特定的认证逻辑
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@Component("accountAuthFilter")
public class AccountAuthFilter extends AbstractAuthFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    protected List<String> getCustomWhiteListPaths() {
        // 账户服务特定的白名单路径
        return List.of(
            "/api/v1/auth/register",           // 用户注册
            "/api/v1/auth/login",              // 用户登录
            "/api/v1/auth/validate",           // Token验证
            "/api/v1/auth/refresh",            // Token刷新
            "/api/v1/auth/forgot-password",    // 忘记密码
            "/api/v1/auth/reset-password",     // 重置密码
            "/api/v1/health",                  // 健康检查
            "/actuator/**",                    // 监控端点
            "/swagger-ui/**",                  // API文档
            "/v3/api-docs/**"                  // OpenAPI文档
        );
    }

    @Override
    protected List<String> getDefaultInterceptPaths() {
        // 账户服务默认拦截的路径
        return List.of(
            "/api/v1/**"          // 拦截所有v1版本的API
        );
    }

    @Override
    protected List<String> getAdditionalInterceptPaths() {
        // 额外需要拦截的路径
        return List.of(
            "/api/**"              // 拦截其他版本的API（如果有）
        );
    }

    @Override
    protected boolean shouldAuthenticate(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        // OPTIONS请求直接通过（CORS预检）
        if ("OPTIONS".equals(method)) {
            return false;
        }

        // 静态资源不认证
        if (uri.startsWith("/static/") || uri.startsWith("/public/") ||
            uri.endsWith(".css") || uri.endsWith(".js") || uri.endsWith(".png") ||
            uri.endsWith(".jpg") || uri.endsWith(".gif") || uri.endsWith(".ico")) {
            return false;
        }

        // 特定的健康检查端点
        if (uri.equals("/health") || uri.startsWith("/actuator/")) {
            return false;
        }

        return true;
    }

    @Override
    protected String getTokenFromRequest(HttpServletRequest request) {
        // 账户服务特定的Token获取逻辑

        // 1. 优先从Authorization Header获取（Bearer格式）- 标准方式
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 2. 从X-Auth-Token获取 - 兼容方式
        String token = request.getHeader(CommonConstants.Header.TOKEN);
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }

        // 3. 从X-Access-Token获取 - 账户服务专用
        String accessToken = request.getHeader("X-Access-Token");
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            return accessToken.trim();
        }

        // 4. 从请求参数获取（仅用于测试和调试）
        return request.getParameter("token");
    }

    @Override
    protected void setUserContext(HttpServletRequest request, Claims claims, String token) {
        // 调用父类的基础实现
        super.setUserContext(request, claims, token);

        // 添加账户服务特定的用户上下文信息
        String userId = claims.getSubject();
        String[] roles = jwtUtils.getRolesFromToken(token);
        String tenant = jwtUtils.getTenantFromToken(token);

        // 设置账户服务特定的上下文属性
        request.setAttribute("account.userId", userId);
        request.setAttribute("account.roles", roles);
        request.setAttribute("account.familyId", tenant);
        request.setAttribute("account.token", token);

        // 设置用户类型（如果存在）
        String userType = claims.get("userType", String.class);
        if (userType != null) {
            request.setAttribute("account.userType", userType);
        }

        // 设置当前家庭ID（如果存在）
        String currentFamilyId = claims.get("currentFamilyId", String.class);
        if (currentFamilyId != null) {
            request.setAttribute("account.currentFamilyId", currentFamilyId);
        }

        log.debug("账户服务认证成功: userId={}, roles={}, familyId={}, userType={}, traceId={}",
                 userId, roles, tenant, userType, TraceIdUtil.getCurrent());
    }
}