package com.haven.base.client;

import com.haven.base.common.response.ErrorCode;
import com.haven.base.common.response.ResponseWrapper;
import com.haven.base.common.exception.SystemException;
import com.haven.base.config.ServiceClientProperties;
import com.haven.base.utils.JsonUtil;
import com.haven.base.utils.TraceIdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * 统一服务调用客户端
 * 提供微服务间通信的标准化接口，自动处理TraceID、认证信息、异常处理等
 *
 * 功能特性：
 * - 自动添加TraceID到请求头
 * - 统一异常处理和响应封装
 * - 支持GET、POST、PUT、DELETE等HTTP方法
 * - 自动JSON序列化/反序列化
 * - 集成重试和超时机制
 *
 * @author HavenButler
 */
@Slf4j
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册
@RequiredArgsConstructor
public class ServiceClient {

    private final RestTemplate restTemplate;
    private final ServiceDiscovery serviceDiscovery;
    private final ServiceClientProperties properties;

    /**
     * GET请求 - 获取单个对象
     *
     * @param serviceName 服务名称
     * @param path 请求路径
     * @param responseType 响应类型
     * @param uriVariables 路径变量
     * @return 响应结果
     */
    public <T> ResponseWrapper<T> get(String serviceName, String path,
                                     Class<T> responseType, Object... uriVariables) {
        return execute(serviceName, path, HttpMethod.GET, null, responseType, uriVariables);
    }

    /**
     * GET请求 - 获取复杂类型（如List）
     *
     * @param serviceName 服务名称
     * @param path 请求路径
     * @param responseType 响应类型引用
     * @param uriVariables 路径变量
     * @return 响应结果
     */
    public <T> ResponseWrapper<T> get(String serviceName, String path,
                                     ParameterizedTypeReference<ResponseWrapper<T>> responseType,
                                     Object... uriVariables) {
        return executeForParameterizedType(serviceName, path, HttpMethod.GET, null, responseType, uriVariables);
    }

    /**
     * POST请求 - 创建资源
     *
     * @param serviceName 服务名称
     * @param path 请求路径
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public <T> ResponseWrapper<T> post(String serviceName, String path,
                                      Object requestBody, Class<T> responseType) {
        return execute(serviceName, path, HttpMethod.POST, requestBody, responseType);
    }

    /**
     * PUT请求 - 更新资源
     *
     * @param serviceName 服务名称
     * @param path 请求路径
     * @param requestBody 请求体
     * @param responseType 响应类型
     * @param uriVariables 路径变量
     * @return 响应结果
     */
    public <T> ResponseWrapper<T> put(String serviceName, String path,
                                     Object requestBody, Class<T> responseType, Object... uriVariables) {
        return execute(serviceName, path, HttpMethod.PUT, requestBody, responseType, uriVariables);
    }

    /**
     * DELETE请求 - 删除资源
     *
     * @param serviceName 服务名称
     * @param path 请求路径
     * @param uriVariables 路径变量
     * @return 响应结果
     */
    public ResponseWrapper<Void> delete(String serviceName, String path, Object... uriVariables) {
        return execute(serviceName, path, HttpMethod.DELETE, null, Void.class, uriVariables);
    }

    /**
     * 通用HTTP请求执行方法
     */
    private <T> ResponseWrapper<T> execute(String serviceName, String path, HttpMethod method,
                                          Object requestBody, Class<T> responseType, Object... uriVariables) {
        if (properties.getRetry().isEnabled()) {
            return executeWithRetry(serviceName, path, method, requestBody, responseType, uriVariables);
        } else {
            return executeSingleAttempt(serviceName, path, method, requestBody, responseType, uriVariables);
        }
    }

    /**
     * 带重试的请求执行
     */
    private <T> ResponseWrapper<T> executeWithRetry(String serviceName, String path, HttpMethod method,
                                                   Object requestBody, Class<T> responseType, Object... uriVariables) {
        ServiceClientProperties.Retry retryConfig = properties.getRetry();
        int maxAttempts = retryConfig.getMaxAttempts();
        long interval = retryConfig.getInterval();
        double multiplier = retryConfig.getMultiplier();
        long maxInterval = retryConfig.getMaxInterval();

        Exception lastException = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return executeSingleAttempt(serviceName, path, method, requestBody, responseType, uriVariables);

            } catch (Exception e) {
                lastException = e;

                if (attempt == maxAttempts) {
                    // 最后一次尝试失败，抛出异常
                    log.error("服务调用重试{}次后仍然失败: {} {} {}", maxAttempts, method, serviceName, path);
                    break;
                }

                if (!shouldRetry(e, retryConfig)) {
                    // 不需要重试的异常，直接抛出
                    log.debug("异常不需要重试: {}", e.getClass().getSimpleName());
                    break;
                }

                // 计算重试间隔
                long currentInterval = Math.min((long) (interval * Math.pow(multiplier, attempt - 1)), maxInterval);

                log.warn("服务调用失败，{}ms后进行第{}次重试: {} {} {}, 错误: {}",
                        currentInterval, attempt, method, serviceName, path, e.getMessage());

                try {
                    Thread.sleep(currentInterval);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.warn("重试等待被中断");
                    break;
                }
            }
        }

        // 所有重试都失败了
        if (lastException instanceof RestClientException) {
            throw new SystemException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    String.format("调用服务[%s]失败: %s", serviceName, lastException.getMessage()));
        } else {
            throw new SystemException(ErrorCode.SYSTEM_ERROR,
                    String.format("调用服务[%s]异常: %s", serviceName, lastException.getMessage()));
        }
    }

    /**
     * 单次请求执行
     */
    private <T> ResponseWrapper<T> executeSingleAttempt(String serviceName, String path, HttpMethod method,
                                                       Object requestBody, Class<T> responseType, Object... uriVariables) {
        try {
            // 构建完整URL
            String serviceUrl = serviceDiscovery.getServiceUrl(serviceName);
            String fullUrl = serviceUrl + path;

            // 构建请求头
            HttpHeaders headers = buildHeaders();

            // 构建请求实体
            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, headers);

            // 记录请求日志
            logRequest(method, fullUrl, requestBody);

            long startTime = System.currentTimeMillis();

            // 执行请求
            ResponseEntity<ResponseWrapper<T>> responseEntity = restTemplate.exchange(
                    fullUrl, method, requestEntity,
                    new ParameterizedTypeReference<ResponseWrapper<T>>() {},
                    uriVariables
            );

            long duration = System.currentTimeMillis() - startTime;

            // 记录响应日志
            logResponse(method, fullUrl, HttpStatus.valueOf(responseEntity.getStatusCode().value()), duration);

            return responseEntity.getBody();

        } catch (RestClientException e) {
            log.debug("服务调用异常: {} {} {}, 错误: {}", method, serviceName, path, e.getMessage());
            throw e; // 重新抛出，由重试逻辑处理
        }
    }

    /**
     * 判断异常是否需要重试
     */
    private boolean shouldRetry(Exception e, ServiceClientProperties.Retry retryConfig) {
        // 检查异常类型
        String exceptionName = e.getClass().getName();
        for (String retryableException : retryConfig.getRetryableExceptions()) {
            if (exceptionName.equals(retryableException)) {
                return true;
            }
        }

        // 检查HTTP状态码（如果是HttpStatusCodeException）
        if (e instanceof org.springframework.web.client.HttpStatusCodeException) {
            org.springframework.web.client.HttpStatusCodeException httpException =
                (org.springframework.web.client.HttpStatusCodeException) e;
            int statusCode = httpException.getStatusCode().value();

            for (int retryableStatusCode : retryConfig.getRetryableStatusCodes()) {
                if (statusCode == retryableStatusCode) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * 执行参数化类型请求
     */
    private <T> ResponseWrapper<T> executeForParameterizedType(String serviceName, String path, HttpMethod method,
                                                              Object requestBody,
                                                              ParameterizedTypeReference<ResponseWrapper<T>> responseType,
                                                              Object... uriVariables) {
        try {
            String serviceUrl = serviceDiscovery.getServiceUrl(serviceName);
            String fullUrl = serviceUrl + path;
            HttpHeaders headers = buildHeaders();
            HttpEntity<?> requestEntity = new HttpEntity<>(requestBody, headers);

            logRequest(method, fullUrl, requestBody);
            long startTime = System.currentTimeMillis();

            ResponseEntity<ResponseWrapper<T>> responseEntity = restTemplate.exchange(
                    fullUrl, method, requestEntity, responseType, uriVariables
            );

            long duration = System.currentTimeMillis() - startTime;
            logResponse(method, fullUrl, HttpStatus.valueOf(responseEntity.getStatusCode().value()), duration);

            return responseEntity.getBody();

        } catch (RestClientException e) {
            log.error("服务调用失败: {} {} {}, 错误: {}", method, serviceName, path, e.getMessage(), e);
            throw new SystemException(ErrorCode.EXTERNAL_SERVICE_ERROR,
                    String.format("调用服务[%s]失败: %s", serviceName, e.getMessage()));
        }
    }

    /**
     * 构建请求头
     */
    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.Collections.singletonList(MediaType.APPLICATION_JSON));

        // 添加User-Agent
        if (properties.getHeaders().getUserAgent() != null) {
            headers.add("User-Agent", properties.getHeaders().getUserAgent());
        }

        // 自动添加TraceID
        String traceId = TraceIdUtil.getCurrentOrGenerate();
        String traceIdHeaderName = properties.getHeaders().getTraceIdName();
        headers.add(traceIdHeaderName, traceId);

        // 添加自定义请求头
        properties.getHeaders().getCustom().forEach(headers::add);

        // TODO: 可以在这里添加其他通用头信息
        // headers.add("Authorization", "Bearer " + getAuthToken());
        // String userIdHeaderName = properties.getHeaders().getUserIdName();
        // headers.add(userIdHeaderName, getCurrentUserId());

        return headers;
    }

    /**
     * 记录请求日志
     */
    private void logRequest(HttpMethod method, String url, Object requestBody) {
        if (log.isDebugEnabled()) {
            String bodyStr = requestBody != null ? JsonUtil.toJson(requestBody) : "null";
            log.debug("服务调用开始: {} {}, 请求体: {}", method, url, bodyStr);
        } else {
            log.info("服务调用开始: {} {}", method, url);
        }
    }

    /**
     * 记录响应日志
     */
    private void logResponse(HttpMethod method, String url, HttpStatus status, long duration) {
        log.info("服务调用完成: {} {}, 状态码: {}, 耗时: {}ms", method, url, status.value(), duration);
    }

    /**
     * 检查服务健康状态
     */
    public boolean isServiceHealthy(String serviceName) {
        return serviceDiscovery.isServiceHealthy(serviceName);
    }

    /**
     * 获取服务实例列表
     */
    public java.util.List<String> getServiceInstances(String serviceName) {
        return serviceDiscovery.getServiceInstances(serviceName);
    }
}