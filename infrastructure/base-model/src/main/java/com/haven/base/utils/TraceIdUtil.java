package com.haven.base.utils;

import com.haven.base.common.constants.SystemConstants;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * TraceID工具类
 * TraceID格式：tr-yyyyMMdd-HHmmss-随机6位
 * 示例：tr-20250115-143022-a3b5c7
 *
 * @author HavenButler
 */
public final class TraceIdUtil {

    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern(SystemConstants.TraceId.PATTERN);

    private TraceIdUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 生成TraceID
     * 所有服务间调用必须携带此ID用于链路追踪
     *
     * @return TraceID
     */
    public static String generate() {
        String timestamp = FORMATTER.format(LocalDateTime.now());
        String random = RandomStringUtils.randomAlphanumeric(SystemConstants.TraceId.RANDOM_LENGTH).toLowerCase();
        return SystemConstants.TraceId.PREFIX + timestamp + "-" + random;
    }

    /**
     * 获取当前TraceID
     * 如果不存在则生成新的
     *
     * @return TraceID
     */
    public static String getCurrentOrGenerate() {
        String traceId = MDC.get(SystemConstants.TraceId.MDC_KEY);
        if (StringUtils.isBlank(traceId)) {
            traceId = generate();
            setTraceId(traceId);
        }
        return traceId;
    }

    /**
     * 获取当前TraceID
     *
     * @return TraceID，如果不存在返回null
     */
    public static String getCurrent() {
        return MDC.get(SystemConstants.TraceId.MDC_KEY);
    }

    /**
     * 设置TraceID到MDC
     *
     * @param traceId TraceID
     */
    public static void setTraceId(String traceId) {
        if (StringUtils.isNotBlank(traceId)) {
            MDC.put(SystemConstants.TraceId.MDC_KEY, traceId);
        }
    }

    /**
     * 清除MDC中的TraceID
     */
    public static void clear() {
        MDC.remove(SystemConstants.TraceId.MDC_KEY);
    }

    /**
     * 验证TraceID格式是否有效
     *
     * @param traceId TraceID
     * @return 是否有效
     */
    public static boolean isValid(String traceId) {
        if (StringUtils.isBlank(traceId)) {
            return false;
        }
        // 检查格式：tr-yyyyMMdd-HHmmss-随机6位
        return traceId.matches("^tr-\\d{8}-\\d{6}-[a-z0-9]{6}$");
    }

    /**
     * 从TraceID中提取时间戳
     *
     * @param traceId TraceID
     * @return 时间戳，格式：yyyyMMdd-HHmmss
     */
    public static String extractTimestamp(String traceId) {
        if (!isValid(traceId)) {
            return null;
        }
        String[] parts = traceId.split("-");
        if (parts.length >= 3) {
            return parts[1] + "-" + parts[2];
        }
        return null;
    }

    /**
     * 从TraceID中提取日期
     *
     * @param traceId TraceID
     * @return 日期，格式：yyyyMMdd
     */
    public static String extractDate(String traceId) {
        if (!isValid(traceId)) {
            return null;
        }
        String[] parts = traceId.split("-");
        if (parts.length >= 2) {
            return parts[1];
        }
        return null;
    }

    /**
     * 传播TraceID到HTTP请求头
     *
     * @param headers HTTP请求头
     */
    public static void propagateToHeaders(org.springframework.http.HttpHeaders headers) {
        String traceId = getCurrentOrGenerate();
        headers.add(SystemConstants.Header.TRACE_ID, traceId);
    }

    /**
     * 从HTTP请求头中提取TraceID并设置到MDC
     *
     * @param headers HTTP请求头
     * @return 提取的TraceID
     */
    public static String extractFromHeaders(org.springframework.http.HttpHeaders headers) {
        String traceId = headers.getFirst(SystemConstants.Header.TRACE_ID);
        if (StringUtils.isNotBlank(traceId) && isValid(traceId)) {
            setTraceId(traceId);
            return traceId;
        }
        return null;
    }

    /**
     * 从Servlet请求中提取TraceID并设置到MDC
     *
     * @param request HTTP请求
     * @return 提取的TraceID
     */
    public static String extractFromRequest(jakarta.servlet.http.HttpServletRequest request) {
        String traceId = request.getHeader(SystemConstants.Header.TRACE_ID);
        if (StringUtils.isNotBlank(traceId) && isValid(traceId)) {
            setTraceId(traceId);
            return traceId;
        }
        return null;
    }

    /**
     * 清理当前线程的TraceID（在请求结束时调用）
     */
    public static void cleanup() {
        clear();
    }
}