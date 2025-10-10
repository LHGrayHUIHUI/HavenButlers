package com.haven.common.aspect;

import com.haven.base.annotation.RateLimit;
import com.haven.base.common.exception.BusinessException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.utils.TraceIdUtil;
import com.haven.common.redis.RedisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 限流切面 - 基于base-model规范
 * 支持SpEL表达式解析和指标收集
 *
 * @author HavenButler
 * @version 2.0.0 - 对齐base-model限流规范
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "base-model.cache", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RateLimitCommonAspect {

    private final RedisUtils redisUtils;

    @Value("${base-model.rate-limit.key-prefix:haven:rate_limit:}")
    private String rateLimitKeyPrefix;

    @Value("${base-model.rate-limit.enable-metrics:true}")
    private boolean enableMetrics;

    // SpEL表达式解析器
    private final ExpressionParser expressionParser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();

    // 表达式缓存
    private final ConcurrentHashMap<String, Expression> expressionCache = new ConcurrentHashMap<>();

    // 限流指标
    private final AtomicLong rateLimitHits = new AtomicLong(0);
    private final AtomicLong rateLimitPassed = new AtomicLong(0);
    private final AtomicLong rateLimitBlocked = new AtomicLong(0);

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String traceId = TraceIdUtil.getCurrent();
        String key = generateKeyWithSpEL(point, rateLimit);

        long startTime = System.currentTimeMillis();

        // 更新指标
        if (enableMetrics) {
            rateLimitHits.incrementAndGet();
        }

        try {
            // 原子递增
            Long count = redisUtils.increment(key);

            // 第一次访问，设置过期时间
            if (count == 1) {
                redisUtils.expire(key, rateLimit.window(), java.util.concurrent.TimeUnit.SECONDS);
            }

            // 检查是否超过限制
            if (count > rateLimit.limit()) {
                if (enableMetrics) {
                    rateLimitBlocked.incrementAndGet();
                }

                log.warn("限流触发: key={}, count={}, limit={}, window={}s, traceId={}",
                        key, count, rateLimit.limit(), rateLimit.window(), traceId);

                throw new BusinessException(ErrorCode.RATE_LIMIT_ERROR,
                        StringUtils.defaultIfBlank(rateLimit.message(), "请求过于频繁，请稍后再试"));
            }

            // 限流检查通过
            if (enableMetrics) {
                rateLimitPassed.incrementAndGet();
            }

            long duration = System.currentTimeMillis() - startTime;
            log.debug("限流检查通过: key={}, count={}, limit={}, duration={}ms, traceId={}",
                     key, count, rateLimit.limit(), duration, traceId);

            return point.proceed();

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("限流处理异常: key={}, traceId={}", key, traceId, e);
            // 限流异常不影响业务执行
            return point.proceed();
        }
    }

    /**
     * 生成限流键 - 支持SpEL表达式
     */
    private String generateKeyWithSpEL(ProceedingJoinPoint point, RateLimit rateLimit) {
        String keyExpression = rateLimit.key();

        // 如果没有指定key，使用默认格式
        if (StringUtils.isBlank(keyExpression)) {
            MethodSignature signature = (MethodSignature) point.getSignature();
            Method method = signature.getMethod();
            String className = method.getDeclaringClass().getSimpleName();
            String methodName = method.getName();
            return rateLimitKeyPrefix + className + ":" + methodName;
        }

        // 解析SpEL表达式
        try {
            String evaluatedKey = evaluateSpELExpression(keyExpression, point);
            return rateLimitKeyPrefix + evaluatedKey;
        } catch (Exception e) {
            log.warn("SpEL表达式解析失败，使用原始key: expression={}, error={}", keyExpression, e.getMessage());
            return rateLimitKeyPrefix + keyExpression;
        }
    }

    /**
     * 解析SpEL表达式
     */
    private String evaluateSpELExpression(String expressionString, ProceedingJoinPoint point) {
        // 从缓存获取或创建表达式
        Expression expression = expressionCache.computeIfAbsent(expressionString,
            key -> expressionParser.parseExpression(key));

        // 创建评估上下文
        EvaluationContext context = createEvaluationContext(point);

        // 评估表达式
        Object result = expression.getValue(context);
        return result != null ? result.toString() : "null";
    }

    /**
     * 创建SpEL评估上下文
     */
    private EvaluationContext createEvaluationContext(ProceedingJoinPoint point) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Object[] args = point.getArgs();

        // 基于方法的评估上下文
        EvaluationContext context = new MethodBasedEvaluationContext(
            point.getTarget(), method, args, parameterNameDiscoverer);

        // 添加常用变量
        context.setVariable("ip", getClientIp());
        context.setVariable("user", getUserId());
        context.setVariable("traceId", TraceIdUtil.getCurrent());
        context.setVariable("className", method.getDeclaringClass().getSimpleName());
        context.setVariable("methodName", method.getName());

        // 添加请求相关变量
        HttpServletRequest request = getCurrentRequest();
        if (request != null) {
            context.setVariable("uri", request.getRequestURI());
            context.setVariable("method", request.getMethod());
            context.setVariable("userAgent", request.getHeader("User-Agent"));
        }

        return context;
    }

    /**
     * 获取当前HTTP请求
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            log.debug("获取当前请求失败", e);
            return null;
        }
    }

    /**
     * 获取客户端IP - 支持多种代理头
     */
    private String getClientIp() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return "unknown";
        }

        try {
            String[] ipHeaders = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP",
                "HTTP_X_FORWARDED_FOR"
            };

            for (String header : ipHeaders) {
                String ip = request.getHeader(header);
                if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
                    // X-Forwarded-For可能包含多个IP，取第一个
                    return ip.split(",")[0].trim();
                }
            }

            return request.getRemoteAddr();
        } catch (Exception e) {
            log.error("获取客户端IP失败", e);
            return "unknown";
        }
    }

    /**
     * 获取用户ID - 支持多种获取方式
     */
    private String getUserId() {
        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return null;
        }

        try {
            // 1. 从请求头获取
            String userId = request.getHeader("X-User-ID");
            if (StringUtils.isNotBlank(userId)) {
                return userId;
            }

            // 2. 从请求属性获取（AuthFilter设置）
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr != null) {
                return userIdAttr.toString();
            }

            // 3. 从JWT Token解析（如果存在）
            String token = request.getHeader("Authorization");
            if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
                // 这里可以调用JwtUtils解析用户ID，但为避免循环依赖暂时省略
            }

            return null;
        } catch (Exception e) {
            log.error("获取用户ID失败", e);
            return null;
        }
    }

    /**
     * 获取限流指标
     */
    public java.util.Map<String, Long> getRateLimitMetrics() {
        java.util.Map<String, Long> metrics = new java.util.HashMap<>();
        metrics.put("rateLimitHits", rateLimitHits.get());
        metrics.put("rateLimitPassed", rateLimitPassed.get());
        metrics.put("rateLimitBlocked", rateLimitBlocked.get());

        // 计算通过率和阻塞率
        long total = rateLimitHits.get();
        if (total > 0) {
            metrics.put("passRate", (rateLimitPassed.get() * 100) / total);
            metrics.put("blockRate", (rateLimitBlocked.get() * 100) / total);
        } else {
            metrics.put("passRate", 0L);
            metrics.put("blockRate", 0L);
        }

        return metrics;
    }

    /**
     * 重置限流指标
     */
    public void resetMetrics() {
        rateLimitHits.set(0);
        rateLimitPassed.set(0);
        rateLimitBlocked.set(0);
        log.info("限流指标已重置");
    }
}