package com.haven.base.interceptor;

import com.haven.base.common.constants.SystemConstants;
import com.haven.base.utils.TraceIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * TraceID拦截器
 * 为每个请求生成或传递TraceID，用于链路追踪
 *
 * @author HavenButler
 */
@Slf4j
public class TraceIdInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 获取请求头中的TraceID
        String traceId = request.getHeader(SystemConstants.Header.TRACE_ID);

        // 如果没有则生成新的TraceID
        if (StringUtils.isBlank(traceId)) {
            traceId = TraceIdUtil.generate();
            log.debug("生成新的TraceID: {}", traceId);
        } else {
            log.debug("使用请求头中的TraceID: {}", traceId);
        }

        // 设置到MDC
        TraceIdUtil.setTraceId(traceId);

        // 设置到响应头
        response.setHeader(SystemConstants.Header.TRACE_ID, traceId);

        // 记录请求信息
        log.info("请求开始 - Method: {}, URI: {}, TraceID: {}, IP: {}",
                request.getMethod(),
                request.getRequestURI(),
                traceId,
                getClientIp(request));

        return true;
    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response,
                          Object handler, ModelAndView modelAndView) {
        // 可以在这里添加额外的处理逻辑
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) {
        String traceId = MDC.get(SystemConstants.TraceId.MDC_KEY);

        // 记录请求结束
        if (ex != null) {
            log.error("请求异常 - Method: {}, URI: {}, TraceID: {}, Exception: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    traceId,
                    ex.getMessage(), ex);
        } else {
            log.info("请求结束 - Method: {}, URI: {}, TraceID: {}, Status: {}",
                    request.getMethod(),
                    request.getRequestURI(),
                    traceId,
                    response.getStatus());
        }

        // 清除MDC
        TraceIdUtil.clear();
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 多次反向代理后会有多个IP值，第一个为真实IP
            int index = ip.indexOf(',');
            if (index != -1) {
                return ip.substring(0, index);
            } else {
                return ip;
            }
        }

        ip = request.getHeader("X-Real-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("Proxy-Client-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("WL-Proxy-Client-IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_CLIENT_IP");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        if (StringUtils.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }

        return request.getRemoteAddr();
    }
}