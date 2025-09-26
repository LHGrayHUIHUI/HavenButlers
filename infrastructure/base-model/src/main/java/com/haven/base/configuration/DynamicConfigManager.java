package com.haven.base.configuration;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * 动态配置管理接口
 * 支持配置热更新、变更通知等功能，适配Nacos、Apollo等配置中心
 *
 * @author HavenButler
 */
public interface DynamicConfigManager {

    /**
     * 获取字符串配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getString(String key, String defaultValue);

    /**
     * 获取整型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    Integer getInt(String key, Integer defaultValue);

    /**
     * 获取长整型配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    Long getLong(String key, Long defaultValue);

    /**
     * 获取布尔配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    Boolean getBoolean(String key, Boolean defaultValue);

    /**
     * 获取Double配置
     *
     * @param key 配置键
     * @param defaultValue 默认值
     * @return 配置值
     */
    Double getDouble(String key, Double defaultValue);

    /**
     * 获取对象配置（JSON反序列化）
     *
     * @param key 配置键
     * @param type 对象类型
     * @param defaultValue 默认值
     * @return 配置对象
     */
    <T> T getObject(String key, Class<T> type, T defaultValue);

    /**
     * 检查配置是否存在
     *
     * @param key 配置键
     * @return 是否存在
     */
    boolean containsKey(String key);

    /**
     * 添加配置变更监听器
     *
     * @param key 配置键
     * @param listener 变更监听器
     */
    void addConfigListener(String key, ConfigChangeListener listener);

    /**
     * 移除配置变更监听器
     *
     * @param key 配置键
     * @param listener 变更监听器
     */
    void removeConfigListener(String key, ConfigChangeListener listener);

    /**
     * 批量添加配置变更监听器（按前缀）
     *
     * @param keyPrefix 配置键前缀
     * @param listener 变更监听器
     */
    void addConfigListenerByPrefix(String keyPrefix, ConfigChangeListener listener);

    /**
     * 获取配置信息
     *
     * @param key 配置键
     * @return 配置信息
     */
    ConfigInfo getConfigInfo(String key);

    /**
     * 刷新配置缓存
     */
    void refreshCache();

    /**
     * 批量获取配置（按前缀）
     *
     * @param prefix 配置前缀
     * @return 配置映射
     */
    java.util.Map<String, String> getConfigsByPrefix(String prefix);

    /**
     * 配置变更监听器
     */
    @FunctionalInterface
    interface ConfigChangeListener {
        /**
         * 配置变更回调
         *
         * @param key 配置键
         * @param oldValue 旧值
         * @param newValue 新值
         */
        void onChange(String key, String oldValue, String newValue);
    }

    /**
     * 配置信息
     */
    class ConfigInfo {
        private String key;
        private String value;
        private String group;
        private String dataId;
        private long version;
        private java.time.LocalDateTime lastModified;
        private String source; // 配置来源：nacos, apollo, file等

        // Constructors
        public ConfigInfo() {}

        public ConfigInfo(String key, String value) {
            this.key = key;
            this.value = value;
            this.lastModified = java.time.LocalDateTime.now();
        }

        // Getters and Setters
        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }

        public String getGroup() { return group; }
        public void setGroup(String group) { this.group = group; }

        public String getDataId() { return dataId; }
        public void setDataId(String dataId) { this.dataId = dataId; }

        public long getVersion() { return version; }
        public void setVersion(long version) { this.version = version; }

        public java.time.LocalDateTime getLastModified() { return lastModified; }
        public void setLastModified(java.time.LocalDateTime lastModified) { this.lastModified = lastModified; }

        public String getSource() { return source; }
        public void setSource(String source) { this.source = source; }
    }
}