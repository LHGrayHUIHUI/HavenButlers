package com.haven.common.utils;

import com.haven.base.client.ServiceClient;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import com.haven.common.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.Map;

/**
 * HTTP工具类 - ServiceClient适配版本
 * 提供HTTP请求封装，内部委托给base-model的ServiceClient以获得更好的服务间通信支持
 *
 * 功能特性：
 * - 自动集成ServiceClient的服务发现、负载均衡、重试等功能
 * - 保持原有API的向后兼容性
 * - 支持traceId透传、异常映射
 * - 外部URL使用原RestTemplate，内部服务调用使用ServiceClient
 *
 * @author HavenButler
 * @version 3.0.0 - 集成ServiceClient架构
 */
@Slf4j
@Component
public final class HttpUtils {

    private static final RestTemplate REST_TEMPLATE;
    private static ServiceClient serviceClient;

    static {
        REST_TEMPLATE = createRestTemplate();
    }

    @Autowired(required = false)
    private ServiceClient injectedServiceClient;

    @PostConstruct
    public void init() {
        serviceClient = injectedServiceClient;
        if (serviceClient != null) {
            log.info("HttpUtils已集成ServiceClient，支持服务发现和重试机制");
        } else {
            log.warn("ServiceClient未配置，HttpUtils将使用传统RestTemplate模式");
        }
    }

    private HttpUtils() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 创建增强的RestTemplate（用于外部URL访问）
     */
    private static RestTemplate createRestTemplate() {
        RestTemplate template = new RestTemplate();

        // 添加traceId拦截器
        template.getInterceptors().add(createTraceIdInterceptor());

        return template;
    }

    /**
     * 创建traceId拦截器
     */
    private static ClientHttpRequestInterceptor createTraceIdInterceptor() {
        return (request, body, execution) -> {
            // 自动注入traceId到请求头
            String traceId = TraceIdUtil.getCurrent();
            if (traceId != null) {
                request.getHeaders().add("X-Trace-ID", traceId);
            }

            // 添加公共请求头
            request.getHeaders().add("User-Agent", "HavenButler-Common/2.0.0");
            request.getHeaders().add("X-Source", "haven-common-http");

            return execution.execute(request, body);
        };
    }

    /**
     * GET请求
     */
    public static String get(String url) {
        return get(url, null, null);
    }

    /**
     * GET请求（带参数）
     */
    public static String get(String url, Map<String, String> params) {
        return get(url, params, null);
    }

    /**
     * GET请求（带参数和请求头）
     */
    public static String get(String url, Map<String, String> params, Map<String, String> headers) {
        // 智能路由：判断是内部服务调用还是外部URL访问
        ServiceCallInfo callInfo = parseServiceCall(url);

        if (callInfo.isServiceCall && serviceClient != null) {
            return getViaServiceClient(callInfo.serviceName, callInfo.path, params, headers);
        } else {
            return getViaRestTemplate(url, params, headers);
        }
    }

    /**
     * 通过ServiceClient发送GET请求（内部服务调用）
     */
    private static String getViaServiceClient(String serviceName, String path,
                                            Map<String, String> params, Map<String, String> headers) {
        try {
            // 构建查询参数
            String fullPath = buildUrlWithParams(path, params);

            // ServiceClient已自动处理traceId、重试等
            ResponseWrapper<String> response = serviceClient.get(serviceName, fullPath, String.class);

            if (response.isSuccess()) {
                return response.getData();
            } else {
                throw new CommonException.HttpException("服务调用失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("ServiceClient GET请求失败: service={}, path={}", serviceName, path, e);
            throw new CommonException.HttpException("ServiceClient GET请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过RestTemplate发送GET请求（外部URL访问）
     */
    private static String getViaRestTemplate(String url, Map<String, String> params, Map<String, String> headers) {
        String traceId = TraceIdUtil.getCurrent();
        long startTime = System.currentTimeMillis();

        try {
            // 构建URL
            String finalUrl = buildUrlWithParams(url, params);

            // 构建请求头（traceId已通过拦截器自动注入）
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                finalUrl, HttpMethod.GET, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("GET请求成功: url={}, duration={}ms, traceId={}", finalUrl, duration, traceId);

            return response.getBody();
        } catch (RestClientException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("GET请求失败: url={}, duration={}ms, traceId={}", url, duration, traceId, e);
            throw new CommonException.HttpException("HTTP GET请求失败: " + e.getMessage(), e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("GET请求异常: url={}, duration={}ms, traceId={}", url, duration, traceId, e);
            throw new CommonException.HttpException("HTTP GET请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * POST请求（JSON）
     */
    public static String postJson(String url, String json) {
        return postJson(url, json, null);
    }

    /**
     * POST请求（JSON，带请求头）
     */
    public static String postJson(String url, String json, Map<String, String> headers) {
        // 智能路由：判断是内部服务调用还是外部URL访问
        ServiceCallInfo callInfo = parseServiceCall(url);

        if (callInfo.isServiceCall && serviceClient != null) {
            return postViaServiceClient(callInfo.serviceName, callInfo.path, json, headers);
        } else {
            return postViaRestTemplate(url, json, headers);
        }
    }

    /**
     * 通过ServiceClient发送POST请求（内部服务调用）
     */
    private static String postViaServiceClient(String serviceName, String path,
                                             String json, Map<String, String> headers) {
        try {
            // 将JSON字符串转换为对象（ServiceClient内部会重新序列化）
            Object requestBody = StringUtils.hasText(json) ? JsonUtil.fromJson(json, Object.class) : null;

            ResponseWrapper<String> response = serviceClient.post(serviceName, path, requestBody, String.class);

            if (response.isSuccess()) {
                return response.getData();
            } else {
                throw new CommonException.HttpException("服务调用失败: " + response.getMessage());
            }
        } catch (Exception e) {
            log.error("ServiceClient POST请求失败: service={}, path={}", serviceName, path, e);
            throw new CommonException.HttpException("ServiceClient POST请求失败: " + e.getMessage(), e);
        }
    }

    /**
     * 通过RestTemplate发送POST请求（外部URL访问）
     */
    private static String postViaRestTemplate(String url, String json, Map<String, String> headers) {
        String traceId = TraceIdUtil.getCurrent();
        long startTime = System.currentTimeMillis();

        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(json, httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.POST, entity, String.class);

            long duration = System.currentTimeMillis() - startTime;
            log.debug("POST请求成功: url={}, duration={}ms, traceId={}", url, duration, traceId);

            return response.getBody();
        } catch (RestClientException e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("POST请求失败: url={}, duration={}ms, traceId={}", url, duration, traceId, e);
            throw new CommonException.HttpException("HTTP POST请求失败: " + e.getMessage(), e);
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("POST请求异常: url={}, duration={}ms, traceId={}", url, duration, traceId, e);
            throw new CommonException.HttpException("HTTP POST请求异常: " + e.getMessage(), e);
        }
    }

    /**
     * POST请求（表单）
     */
    public static String postForm(String url, Map<String, String> params) {
        return postForm(url, params, null);
    }

    /**
     * POST请求（表单，带请求头）
     */
    public static String postForm(String url, Map<String, String> params, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
            if (params != null) {
                params.forEach(map::add);
            }

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("POST表单请求失败: url={}", url, e);
            throw new RuntimeException("HTTP POST表单请求失败", e);
        }
    }

    /**
     * PUT请求
     */
    public static String put(String url, String json) {
        return put(url, json, null);
    }

    /**
     * PUT请求（带请求头）
     */
    public static String put(String url, String json, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(json, httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.PUT, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("PUT请求失败: url={}", url, e);
            throw new RuntimeException("HTTP PUT请求失败", e);
        }
    }

    /**
     * DELETE请求
     */
    public static String delete(String url) {
        return delete(url, null);
    }

    /**
     * DELETE请求（带请求头）
     */
    public static String delete(String url, Map<String, String> headers) {
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.DELETE, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("DELETE请求失败: url={}", url, e);
            throw new RuntimeException("HTTP DELETE请求失败", e);
        }
    }

    /**
     * 通用请求方法
     */
    public static ResponseEntity<String> request(String url, HttpMethod method,
                                                 HttpHeaders headers, String body) {
        try {
            HttpEntity<String> entity = new HttpEntity<>(body, headers);
            return REST_TEMPLATE.exchange(url, method, entity, String.class);
        } catch (Exception e) {
            log.error("HTTP请求失败: url={}, method={}", url, method, e);
            throw new RuntimeException("HTTP请求失败", e);
        }
    }

    /**
     * 解析服务调用信息
     * 支持格式：service://serviceName/path 或 http://serviceName/path（内部调用）
     */
    private static ServiceCallInfo parseServiceCall(String url) {
        if (url == null) {
            return new ServiceCallInfo(false, null, null);
        }

        // 检查service://协议
        if (url.startsWith("service://")) {
            String remaining = url.substring("service://".length());
            int slashIndex = remaining.indexOf('/');
            if (slashIndex > 0) {
                String serviceName = remaining.substring(0, slashIndex);
                String path = remaining.substring(slashIndex);
                return new ServiceCallInfo(true, serviceName, path);
            }
        }

        // 检查是否为内部服务调用（没有协议前缀，且不包含端口和IP）
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            // 可能是相对路径的内部调用，如 account-service/api/v1/users
            int slashIndex = url.indexOf('/');
            if (slashIndex > 0) {
                String serviceName = url.substring(0, slashIndex);
                String path = url.substring(slashIndex);
                // 简单判断：如果serviceName看起来像服务名（包含-或_），则认为是内部调用
                if (serviceName.contains("-") || serviceName.contains("_")) {
                    return new ServiceCallInfo(true, serviceName, path);
                }
            }
        }

        // 其他情况认为是外部URL调用
        return new ServiceCallInfo(false, null, url);
    }

    /**
     * 构建带参数的URL
     */
    private static String buildUrlWithParams(String url, Map<String, String> params) {
        if (params == null || params.isEmpty()) {
            return url;
        }

        StringBuilder urlBuilder = new StringBuilder(url);
        urlBuilder.append(url.contains("?") ? "&" : "?");
        params.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));

        // 移除最后的&
        return urlBuilder.substring(0, urlBuilder.length() - 1);
    }

    /**
     * 服务调用信息
     */
    private static class ServiceCallInfo {
        final boolean isServiceCall;
        final String serviceName;
        final String path;

        ServiceCallInfo(boolean isServiceCall, String serviceName, String path) {
            this.isServiceCall = isServiceCall;
            this.serviceName = serviceName;
            this.path = path;
        }
    }
}