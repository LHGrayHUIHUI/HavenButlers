package com.haven.common.web.filter;

import com.haven.base.common.constants.SystemConstants;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import com.haven.common.core.constants.CommonConstants;
import com.haven.common.security.JwtUtils;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 认证过滤器 - 基于base-model规范
 * 使用OncePerRequestFilter确保每个请求只执行一次
 * 统一ResponseWrapper格式返回认证错误
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model规范
 */
@Slf4j
@Component
public class AuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("#{'${base-model.trace.exclude-paths:/health,/actuator/**,/swagger-ui/**,/v3/api-docs/**}'.split(',')}")
    private List<String> whiteListPaths;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                   FilterChain filterChain) throws ServletException, IOException {

        String uri = request.getRequestURI();
        String traceId = TraceIdUtil.getCurrent();

        // 检查白名单 - 使用配置化路径
        if (isWhiteListed(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 获取令牌 - 支持多种Header
        String token = getTokenFromRequest(request);

        if (token == null || token.trim().isEmpty()) {
            log.warn("请求缺少令牌: uri={}, traceId={}", uri, traceId);
            writeErrorResponse(response, ErrorCode.UNAUTHORIZED, "未授权访问，请提供有效令牌");
            return;
        }

        // 验证令牌
        if (!jwtUtils.validateToken(token)) {
            log.warn("无效的令牌: uri={}, traceId={}", uri, traceId);
            writeErrorResponse(response, ErrorCode.TOKEN_INVALID, "JWT令牌无效或已过期");
            return;
        }

        // 提取用户信息并设置到请求上下文
        Claims claims = jwtUtils.getClaimsFromToken(token);
        if (claims != null) {
            String userId = claims.getSubject();
            String[] roles = jwtUtils.getRolesFromToken(token);
            String tenant = jwtUtils.getTenantFromToken(token);

            // 设置用户上下文
            request.setAttribute("userId", userId);
            request.setAttribute("roles", roles);
            request.setAttribute("tenant", tenant);
            request.setAttribute("token", token);

            // 设置到请求头供下游服务使用
            request.setAttribute(CommonConstants.Header.USER_ID, userId);
            request.setAttribute(CommonConstants.Header.FAMILY_ID, tenant);

            log.debug("认证成功: userId={}, roles={}, tenant={}, traceId={}",
                     userId, roles, tenant, traceId);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从请求中获取Token - 支持多种Header
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        // 1. 优先从X-Auth-Token获取
        String token = request.getHeader(CommonConstants.Header.TOKEN);
        if (token != null && !token.trim().isEmpty()) {
            return token.trim();
        }

        // 2. 从Authorization Header获取（Bearer格式）
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7).trim();
        }

        // 3. 从请求参数获取（仅用于测试环境）
        return request.getParameter("token");
    }

    /**
     * 检查是否在白名单中 - 支持Ant风格路径匹配
     */
    private boolean isWhiteListed(String uri) {
        if (whiteListPaths == null || whiteListPaths.isEmpty()) {
            return false;
        }

        return whiteListPaths.stream().anyMatch(pattern -> {
            // 简单的Ant风格匹配实现
            if (pattern.endsWith("/**")) {
                String prefix = pattern.substring(0, pattern.length() - 3);
                return uri.startsWith(prefix);
            } else if (pattern.endsWith("/*")) {
                String prefix = pattern.substring(0, pattern.length() - 2);
                return uri.startsWith(prefix) && !uri.substring(prefix.length()).contains("/");
            } else {
                return uri.equals(pattern) || uri.startsWith(pattern + "/");
            }
        });
    }

    /**
     * 写入统一格式的错误响应
     */
    private void writeErrorResponse(HttpServletResponse response, ErrorCode errorCode, String message) {
        try {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");

            ResponseWrapper<Void> errorResponse = ResponseWrapper.error(errorCode.getCode(), message);
            errorResponse.setTraceId(TraceIdUtil.getCurrent());

            String jsonResponse = JsonUtil.toJson(errorResponse);
            response.getWriter().write(jsonResponse);
            response.getWriter().flush();

        } catch (IOException e) {
            log.error("写入错误响应失败", e);
        }
    }
}