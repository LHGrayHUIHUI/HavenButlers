package com.haven.base.security;

import com.haven.base.config.SecurityProperties;
import com.haven.base.utils.TraceIdUtil;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Map;

/**
 * JWT工具类（重构为使用SecurityProperties）
 * 提供JWT令牌的生成、验证、解析和刷新功能
 *
 * @author HavenButler
 * @version 1.0.0
 */
@Component
public class JwtUtils {

    private static final Logger log = LoggerFactory.getLogger(JwtUtils.class);

    private final SecurityProperties securityProperties;

    public JwtUtils(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(securityProperties.getJwt().getSecret().getBytes());
    }

    private long getJwtExpirationMs() {
        return securityProperties.getJwt().getExpiration().toMillis();
    }

    private long getJwtRefreshExpirationMs() {
        return securityProperties.getJwt().getRefreshExpiration().toMillis();
    }

    /**
     * 生成JWT令牌
     *
     * @param userId 用户ID
     * @param claims 额外的声明信息
     * @return JWT令牌字符串
     */
    public String generateToken(String userId, Map<String, Object> claims) {
        String traceId = TraceIdUtil.getCurrent();
        log.debug("生成JWT令牌: userId={}, traceId={}", userId, traceId);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + getJwtExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .setSubject(userId)
                .addClaims(claims)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey());

        // 添加签发者（如果启用验证）
        if (securityProperties.getJwt().isValidateIssuer()) {
            builder.setIssuer(securityProperties.getJwt().getIssuer());
        }

        // 添加受众（如果启用验证）
        if (securityProperties.getJwt().isValidateAudience()) {
            builder.setAudience(securityProperties.getJwt().getAudience());
        }

        return builder.compact();
    }

    /**
     * 生成JWT令牌（无额外声明）
     *
     * @param userId 用户ID
     * @return JWT令牌字符串
     */
    public String generateToken(String userId) {
        return generateToken(userId, null);
    }

    /**
     * 从令牌中获取用户ID
     *
     * @param token JWT令牌
     * @return 用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getSubject();
        } catch (Exception e) {
            log.error("从JWT令牌中获取用户ID失败: token={}, error={}", token, e.getMessage());
            return null;
        }
    }

    /**
     * 从令牌中获取Claims
     *
     * @param token JWT令牌
     * @return Claims对象
     */
    public Claims getClaimsFromToken(String token) {
        String traceId = TraceIdUtil.getCurrent();
        try {
            JwtParserBuilder parserBuilder = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey());

            // 配置签发者验证（如果启用）
            if (securityProperties.getJwt().isValidateIssuer()) {
                parserBuilder.requireIssuer(securityProperties.getJwt().getIssuer());
            }

            // 配置受众验证（如果启用）
            if (securityProperties.getJwt().isValidateAudience()) {
                parserBuilder.requireAudience(securityProperties.getJwt().getAudience());
            }

            return parserBuilder
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

        } catch (ExpiredJwtException e) {
            log.warn("JWT令牌已过期: token={}, traceId={}", token, traceId);
            throw new RuntimeException("令牌已过期", e);
        } catch (UnsupportedJwtException e) {
            log.error("不支持的JWT令牌: token={}, traceId={}", token, traceId);
            throw new RuntimeException("不支持的令牌", e);
        } catch (MalformedJwtException e) {
            log.error("JWT令牌格式错误: token={}, traceId={}", token, traceId);
            throw new RuntimeException("令牌格式错误", e);
        } catch (SecurityException e) {
            log.error("JWT令牌签名验证失败: token={}, traceId={}", token, traceId);
            throw new RuntimeException("令牌签名无效", e);
        } catch (Exception e) {
            log.error("解析JWT令牌失败: token={}, traceId={}, error={}", token, traceId, e.getMessage());
            throw new RuntimeException("令牌解析失败", e);
        }
    }

    /**
     * 验证JWT令牌是否有效
     *
     * @param token JWT令牌
     * @return 是否有效
     */
    public boolean validateToken(String token) {
        try {
            getClaimsFromToken(token);
            return true;
        } catch (Exception e) {
            log.debug("JWT令牌验证失败: token={}, error={}", token, e.getMessage());
            return false;
        }
    }

    /**
     * 检查JWT令牌是否过期
     *
     * @param token JWT令牌
     * @return 是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return true;
        }
    }

    /**
     * 刷新JWT令牌
     *
     * @param token 原JWT令牌
     * @return 新的JWT令牌
     */
    public String refreshToken(String token) {
        String traceId = TraceIdUtil.getCurrent();
        try {
            if (!securityProperties.getJwt().isEnableRefreshToken()) {
                throw new RuntimeException("刷新令牌功能已禁用");
            }

            final Claims claims = getClaimsFromToken(token);
            String userId = claims.getSubject();

            // 检查是否在刷新窗口期内
            if (!isInRefreshWindow(token)) {
                log.warn("JWT令牌超出刷新窗口期: token={}, traceId={}", token, traceId);
                throw new RuntimeException("令牌超出刷新窗口期");
            }

            // 移除过期时间等时效性信息，保留用户信息
            claims.remove("exp");
            claims.remove("iat");

            return generateToken(userId, claims);
        } catch (Exception e) {
            log.error("刷新JWT令牌失败: token={}, traceId={}, error={}", token, traceId, e.getMessage());
            throw new RuntimeException("令牌刷新失败", e);
        }
    }

    /**
     * 从令牌中获取指定声明
     *
     * @param token JWT令牌
     * @param claimName 声明名称
     * @param clazz 声明值类型
     * @param <T> 泛型类型
     * @return 声明值
     */
    public <T> T getClaimFromToken(String token, String claimName, Class<T> clazz) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.get(claimName, clazz);
        } catch (Exception e) {
            log.error("从JWT令牌中获取声明失败: token={}, claim={}, error={}", token, claimName, e.getMessage());
            return null;
        }
    }

    /**
     * 获取令牌剩余有效时间（毫秒）
     *
     * @param token JWT令牌
     * @return 剩余时间（毫秒），已过期返回0
     */
    public long getTokenRemainingTime(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            long remaining = expiration.getTime() - System.currentTimeMillis();
            return Math.max(0, remaining);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 生成刷新令牌
     *
     * @param userId 用户ID
     * @return 刷新令牌
     */
    public String generateRefreshToken(String userId) {
        String traceId = TraceIdUtil.getCurrent();
        log.debug("生成JWT刷新令牌: userId={}, traceId={}", userId, traceId);

        if (!securityProperties.getJwt().isEnableRefreshToken()) {
            throw new RuntimeException("刷新令牌功能已禁用");
        }

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + getJwtRefreshExpirationMs());

        JwtBuilder builder = Jwts.builder()
                .setSubject(userId)
                .claim("type", "refresh")
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey());

        // 添加签发者（如果启用验证）
        if (securityProperties.getJwt().isValidateIssuer()) {
            builder.setIssuer(securityProperties.getJwt().getIssuer());
        }

        return builder.compact();
    }

    /**
     * 验证是否为刷新令牌
     *
     * @param token JWT令牌
     * @return 是否为刷新令牌
     */
    public boolean isRefreshToken(String token) {
        try {
            String type = getClaimFromToken(token, "type", String.class);
            return "refresh".equals(type);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取令牌签发时间
     *
     * @param token JWT令牌
     * @return 签发时间
     */
    public Date getIssuedAtDate(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            return claims.getIssuedAt();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 检查令牌是否在刷新窗口期内
     *
     * @param token JWT令牌
     * @return 是否在刷新窗口期内
     */
    public boolean isInRefreshWindow(String token) {
        try {
            Claims claims = getClaimsFromToken(token);
            Date expiration = claims.getExpiration();
            long refreshWindowMs = securityProperties.getJwt().getRefreshWindow().toMillis();
            long remainingTime = expiration.getTime() - System.currentTimeMillis();
            return remainingTime > 0 && remainingTime <= refreshWindowMs;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取令牌类型
     *
     * @param token JWT令牌
     * @return 令牌类型（access、refresh等）
     */
    public String getTokenType(String token) {
        try {
            return getClaimFromToken(token, "type", String.class);
        } catch (Exception e) {
            return "access"; // 默认为访问令牌
        }
    }

    /**
     * 验证令牌是否为访问令牌
     *
     * @param token JWT令牌
     * @return 是否为访问令牌
     */
    public boolean isAccessToken(String token) {
        return !isRefreshToken(token);
    }

    /**
     * 获取完整的令牌（包含前缀）
     *
     * @param token JWT令牌
     * @return 带前缀的令牌
     */
    public String getTokenWithPrefix(String token) {
        return securityProperties.getJwt().getTokenPrefix() + token;
    }

    /**
     * 从带前缀的令牌中提取JWT令牌
     *
     * @param tokenWithPrefix 带前缀的令牌
     * @return JWT令牌
     */
    public String extractToken(String tokenWithPrefix) {
        if (tokenWithPrefix != null && tokenWithPrefix.startsWith(securityProperties.getJwt().getTokenPrefix())) {
            return tokenWithPrefix.substring(securityProperties.getJwt().getTokenPrefix().length());
        }
        return tokenWithPrefix;
    }

    /**
     * 验证令牌格式
     *
     * @param token JWT令牌
     * @return 格式是否正确
     */
    public boolean isValidTokenFormat(String token) {
        try {
            // 基本格式检查：JWT由三部分组成，用点分隔
            String[] parts = token.split("\\.");
            return parts.length == 3;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取JWT配置信息
     *
     * @return JWT配置信息
     */
    public SecurityProperties.Jwt getJwtConfig() {
        return securityProperties.getJwt();
    }

    /**
     * 检查安全功能是否启用
     *
     * @return 安全功能是否启用
     */
    public boolean isSecurityEnabled() {
        return securityProperties.isEnabled();
    }
}