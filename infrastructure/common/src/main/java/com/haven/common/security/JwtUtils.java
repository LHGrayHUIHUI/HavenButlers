package com.haven.common.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类
 * 用于生成和验证JWT令牌
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class JwtUtils {

    private static final String SECRET = "HavenButlerSecretKeyForJWTGenerationAndValidation2025";
    private static final long EXPIRATION = 86400000; // 24小时
    private static final SecretKey KEY = Keys.hmacShaKeyFor(SECRET.getBytes());

    /**
     * 生成JWT令牌
     */
    public String generateToken(String userId, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION);

        return Jwts.builder()
            .setSubject(userId)
            .addClaims(claims)
            .setIssuedAt(now)
            .setExpiration(expiry)
            .signWith(KEY, SignatureAlgorithm.HS256)
            .compact();
    }

    /**
     * 生成JWT令牌（简化版）
     */
    public String generateToken(String userId) {
        return generateToken(userId, Map.of());
    }

    /**
     * 验证JWT令牌
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            log.error("JWT令牌验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * 从令牌中获取Claims
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(KEY)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.error("解析JWT令牌失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取令牌过期时间
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? claims.getExpiration() : null;
    }

    /**
     * 判断令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        return expiration != null && expiration.before(new Date());
    }

    /**
     * 刷新令牌
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims != null) {
            String userId = claims.getSubject();
            claims.remove("iat");
            claims.remove("exp");
            return generateToken(userId, claims);
        }
        return null;
    }
}