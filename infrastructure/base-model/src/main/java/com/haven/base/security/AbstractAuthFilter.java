package com.haven.base.security;

import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 抽象认证过滤器
 * 提供基础认证逻辑，允许子类自定义关键行为
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public abstract class AbstractAuthFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;

    @Value("${base-model.security.auth-exclude-paths:/health,/actuator/**}")
    protected String[] excludePaths;

    @Value("${base-model.security.token-header:X-Auth-Token}")
    protected String tokenHeader;

    @Value("${base-model.security.auth-enabled:true}")
    protected boolean authEnabled;

    // ==================== 认证配置函数 ====================

    /**
     * 判断当前微服务是否需要认证拦截
     * 默认返回true（需要认证），子类可重写
     *
     * @return true表示需要认证，false表示不需要认证
     */
    protected boolean isAuthenticationRequired() {
        return authEnabled;
    }

    /**
     * 获取需要排除认证的路径列表
     * 子类可以重写此方法来自定义排除路径
     */
    protected List<String> getExcludePaths() {
        List<String> paths = new ArrayList<>();
        if (excludePaths != null) {
            for (String path : excludePaths) {
                paths.add(path.trim());
            }
        }
        return paths;
    }

    /**
     * 子类可以添加自定义的排除路径
     *
     * @return 子类自定义的排除路径列表
     */
    protected List<String> getCustomExcludePaths() {
        return new ArrayList<>();
    }

    /**
     * 检查请求路径是否需要排除认证
     */
    protected boolean isExcludePath(String requestURI) {
        // 合并配置的排除路径和子类自定义的排除路径
        List<String> allExcludePaths = new ArrayList<>();
        allExcludePaths.addAll(getExcludePaths());
        allExcludePaths.addAll(getCustomExcludePaths());

        // 使用Spring的AntPathMatcher进行路径匹配
        AntPathMatcher matcher = new AntPathMatcher();
        for (String excludePath : allExcludePaths) {
            if (matcher.match(excludePath, requestURI)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 路径匹配（支持通配符）- 保留原有实现
     */
    protected boolean matchPath(String pattern, String path) {
        if (pattern.equals(path)) {
            return true;
        }
        if (pattern.contains("*")) {
            String regex = pattern.replace("*", ".*");
            return path.matches(regex);
        }
        return false;
    }


    /**
     * 执行认证流程
     * 封装完整的认证步骤：Token提取 -> 认证方式 -> 权限验证
     *
     * @param request    HTTP请求
     * @param response   HTTP响应
     * @param requestURI 请求URI
     * @param traceId    链路追踪ID
     * @return 认证成功返回Claims，失败返回null
     */
    protected Claims performAuthentication(HttpServletRequest request, HttpServletResponse response, String requestURI, String traceId) throws IOException {
        // 1. 从请求中提取Token
        String token = extractTokenFromRequest(request);
        if (token == null || token.trim().isEmpty()) {
            log.warn("请求缺少认证Token: uri={}, traceId={}", requestURI, traceId);
            handleAuthFailure(response, "缺少认证Token", HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        // 2. 执行认证（子类可重写认证方式）
        Claims claims = authenticateRequest(request, response, token);
        if (claims == null) {
            log.warn("认证失败: uri={}, token={}, traceId={}", requestURI, token, traceId);
            handleAuthFailure(response, "认证失败", HttpServletResponse.SC_UNAUTHORIZED);
            return null;
        }

        // 3. 验证权限
        if (!validatePermissions(claims, request)) {
            log.warn("用户权限不足: uri={}, userId={}, traceId={}", requestURI, claims.getSubject(), traceId);
            handleAuthFailure(response, "权限不足", HttpServletResponse.SC_FORBIDDEN);
            return null;
        }

        return claims;
    }

    /**
     * 认证方式
     * 子类可以重写此方法来实现自定义认证逻辑
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param token    从请求中提取的Token
     * @return 认证成功返回用户信息Claims，失败返回null
     */
    protected Claims authenticateRequest(HttpServletRequest request, HttpServletResponse response, String token) {
        // 默认实现：基于JWT Token的认证方式
        return validateToken(token);
    }

    /**
     * 从请求中提取Token
     * 子类可以重写此方法来实现自定义Token提取逻辑
     */
    protected String extractTokenFromRequest(HttpServletRequest request) {
        String token = request.getHeader(tokenHeader);
        if (token == null || token.trim().isEmpty()) {
            // 尝试从Authorization header获取Bearer token
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                token = authHeader.substring(7);
            }
        }
        return token;
    }

    /**
     * 验证Token（保留原有实现作为默认认证方式）
     */
    protected Claims validateToken(String token) {
        try {
            if (jwtUtils.validateToken(token)) {
                return jwtUtils.getClaimsFromToken(token);
            }
        } catch (Exception e) {
            log.debug("Token验证失败: token={}, error={}", token, e.getMessage());
        }
        return null;
    }

    /**
     * 验证用户权限
     * 子类可以重写此方法来实现自定义权限验证
     */
    protected boolean validatePermissions(Claims claims, HttpServletRequest request) {
        // 默认实现：只验证token有效性，不做额外权限检查
        return true;
    }

    /**
     * 认证失败处理
     */
    protected void handleAuthFailure(HttpServletResponse response, String message, int statusCode) throws IOException {
        ResponseWrapper<?> errorResponse = ResponseWrapper.error(statusCode, message);

        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(JsonUtil.toJson(errorResponse));
    }

    /**
     * 认证成功处理
     * 子类可以重写此方法来自定义认证成功后的处理逻辑
     * 默认实现为空，子类需要根据业务需求实现具体的用户信息存储逻辑
     *
     * @param request HTTP请求
     * @param claims  用户认证信息
     */
    protected void handleAuthSuccess(HttpServletRequest request, Claims claims) {
        // 默认实现为空，子类可重写实现：
        // - 存储用户信息到request属性
        // - 记录登录日志
        // - 设置用户上下文等
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String traceId = TraceIdUtil.getCurrent();

        log.debug("认证过滤器处理请求: uri={}, traceId={}", requestURI, traceId);

        // 检查当前微服务是否需要认证拦截
        if (!isAuthenticationRequired()) {
            log.debug("当前服务配置为不需要认证: uri={}, traceId={}", requestURI, traceId);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查是否需要排除认证
        if (isExcludePath(requestURI)) {
            log.debug("请求路径无需认证: uri={}, traceId={}", requestURI, traceId);
            filterChain.doFilter(request, response);
            return;
        }

        // 执行认证流程
        Claims claims;
        try {
            claims = performAuthentication(request, response, requestURI, traceId);
        } catch (IOException e) {
            log.error("认证过程中发生IO异常: uri={}, traceId={}, error={}", requestURI, traceId, e.getMessage());
            handleAuthFailure(response, "认证过程异常", HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            return;
        }

        if (claims == null) {
            return; // 认证失败已在performAuthentication中处理
        }

        // 认证成功
        handleAuthSuccess(request, claims);
        log.debug("用户认证成功: uri={}, userId={}, traceId={}", requestURI, claims.getSubject(), traceId);

        filterChain.doFilter(request, response);
    }

}