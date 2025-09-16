package com.haven.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP工具类
 * 提供HTTP请求的封装方法
 *
 * @author HavenButler
 */
@Slf4j
public final class HttpUtils {

    private static final RestTemplate REST_TEMPLATE = new RestTemplate();

    private HttpUtils() {
        throw new AssertionError("不允许实例化");
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
        try {
            // 构建URL
            if (params != null && !params.isEmpty()) {
                StringBuilder urlBuilder = new StringBuilder(url);
                urlBuilder.append("?");
                params.forEach((k, v) -> urlBuilder.append(k).append("=").append(v).append("&"));
                url = urlBuilder.substring(0, urlBuilder.length() - 1);
            }

            // 构建请求头
            HttpHeaders httpHeaders = new HttpHeaders();
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<?> entity = new HttpEntity<>(httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.GET, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("GET请求失败: url={}", url, e);
            throw new RuntimeException("HTTP GET请求失败", e);
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
        try {
            HttpHeaders httpHeaders = new HttpHeaders();
            httpHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (headers != null) {
                headers.forEach(httpHeaders::add);
            }

            HttpEntity<String> entity = new HttpEntity<>(json, httpHeaders);

            ResponseEntity<String> response = REST_TEMPLATE.exchange(
                url, HttpMethod.POST, entity, String.class);

            return response.getBody();
        } catch (Exception e) {
            log.error("POST请求失败: url={}", url, e);
            throw new RuntimeException("HTTP POST请求失败", e);
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
}