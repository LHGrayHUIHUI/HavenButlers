package com.haven.account.security;

import com.haven.account.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
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

    @Value("${base-model.security.jwt-secret:haven-account-service-jwt-secret-key-2025}")
    private String jwtSecret;

    @Value("${base-model.security.jwt-expiration:7200000}")
    private long jwtExpiration;

    @Value("${base-model.security.jwt-clock-skew:30}")
    private int jwtClockSkew;

    @Value("${jwt.access-token-validity:7200000}")
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:604800000}")
    private long refreshTokenValidity;

    private SecretKey getSigningKey() {
        // TODO: 使用更安全的密钥管理方式
        byte[] keyBytes = jwtSecret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenValidity, "access");
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenValidity, "refresh");
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
     * 生成Token
     */
    private String generateToken(User user, long expiration, String tokenType) {
        Instant now = Instant.now();
        Instant expiryDate = now.plusSeconds(expiration / 1000);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId().toString());
        claims.put("uuid", user.getUuid().toString());
        claims.put("username", user.getUsername());
        claims.put("email", user.getEmail());
        claims.put("status", user.getStatus());
        claims.put("roles", user.getRoles());
        claims.put("currentFamilyId", user.getCurrentFamilyId());
        claims.put("tokenType", tokenType);
        claims.put("iat", Date.from(now));
        claims.put("exp", Date.from(expiryDate));
        claims.put("jti", UUID.randomUUID().toString());

        return Jwts.builder()
                .setClaims(claims)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * 验证Token
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("Token已过期: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Token类型不支持: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token格式错误: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.warn("Token签名无效: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token参数错误: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 解析Token获取Claims
     */
    public Claims parseToken(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Token解析失败: {}", e.getMessage());
            throw new IllegalArgumentException("无效的Token", e);
        }
    }

    /**
     * 从Token中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 检查Token是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true; // 解析失败视为已过期
        }
    }

    /**
     * 从Token中获取Token类型
     */
    public String getTokenType(String token) {
        Claims claims = parseToken(token);
        return claims.get("tokenType", String.class);
    }

    /**
     * 刷新访问令牌
     */
    public String refreshAccessToken(String refreshToken) {
        try {
            Claims claims = parseToken(refreshToken);

            // 验证这是刷新令牌
            if (!"refresh".equals(claims.get("tokenType"))) {
                throw new IllegalArgumentException("不是有效的刷新令牌");
            }

            // 检查令牌是否过期
            if (claims.getExpiration().before(new Date())) {
                throw new IllegalArgumentException("刷新令牌已过期");
            }

            // 从刷新令牌中创建新的访问令牌
            Map<String, Object> accessClaims = new HashMap<>(claims);
            accessClaims.remove("exp"); // 移除过期时间
            accessClaims.remove("iat");
            accessClaims.put("exp", new Date(System.currentTimeMillis() + accessTokenValidity));
            accessClaims.put("tokenType", "access");
            accessClaims.put("jti", UUID.randomUUID().toString());

            return Jwts.builder()
                    .setClaims(accessClaims)
                    .signWith(getSigningKey())
                    .compact();

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