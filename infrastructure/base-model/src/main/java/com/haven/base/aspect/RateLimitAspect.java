package com.haven.base.aspect;

import com.haven.base.annotation.RateLimit;
import com.haven.base.cache.CacheService;
import com.haven.base.common.exception.RateLimitException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;
import java.util.Optional;

/**
 * 限流切面实现
 * 基于CacheService实现分布式限流，支持全局、IP、用户三种限流类型
 *
 * @author HavenButler
 */
@Slf4j
@Aspect
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
@ConditionalOnClass({CacheService.class})
public class RateLimitAspect {

    private final CacheService cacheService;

    /**
     * 限流切面方法
     * 根据注解配置进行限流检查
     */
    @Around("@annotation(rateLimit)")
    public Object rateLimit(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String traceId = TraceIdUtil.getCurrentOrGenerate();

        try {
            // 生成限流键
            String rateLimitKey = generateRateLimitKey(rateLimit, point);

            // 检查限流
            checkRateLimit(rateLimitKey, rateLimit, traceId);

            // 执行原方法
            Object result = point.proceed();

            log.debug("限流检查通过，方法执行成功: key={}, traceId={}", rateLimitKey, traceId);
            return result;

        } catch (RateLimitException e) {
            log.warn("请求被限流拒绝: method={}, traceId={}, reason={}",
                    point.getSignature().getName(), traceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("限流切面执行异常: method={}, traceId={}, error={}",
                    point.getSignature().getName(), traceId, e.getMessage(), e);
            // 限流组件异常时，为了不影响业务，选择放行
            return point.proceed();
        }
    }

    /**
     * 生成限流键
     * 格式：rate_limit:{type}:{identifier}
     */
    private String generateRateLimitKey(RateLimit rateLimit, ProceedingJoinPoint point) {
        String prefix = "rate_limit:";
        String type = rateLimit.type().name().toLowerCase();
        String identifier;

        // 优先使用注解中指定的key
        if (rateLimit.key() != null && !rateLimit.key().isEmpty()) {
            return prefix + type + ":" + rateLimit.key();
        }

        switch (rateLimit.type()) {
            case GLOBAL:
                // 全局限流：使用方法签名
                identifier = point.getSignature().toShortString();
                break;

            case IP:
                // IP限流：获取客户端IP
                identifier = getClientIP();
                break;

            case USER:
                // 用户限流：从请求中提取用户ID
                identifier = getCurrentUserId();
                break;

            case IP_USER:
                // IP+用户组合限流：使用IP:用户ID组合
                String ip = getClientIP();
                String userId = getCurrentUserId();
                identifier = ip + ":" + userId;
                break;

            default:
                identifier = "default";
        }

        return prefix + type + ":" + identifier;
    }

    /**
     * 检查是否超过限流阈值
     */
    private void checkRateLimit(String key, RateLimit rateLimit, String traceId) {
        try {
            // 获取当前计数
            Optional<Long> currentCount = cacheService.get(key, Long.class);
            long count = currentCount.orElse(0L);

            if (count >= rateLimit.limit()) {
                // 超过限制，抛出限流异常
                String message = rateLimit.message().isEmpty() ?
                    String.format("请求过于频繁，请%d秒后重试", rateLimit.window()) :
                    rateLimit.message();

                throw new RateLimitException(ErrorCode.RATE_LIMIT_EXCEEDED, message);
            }

            // 增加计数
            long newCount = cacheService.increment(key, 1);

            // 第一次访问时设置过期时间
            if (newCount == 1) {
                cacheService.expire(key, Duration.ofSeconds(rateLimit.window()));
            }

            log.debug("限流计数更新: key={}, count={}/{}, window={}s, traceId={}",
                    key, newCount, rateLimit.limit(), rateLimit.window(), traceId);

        } catch (RateLimitException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流检查异常: key={}, traceId={}, error={}", key, traceId, e.getMessage(), e);
            // 缓存异常时放行，避免影响业务
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIP() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }

            // 处理多个IP的情况，取第一个
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }

            return ip != null ? ip : "unknown";

        } catch (Exception e) {
            log.warn("获取客户端IP失败: {}", e.getMessage());
            return "unknown";
        }
    }

    /**
     * 获取当前用户ID
     * 这里提供默认实现，实际项目中应该从认证信息中获取
     */
    private String getCurrentUserId() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();

            // 尝试从请求头获取用户ID
            String userId = request.getHeader("X-User-Id");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }

            // 尝试从请求参数获取
            userId = request.getParameter("userId");
            if (userId != null && !userId.isEmpty()) {
                return userId;
            }

            // TODO: 实际项目中应该从Spring Security上下文或JWT Token中获取用户ID
            // 这里返回IP作为fallback
            return getClientIP();

        } catch (Exception e) {
            log.warn("获取用户ID失败: {}", e.getMessage());
            return "anonymous";
        }
    }
}