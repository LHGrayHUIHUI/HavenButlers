package com.haven.common.security;

import com.haven.base.common.exception.AuthException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT工具类 - 基于base-model安全规范
 * 用于生成和验证JWT令牌，支持密钥轮换和标准Claims
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model安全规范
 */
@Slf4j
@Component
public class JwtUtils {

    @Value("${base-model.security.jwt-secret:${JWT_SECRET:}}")
    private String jwtSecret;

    @Value("${base-model.security.jwt-expiration:86400000}")
    private long jwtExpiration;

    @Value("${base-model.security.jwt-clock-skew:30}")
    private int clockSkewSeconds;

    @Value("${base-model.security.jwt-issuer:HavenButler}")
    private String issuer;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        if (jwtSecret == null || jwtSecret.trim().isEmpty()) {
            throw new IllegalArgumentException(
                "JWT密钥未配置！请设置环境变量JWT_SECRET或配置base-model.security.jwt-secret");
        }

        if (jwtSecret.length() < 32) {
            log.warn("⚠️ JWT密钥长度不足32位，生产环境存在安全风险！当前长度: {}", jwtSecret.length());
        }

        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes());
        log.info("JWT工具类初始化完成 - 过期时间: {}ms, 时钟偏差: {}s", jwtExpiration, clockSkewSeconds);
    }

    /**
     * 生成JWT令牌 - 支持标准Claims
     *
     * @param userId 用户ID
     * @param claims 自定义Claims
     * @return JWT令牌
     */
    public String generateToken(String userId, Map<String, Object> claims) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        // 构建标准Claims
        Map<String, Object> standardClaims = new HashMap<>(claims);
        standardClaims.put("roles", claims.getOrDefault("roles", new String[]{}));
        standardClaims.put("tenant", claims.getOrDefault("tenant", "default"));
        standardClaims.put("traceId", TraceIdUtil.getCurrent());

        try {
            return Jwts.builder()
                .setSubject(userId)
                .setIssuer(issuer)
                .addClaims(standardClaims)
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
        } catch (Exception e) {
            log.error("生成JWT令牌失败: userId={}, error={}", userId, e.getMessage());
            throw new AuthException(ErrorCode.TOKEN_INVALID, "JWT令牌生成失败");
        }
    }

    /**
     * 生成JWT令牌（简化版）
     */
    public String generateToken(String userId) {
        return generateToken(userId, new HashMap<>());
    }

    /**
     * 验证JWT令牌 - 支持时钟偏差
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .requireIssuer(issuer)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.debug("JWT令牌已过期: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.UnsupportedJwtException e) {
            log.warn("不支持的JWT令牌: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.warn("格式错误的JWT令牌: {}", e.getMessage());
            return false;
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.warn("JWT签名验证失败: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("JWT令牌为空或无效: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("JWT令牌验证异常: {}", e.getMessage());
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
     * 从令牌中获取Claims - 统一异常处理
     */
    public Claims getClaimsFromToken(String token) {
        try {
            return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .setAllowedClockSkewSeconds(clockSkewSeconds)
                .build()
                .parseClaimsJws(token)
                .getBody();
        } catch (Exception e) {
            log.debug("解析JWT令牌失败: {}", e.getMessage());
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
     * 判断令牌是否过期 - 考虑时钟偏差
     */
    public boolean isTokenExpired(String token) {
        Date expiration = getExpirationDateFromToken(token);
        if (expiration == null) {
            return true;
        }

        // 考虑时钟偏差
        long clockSkewMs = clockSkewSeconds * 1000L;
        return expiration.getTime() < (System.currentTimeMillis() - clockSkewMs);
    }

    /**
     * 刷新令牌 - 保持标准Claims结构
     */
    public String refreshToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            throw new AuthException(ErrorCode.TOKEN_INVALID, "无效的JWT令牌，无法刷新");
        }

        String userId = claims.getSubject();
        Map<String, Object> newClaims = new HashMap<>(claims);

        // 移除系统Claims，保留业务Claims
        newClaims.remove("iat");
        newClaims.remove("exp");
        newClaims.remove("iss");
        newClaims.remove("sub");

        // 更新traceId
        newClaims.put("traceId", TraceIdUtil.getCurrent());

        return generateToken(userId, newClaims);
    }

    /**
     * 获取令牌中的角色信息
     */
    public String[] getRolesFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        if (claims == null) {
            return new String[]{};
        }

        Object roles = claims.get("roles");
        if (roles instanceof String[]) {
            return (String[]) roles;
        } else if (roles instanceof String) {
            return new String[]{(String) roles};
        }

        return new String[]{};
    }

    /**
     * 获取令牌中的租户信息
     */
    public String getTenantFromToken(String token) {
        Claims claims = getClaimsFromToken(token);
        return claims != null ? (String) claims.get("tenant") : "default";
    }
}