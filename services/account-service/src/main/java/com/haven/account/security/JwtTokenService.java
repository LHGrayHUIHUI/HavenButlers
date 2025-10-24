package com.haven.account.security;

import com.haven.account.entity.User;
import com.haven.common.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JWT Token 服务类
 *
 * @author HavenButler
 * @since 2025-01-16
 */
@Slf4j
@Service
public class JwtTokenService {

    @Autowired
    private JwtUtils jwtUtils;

    @Value("${jwt.access-token-validity:7200000}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:604800000}")
    private long refreshTokenValidity;

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user) {
        Map<String, Object> claims = buildUserClaims(user, "access");
        return jwtUtils.generateToken(user.getId().toString(), claims);
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(User user) {
        Map<String, Object> claims = buildUserClaims(user, "refresh");
        return jwtUtils.generateToken(user.getId().toString(), claims);
    }

    /**
     * 生成Token对
     */
    public TokenPair generateTokenPair(User user) {
        String accessToken = generateAccessToken(user);
        String refreshToken = generateRefreshToken(user);
        return new TokenPair(accessToken, refreshToken);
    }

    /**
     * 构建用户Claims
     */
    private Map<String, Object> buildUserClaims(User user, String tokenType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("uuid", user.getUuid().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("status", user.getStatus());
        claims.put("roles", user.getRoles());
        claims.put("currentFamilyId", user.getCurrentFamilyId());
        claims.put("tokenType", tokenType);
        claims.put("tenant", "family-" + user.getCurrentFamilyId()); // 租户标识

        return claims;
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        return jwtUtils.validateToken(token);
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        return jwtUtils.getClaimsFromToken(token);
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        String userId = jwtUtils.getUserIdFromToken(token);
        return userId != null ? Long.parseLong(userId) : null;
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        return jwtUtils.isTokenExpired(token);
    }

    /**
     * 从Token中获取Token类型
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims != null ? (String) claims.get("tokenType") : null;
    }

    /**
     * 刷新访问令牌
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);

            if (claims == null) {
                throw new IllegalArgumentException("无效的刷新令牌");
            }

            // 验证这是刷新令牌
            if (!"refresh".equals(claims.get("tokenType"))) {
                throw new IllegalArgumentException("不是有效的刷新令牌");
            }

            // 检查令牌是否过期
            if (jwtUtils.isTokenExpired(refreshToken)) {
                throw new IllegalArgumentException("刷新令牌已过期");
            }

            // 构建新的访问令牌claims
            Map<String, Object> accessClaims = new HashMap<>();
            accessClaims.put("uuid", claims.get("uuid"));
            accessClaims.put("username", claims.get("username"));
            accessClaims.put("email", claims.get("email"));
            accessClaims.put("status", claims.get("status"));
            accessClaims.put("roles", claims.get("roles"));
            accessClaims.put("currentFamilyId", claims.get("currentFamilyId"));
            accessClaims.put("tokenType", "access");
            accessClaims.put("tenant", claims.get("tenant"));

            // 重新生成访问令牌
            return jwtUtils.generateToken(claims.getSubject(), accessClaims);

        } catch (IllegalArgumentException e) {
            log.error("刷新访问令牌失败: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("刷新访问令牌失败: {}", e.getMessage());
            throw new IllegalArgumentException("无效的刷新令牌", e);
        }
    }

    /**
     * Token对
     */
    public static class TokenPair {
        private final String accessToken;
        private final String refreshToken;

        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}