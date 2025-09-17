package com.haven.common.aspect;

import com.haven.base.annotation.RateLimit;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.response.ErrorCode;
import com.haven.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

/**
 * 限流切面
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "common.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitAspect {

    private final RedisUtils redisUtils;

    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = generateKey(point, rateLimit);

        try {
            // 原子递增
            Long count = redisUtils.increment(key);

            // 第一次访问，设置过期时间
            if (count == 1) {
                redisUtils.expire(key, rateLimit.window(), java.util.concurrent.TimeUnit.SECONDS);
            }

            // 检查是否超过限制
            if (count > rateLimit.limit()) {
                log.warn("限流触发: key={}, count={}, limit={}", key, count, rateLimit.limit());
                throw new BusinessException(ErrorCode.RATE_LIMIT_ERROR,
                        StringUtils.defaultIfBlank(rateLimit.message(), "请求过于频繁，请稍后再试"));
            }

            log.debug("限流检查通过: key={}, count={}, limit={}", key, count, rateLimit.limit());
            return point.proceed();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流处理异常: key={}", key, e);
            // 限流异常不影响业务执行
            return point.proceed();
        }
    }

    /**
     * 生成限流键
     */
    private String generateKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder(RATE_LIMIT_KEY_PREFIX);

        // 如果指定了key，使用指定的key
        if (StringUtils.isNotBlank(rateLimit.key())) {
            keyBuilder.append(rateLimit.key());
        } else {
            // 默认使用类名+方法名
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass().getSimpleName();
            String methodName = method.getName();
            keyBuilder.append(className).append(":").append(methodName);
        }

        // 如果需要按IP限流
        if (rateLimit.key().contains("${ip}")) {
            String ip = getClientIp();
            keyBuilder.append(":").append(ip);
        }

        // 如果需要按用户限流
        if (rateLimit.key().contains("${user}")) {
            String userId = getUserId();
            if (StringUtils.isNotBlank(userId)) {
                keyBuilder.append(":").append(userId);
            }
        }

        return keyBuilder.toString();
    }

    /**
     * 获取客户端IP
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String ip = request.getHeader("X-Forwarded-For");
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("X-Real-IP");
                }
                if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip;
            }
        } catch (Exception e) {
            log.error("获取客户端IP失败", e);
        }
        return "unknown";
    }

    /**
     * 获取用户ID
     */
    private String getUserId() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                // 从请求头获取用户ID
                String userId = request.getHeader("X-User-ID");
                if (StringUtils.isBlank(userId)) {
                    // 从session获取
                    Object userIdObj = request.getSession().getAttribute("userId");
                    if (userIdObj != null) {
                        userId = userIdObj.toString();
                    }
                }
                return userId;
            }
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
        }
        return null;
    }
}