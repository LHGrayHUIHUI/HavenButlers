package com.haven.base.configuration;

import com.haven.base.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 默认动态配置管理实现
 * 基于Spring Environment的简单实现，适合开发和测试环境
 * 生产环境建议使用Nacos、Apollo等专业配置中心
 *
 * @author HavenButler
 */
@Slf4j
// 移除@Component注解，改由BaseModelAutoConfiguration中@Bean方式注册

public class DefaultDynamicConfigManager implements DynamicConfigManager {

    private final Environment environment;

    /**
     * 配置监听器映射
     */
    private final Map<String, List<ConfigChangeListener>> listeners = new ConcurrentHashMap<>();

    /**
     * 配置缓存
     */
    private final Map<String, ConfigInfo> configCache = new ConcurrentHashMap<>();

    public DefaultDynamicConfigManager(Environment environment) {
        this.environment = environment;
        log.info("默认动态配置管理器初始化完成");
    }

    @Override
    public String getString(String key, String defaultValue) {
        String value = environment.getProperty(key, defaultValue);
        cacheConfig(key, value, "string");
        return value;
    }

    @Override
    public Integer getInt(String key, Integer defaultValue) {
        Integer value = environment.getProperty(key, Integer.class, defaultValue);
        cacheConfig(key, value != null ? value.toString() : null, "integer");
        return value;
    }

    @Override
    public Long getLong(String key, Long defaultValue) {
        Long value = environment.getProperty(key, Long.class, defaultValue);
        cacheConfig(key, value != null ? value.toString() : null, "long");
        return value;
    }

    @Override
    public Boolean getBoolean(String key, Boolean defaultValue) {
        Boolean value = environment.getProperty(key, Boolean.class, defaultValue);
        cacheConfig(key, value != null ? value.toString() : null, "boolean");
        return value;
    }

    @Override
    public Double getDouble(String key, Double defaultValue) {
        Double value = environment.getProperty(key, Double.class, defaultValue);
        cacheConfig(key, value != null ? value.toString() : null, "double");
        return value;
    }

    @Override
    public <T> T getObject(String key, Class<T> type, T defaultValue) {
        String jsonValue = environment.getProperty(key);
        if (jsonValue == null) {
            return defaultValue;
        }

        try {
            T value = JsonUtil.fromJson(jsonValue, type);
            cacheConfig(key, jsonValue, "object:" + type.getSimpleName());
            return value;
        } catch (Exception e) {
            log.warn("配置[{}]JSON解析失败: {}", key, e.getMessage());
            return defaultValue;
        }
    }

    @Override
    public boolean containsKey(String key) {
        return environment.containsProperty(key);
    }

    @Override
    public void addConfigListener(String key, ConfigChangeListener listener) {
        listeners.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("添加配置监听器: key={}", key);
    }

    @Override
    public void removeConfigListener(String key, ConfigChangeListener listener) {
        List<ConfigChangeListener> keyListeners = listeners.get(key);
        if (keyListeners != null) {
            keyListeners.remove(listener);
            if (keyListeners.isEmpty()) {
                listeners.remove(key);
            }
        }
        log.debug("移除配置监听器: key={}", key);
    }

    @Override
    public void addConfigListenerByPrefix(String keyPrefix, ConfigChangeListener listener) {
        listeners.computeIfAbsent(keyPrefix + "*", k -> new CopyOnWriteArrayList<>()).add(listener);
        log.debug("添加前缀配置监听器: keyPrefix={}", keyPrefix);
    }

    @Override
    public ConfigInfo getConfigInfo(String key) {
        return configCache.get(key);
    }

    @Override
    public void refreshCache() {
        configCache.clear();
        log.info("配置缓存已刷新");
    }

    @Override
    public Map<String, String> getConfigsByPrefix(String prefix) {
        Map<String, String> result = new ConcurrentHashMap<>();

        // 注意：Spring Environment没有直接按前缀获取的方法
        // 这里提供基础实现，生产环境建议使用专业配置中心
        log.warn("按前缀获取配置功能有限，建议使用Nacos等配置中心: prefix={}", prefix);

        return result;
    }

    /**
     * 缓存配置信息
     */
    private void cacheConfig(String key, String value, String type) {
        if (value != null) {
            ConfigInfo configInfo = new ConfigInfo(key, value);
            configInfo.setSource("spring-environment");
            configInfo.setGroup("default");
            configInfo.setDataId(key);
            configCache.put(key, configInfo);
        }
    }

    /**
     * 通知配置变更（模拟）
     * 注意：Spring Environment本身不支持热更新
     * 这个方法主要用于手动触发变更通知
     */
    public void notifyConfigChange(String key, String oldValue, String newValue) {
        List<ConfigChangeListener> keyListeners = listeners.get(key);
        if (keyListeners != null) {
            for (ConfigChangeListener listener : keyListeners) {
                try {
                    listener.onChange(key, oldValue, newValue);
                    log.debug("配置变更通知: key={}, oldValue={}, newValue={}", key, oldValue, newValue);
                } catch (Exception e) {
                    log.error("配置变更监听器执行失败: key={}, error={}", key, e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 获取配置统计信息
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("cachedConfigs", configCache.size());
        stats.put("listeners", listeners.size());
        stats.put("source", "spring-environment");
        return stats;
    }
}