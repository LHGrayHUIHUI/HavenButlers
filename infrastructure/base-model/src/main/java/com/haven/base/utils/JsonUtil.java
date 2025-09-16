package com.haven.base.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;

/**
 * JSON工具类
 * 基于Jackson实现的JSON序列化和反序列化工具
 *
 * @author HavenButler
 */
@Slf4j
public final class JsonUtil {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        // 配置ObjectMapper
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
        OBJECT_MAPPER.setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"));
        OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        OBJECT_MAPPER.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        OBJECT_MAPPER.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
    }

    private JsonUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 对象转JSON字符串
     *
     * @param object 待转换对象
     * @return JSON字符串
     */
    public static String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            throw new SystemException("JSON序列化失败", e);
        }
    }

    /**
     * 对象转格式化的JSON字符串
     *
     * @param object 待转换对象
     * @return 格式化的JSON字符串
     */
    public static String toPrettyJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("对象转JSON失败", e);
            throw new SystemException("JSON序列化失败", e);
        }
    }

    /**
     * JSON字符串转对象
     *
     * @param json  JSON字符串
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败, json={}, class={}", json, clazz.getName(), e);
            throw new SystemException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON字符串转对象（复杂类型）
     *
     * @param json          JSON字符串
     * @param typeReference 类型引用
     * @param <T>           泛型
     * @return 对象实例
     */
    public static <T> T fromJson(String json, TypeReference<T> typeReference) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, typeReference);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败, json={}", json, e);
            throw new SystemException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON字符串转List
     *
     * @param json  JSON字符串
     * @param clazz 元素类型
     * @param <T>   泛型
     * @return List实例
     */
    public static <T> List<T> fromJsonToList(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json,
                    OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            log.error("JSON转List失败, json={}, class={}", json, clazz.getName(), e);
            throw new SystemException("JSON反序列化失败", e);
        }
    }

    /**
     * JSON字符串转Map
     *
     * @param json JSON字符串
     * @return Map实例
     */
    public static Map<String, Object> fromJsonToMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (JsonProcessingException e) {
            log.error("JSON转Map失败, json={}", json, e);
            throw new SystemException("JSON反序列化失败", e);
        }
    }

    /**
     * 对象转换
     *
     * @param source 源对象
     * @param clazz  目标类型
     * @param <T>    泛型
     * @return 目标对象
     */
    public static <T> T convert(Object source, Class<T> clazz) {
        if (source == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(source, clazz);
    }

    /**
     * 对象转Map
     *
     * @param object 源对象
     * @return Map
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> objectToMap(Object object) {
        if (object == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(object, Map.class);
    }

    /**
     * Map转对象
     *
     * @param map   Map
     * @param clazz 目标类型
     * @param <T>   泛型
     * @return 目标对象
     */
    public static <T> T mapToObject(Map<String, Object> map, Class<T> clazz) {
        if (map == null) {
            return null;
        }
        return OBJECT_MAPPER.convertValue(map, clazz);
    }

    /**
     * 验证JSON格式是否有效
     *
     * @param json JSON字符串
     * @return 是否有效
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        try {
            OBJECT_MAPPER.readTree(json);
            return true;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    /**
     * 获取ObjectMapper实例
     * 仅在需要自定义配置时使用
     *
     * @return ObjectMapper
     */
    public static ObjectMapper getObjectMapper() {
        return OBJECT_MAPPER;
    }
}