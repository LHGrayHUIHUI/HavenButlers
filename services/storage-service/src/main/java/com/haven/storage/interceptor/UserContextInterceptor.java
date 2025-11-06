package com.haven.storage.interceptor;


import com.haven.storage.security.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

/**
 * 用户上下文拦截器
 * <p>
 * 职责：
 * 1. 在请求开始时从请求头加载用户信息并设置到 ThreadLocal
 * 2. 在请求结束时清理 ThreadLocal，防止内存泄漏
 * 3. 记录请求的用户信息和执行时间
 *
 * @author HavenButler
 */
@Slf4j
@Component
public class UserContextInterceptor implements HandlerInterceptor {

    /**
     * 请求处理之前
     * 从请求头加载用户信息并设置到上下文
     */
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) {

        // 记录请求开始时间
        request.setAttribute("requestStartTime", System.currentTimeMillis());

        // 从请求头加载用户信息
        UserContext.loadFromRequest(request).ifPresentOrElse(
                userInfo -> {
                    // 设置用户上下文
                    UserContext.setCurrentUser(userInfo);
                    log.debug("用户上下文已设置: {} | URI: {}",
                            userInfo.toSummaryString(), request.getRequestURI());
                },
                () -> {
                    // 未找到用户信息（可能是公开接口）
                    log.debug("请求头中未找到用户信息 | URI: {}", request.getRequestURI());
                }
        );

        // 设置 TraceID（用于链路追踪）
        String traceId = request.getHeader(UserContext.TRACE_ID_HEADER);
        if (traceId != null) {
            UserContext.setCurrentTraceId(traceId);
        } else {
            // 如果请求头没有 TraceID，生成一个新的
            String generatedTraceId = generateTraceId();
            UserContext.setCurrentTraceId(generatedTraceId);
            response.setHeader(UserContext.TRACE_ID_HEADER, generatedTraceId);
        }

        return true;
    }

    /**
     * 请求处理之后，视图渲染之前
     */
    @Override
    public void postHandle(@NonNull HttpServletRequest request,
                           @NonNull HttpServletResponse response,
                           @NonNull Object handler,
                           ModelAndView modelAndView) {
        // 可以在这里添加一些后置处理逻辑
        // 例如：设置响应头、记录审计日志等

        // 将用户信息添加到响应头（可选）
        UserContext.getCurrentUserInfo().ifPresent(userInfo -> {
            response.setHeader("X-Current-User", userInfo.userId());
        });
    }

    /**
     * 请求完成之后
     * 清理 ThreadLocal，防止内存泄漏
     */
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        try {
            // 记录请求执行时间
            Long startTime = (Long) request.getAttribute("requestStartTime");
            if (startTime != null) {
                long duration = System.currentTimeMillis() - startTime;

                if (UserContext.isAuthenticated()) {
                    log.debug("请求完成: {} | 用户: {} | 耗时: {}ms | 状态: {}",
                            request.getRequestURI(),
                            UserContext.getUserSummary(),
                            duration,
                            response.getStatus());
                } else {
                    log.debug("请求完成: {} | 匿名访问 | 耗时: {}ms | 状态: {}",
                            request.getRequestURI(),
                            duration,
                            response.getStatus());
                }
            }

            // 检查是否使用了降级方案
            if (UserContext.isFallbackLoaded()) {
                log.warn("本次请求使用了降级方案加载用户上下文 | URI: {} | 请检查拦截器配置",
                        request.getRequestURI());
            }

            // 如果有异常，记录详细信息
            if (ex != null) {
                log.error("请求处理异常: {} | 用户: {} | 异常: {}",
                        request.getRequestURI(),
                        UserContext.getUserSummary(),
                        ex.getMessage(), ex);
            }

        } finally {
            // 清理 ThreadLocal（非常重要，防止内存泄漏）
            UserContext.clear();
            log.trace("用户上下文已清理");
        }
    }

    /**
     * 生成 TraceID
     * 格式：timestamp-randomNumber
     */
    private String generateTraceId() {
        return System.currentTimeMillis() + "-" +
                String.format("%06d", (int) (Math.random() * 1000000));
    }
}
