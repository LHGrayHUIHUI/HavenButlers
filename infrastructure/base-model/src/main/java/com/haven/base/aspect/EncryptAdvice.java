package com.haven.base.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.haven.base.annotation.Encrypt;
import com.haven.base.utils.EncryptUtil;
import com.haven.base.utils.JsonUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * 加密响应通知
 * 处理@Encrypt注解标记的响应数据加密和脱敏
 * 注册方式：通过BaseModelAutoConfiguration中的@Bean方式注册
 *
 * @author HavenButler
 */
@Slf4j
@RequiredArgsConstructor
@Order(100)
public class EncryptAdvice implements ResponseBodyAdvice<Object> {

    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // 检查方法是否有@Encrypt注解
        return returnType.hasMethodAnnotation(Encrypt.class);
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                MediaType selectedContentType,
                                Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                ServerHttpRequest request, ServerHttpResponse response) {

        if (body == null) {
            return null;
        }

        Encrypt encrypt = returnType.getMethodAnnotation(Encrypt.class);
        if (encrypt == null) {
            return body;
        }

        try {
            return processEncryption(body, encrypt);
        } catch (Exception e) {
            log.error("响应数据加密处理失败: {}", e.getMessage(), e);
            // 加密失败时根据策略处理
            if (encrypt.failOnError()) {
                throw new RuntimeException("数据加密失败", e);
            } else {
                // 返回原始数据（可能包含脱敏）
                try {
                    return processMasking(body, encrypt);
                } catch (Exception maskingException) {
                    log.error("数据脱敏处理失败: {}", maskingException.getMessage(), maskingException);
                    return body;
                }
            }
        }
    }

    /**
     * 处理加密逻辑
     */
    private Object processEncryption(Object body, Encrypt encrypt) throws Exception {
        if (encrypt.type() == Encrypt.Type.FULL) {
            // 全量加密：整个响应体加密
            return encryptFullResponse(body, encrypt);
        } else {
            // 字段级加密：对特定字段进行加密或脱敏
            return processFieldEncryption(body, encrypt);
        }
    }

    /**
     * 全量响应加密
     */
    private Object encryptFullResponse(Object body, Encrypt encrypt) throws Exception {
        // 将响应体序列化为JSON
        String jsonString = JsonUtil.toJson(body);

        // 根据算法进行加密
        String encryptedData;
        switch (encrypt.algorithm()) {
            case AES:
                encryptedData = EncryptUtil.encryptAES(jsonString, encrypt.key());
                break;
            case RSA:
                encryptedData = EncryptUtil.encryptRSA(jsonString, encrypt.key());
                break;
            default:
                throw new IllegalArgumentException("不支持的加密算法: " + encrypt.algorithm());
        }

        // 返回加密结果封装
        return Map.of(
            "encrypted", true,
            "algorithm", encrypt.algorithm().name(),
            "data", encryptedData,
            "timestamp", System.currentTimeMillis()
        );
    }

    /**
     * 字段级加密处理
     */
    private Object processFieldEncryption(Object body, Encrypt encrypt) {
        if (body == null) {
            return null;
        }

        try {
            // 克隆对象以避免修改原对象
            Object clonedBody = cloneObject(body);

            // 处理字段加密和脱敏
            processObjectFields(clonedBody, encrypt);

            return clonedBody;

        } catch (Exception e) {
            log.error("字段级加密处理失败: {}", e.getMessage(), e);
            throw new RuntimeException("字段加密失败", e);
        }
    }

    /**
     * 处理对象字段
     */
    private void processObjectFields(Object obj, Encrypt encrypt) throws Exception {
        if (obj == null) {
            return;
        }

        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);

            // 检查字段是否有@Encrypt注解
            Encrypt fieldEncrypt = field.getAnnotation(Encrypt.class);
            if (fieldEncrypt != null) {
                processField(obj, field, fieldEncrypt);
            } else if (shouldProcessField(field.getName(), encrypt)) {
                // 根据方法级注解的配置处理字段
                processField(obj, field, encrypt);
            }
        }
    }

    /**
     * 处理单个字段
     */
    private void processField(Object obj, Field field, Encrypt encrypt) throws Exception {
        Object fieldValue = field.get(obj);
        if (fieldValue == null) {
            return;
        }

        if (fieldValue instanceof String) {
            String strValue = (String) fieldValue;

            if (encrypt.mask()) {
                // 脱敏处理
                String maskedValue = maskSensitiveData(strValue, field.getName());
                field.set(obj, maskedValue);
            } else {
                // 加密处理
                String encryptedValue = encryptFieldValue(strValue, encrypt);
                field.set(obj, encryptedValue);
            }
        }
        // TODO: 处理其他类型的字段（如嵌套对象）
    }

    /**
     * 判断是否需要处理指定字段
     */
    private boolean shouldProcessField(String fieldName, Encrypt encrypt) {
        if (encrypt.fields().length == 0) {
            return false;
        }

        for (String targetField : encrypt.fields()) {
            if (targetField.equals(fieldName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 加密字段值
     */
    private String encryptFieldValue(String value, Encrypt encrypt) throws Exception {
        switch (encrypt.algorithm()) {
            case AES:
                return EncryptUtil.encryptAES(value, encrypt.key());
            case RSA:
                return EncryptUtil.encryptRSA(value, encrypt.key());
            default:
                throw new IllegalArgumentException("不支持的加密算法: " + encrypt.algorithm());
        }
    }

    /**
     * 脱敏处理
     */
    private String maskSensitiveData(String value, String fieldName) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // 根据字段名进行不同的脱敏处理
        switch (fieldName.toLowerCase()) {
            case "phone":
            case "mobile":
                return maskPhone(value);
            case "email":
                return maskEmail(value);
            case "idcard":
            case "idnumber":
                return maskIdCard(value);
            case "bankcard":
            case "cardnumber":
                return maskBankCard(value);
            default:
                // 默认脱敏：保留前后各2位
                return maskDefault(value);
        }
    }

    /**
     * 手机号脱敏
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 邮箱脱敏
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        String[] parts = email.split("@");
        String username = parts[0];
        String domain = parts[1];

        if (username.length() <= 2) {
            return email;
        }
        return username.substring(0, 2) + "****@" + domain;
    }

    /**
     * 身份证脱敏
     */
    private String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡脱敏
     */
    private String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + "********" + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 默认脱敏
     */
    private String maskDefault(String value) {
        if (value == null || value.length() < 4) {
            return value;
        }
        return value.substring(0, 2) + "****" + value.substring(value.length() - 2);
    }

    /**
     * 处理脱敏逻辑（当加密失败时的备用方案）
     */
    private Object processMasking(Object body, Encrypt encrypt) {
        try {
            if (encrypt.mask()) {
                Object clonedBody = cloneObject(body);
                processObjectFields(clonedBody, encrypt);
                return clonedBody;
            }
            return body;
        } catch (Exception e) {
            log.error("数据脱敏处理失败: {}", e.getMessage(), e);
            return body;
        }
    }

    /**
     * 克隆对象
     */
    private Object cloneObject(Object obj) throws Exception {
        // 使用JSON序列化/反序列化进行深度克隆
        String json = JsonUtil.toJson(obj);
        return JsonUtil.fromJson(json, obj.getClass());
    }
}