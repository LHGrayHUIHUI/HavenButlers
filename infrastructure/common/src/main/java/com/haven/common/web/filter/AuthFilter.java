package com.haven.common.web.filter;

import com.haven.base.common.constants.SystemConstants;
import com.haven.common.security.JwtUtils;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 认证过滤器
 * 验证JWT令牌
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class AuthFilter implements Filter {

    @Autowired
    private JwtUtils jwtUtils;

    // 白名单路径
    private static final List<String> WHITE_LIST = Arrays.asList(
        "/api/v1/account/login",
        "/api/v1/account/register",
        "/health",
        "/actuator",
        "/swagger-ui",
        "/v3/api-docs",
        "/api/v1/storage/health",
        "/api/storage/health",
        "/test"
    );

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String uri = httpRequest.getRequestURI();

        // 检查白名单
        if (isWhiteListed(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 获取令牌
        String token = httpRequest.getHeader(SystemConstants.Header.TOKEN);

        if (token == null || token.isEmpty()) {
            log.warn("请求缺少令牌: {}", uri);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\":401,\"message\":\"未授权\"}");
            return;
        }

        // 验证令牌
        if (!jwtUtils.validateToken(token)) {
            log.warn("无效的令牌: {}", uri);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\":401,\"message\":\"令牌无效\"}");
            return;
        }

        // 检查令牌是否过期
        if (jwtUtils.isTokenExpired(token)) {
            log.warn("令牌已过期: {}", uri);
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.getWriter().write("{\"code\":401,\"message\":\"令牌已过期\"}");
            return;
        }

        // 提取用户ID并设置到请求头
        String userId = jwtUtils.getUserIdFromToken(token);
        if (userId != null) {
            httpRequest.setAttribute("userId", userId);
        }

        chain.doFilter(request, response);
    }

    /**
     * 检查是否在白名单中
     */
    private boolean isWhiteListed(String uri) {
        return WHITE_LIST.stream().anyMatch(uri::startsWith);
    }
}