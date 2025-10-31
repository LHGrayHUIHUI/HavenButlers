package com.haven.base.security;

import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.configuration.DynamicConfigManager;
import com.haven.base.utils.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 基于配置中心的密钥管理器实现
 * 支持密钥的动态获取、轮换和生命周期管理
 *
 * @author HavenButler
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConfigCenterKeyManager implements KeyManager {

    private final DynamicConfigManager configManager;

    // 密钥缓存，减少配置访问
    private final ConcurrentHashMap<String, CacheEntry> keyCache = new ConcurrentHashMap<>();

    // 密钥轮换计数器
    private final AtomicLong rotationCounter = new AtomicLong(0);

    // 缓存过期时间（毫秒）
    private static final long CACHE_TTL = 5 * 60 * 1000; // 5分钟

    @Override
    public String getEncryptionKey(String keyId) {
        return getKey("security.keys.encryption." + keyId, "AES", 256);
    }

    @Override
    public String getSigningKey(String keyId) {
        return getKey("security.keys.signing." + keyId, "HMAC", 256);
    }

    @Override
    public String getAESKey(String keyId) {
        return getEncryptionKey(keyId); // AES密钥使用加密密钥
    }

    @Override
    public String getRSAPublicKey(String keyId) {
        return getKeyPair("security.keys.rsa." + keyId, "public");
    }

    @Override
    public String getRSAPrivateKey(String keyId) {
        return getKeyPair("security.keys.rsa." + keyId, "private");
    }

    @Override
    public void rotateKey(String keyId) {
        try {
            log.info("开始轮换密钥: {}", keyId);

            // 生成新密钥
            String newKey = generateNewKeyForType(keyId);

            // 更新配置（这里需要扩展DynamicConfigManager支持设置配置，暂时使用监听器模式）
            String configKey = getConfigKeyForKeyId(keyId);
            // TODO: 需要扩展DynamicConfigManager接口支持setProperty
            // configManager.setProperty(configKey, newKey);
            log.warn("密钥轮换功能需要DynamicConfigManager支持setProperty方法，当前仅记录日志");

            // 记录轮换时间
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            // configManager.setProperty(configKey + ".last-rotation", timestamp);

            // 清除缓存
            invalidateCache(keyId);

            rotationCounter.incrementAndGet();
            log.info("密钥轮换完成: {}, 轮换次数: {}", keyId, rotationCounter.get());

        } catch (Exception e) {
            log.error("密钥轮换失败: {}", keyId, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥轮换失败: " + keyId);
        }
    }

    @Override
    public int validateKeyStrength(String key) {
        return EncryptUtil.validateKeyStrength(key);
    }

    @Override
    public boolean isKeyExpiringSoon(String keyId, int daysThreshold) {
        try {
            String configKey = getConfigKeyForKeyId(keyId) + ".last-rotation";
            String lastRotationStr = configManager.getString(configKey, null);

            if (lastRotationStr == null) {
                return true; // 如果没有轮换记录，建议立即轮换
            }

            LocalDateTime lastRotation = LocalDateTime.parse(lastRotationStr);
            LocalDateTime expiryThreshold = lastRotation.plusDays(daysThreshold);

            return LocalDateTime.now().isAfter(expiryThreshold);

        } catch (Exception e) {
            log.warn("检查密钥过期状态失败: {}", keyId, e);
            return true; // 出错时保守处理，建议轮换
        }
    }

    @Override
    public String generateSecureKey(String keyType, int keySize) {
        try {
            switch (keyType.toUpperCase()) {
                case "AES":
                    return EncryptUtil.generateAESKey(keySize);
                case "RSA":
                    String[] keyPair = EncryptUtil.generateRSAKeyPair(keySize);
                    return keyPair[0]; // 返回公钥
                case "HMAC":
                    byte[] seed = new byte[keySize / 8];
                    java.util.concurrent.ThreadLocalRandom.current().nextBytes(seed);
                    return Base64.getEncoder().encodeToString(seed);
                default:
                    throw new SystemException(ErrorCode.PARAM_ERROR, "不支持的密钥类型: " + keyType);
            }
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "生成密钥失败: " + keyType);
        }
    }

    /**
     * 获取密钥，带缓存
     */
    private String getKey(String configKey, String keyType, int keySize) {
        CacheEntry cached = keyCache.get(configKey);
        if (cached != null && !cached.isExpired()) {
            return cached.getValue();
        }

        try {
            String key = configManager.getString(configKey, null);
            if (key == null) {
                // 如果配置中没有密钥，生成一个新密钥
                key = generateSecureKey(keyType, keySize);
                // TODO: 需要扩展DynamicConfigManager接口支持setProperty
                // configManager.setProperty(configKey, key);
                log.warn("为配置项 {} 生成新密钥，但需要DynamicConfigManager支持setProperty方法", configKey);
            }

            // 缓存密钥
            keyCache.put(configKey, new CacheEntry(key, System.currentTimeMillis() + CACHE_TTL));
            return key;

        } catch (Exception e) {
            log.error("获取密钥失败: {}", configKey, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥获取失败: " + configKey);
        }
    }

    /**
     * 获取密钥对（用于RSA）
     */
    private String getKeyPair(String configKey, String keyType) {
        String key = getKey(configKey + "." + keyType, "RSA", 2048);
        return key;
    }

    /**
     * 根据密钥ID生成新密钥
     */
    private String generateNewKeyForType(String keyId) {
        if (keyId.contains("rsa")) {
            return generateSecureKey("RSA", 2048);
        } else if (keyId.contains("hmac")) {
            return generateSecureKey("HMAC", 256);
        } else {
            return generateSecureKey("AES", 256);
        }
    }

    /**
     * 根据密钥ID获取配置键
     */
    private String getConfigKeyForKeyId(String keyId) {
        if (keyId.contains("encryption")) {
            return "security.keys.encryption." + keyId.replace("encryption.", "");
        } else if (keyId.contains("signing")) {
            return "security.keys.signing." + keyId.replace("signing.", "");
        } else if (keyId.contains("rsa")) {
            return "security.keys.rsa." + keyId.replace("rsa.", "");
        } else {
            return "security.keys." + keyId;
        }
    }

    /**
     * 清除指定密钥的缓存
     */
    private void invalidateCache(String keyId) {
        keyCache.entrySet().removeIf(entry -> entry.getKey().contains(keyId));
    }

    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        keyCache.clear();
        log.info("密钥缓存已清空");
    }

    /**
     * 获取密钥轮换统计信息
     */
    public long getRotationCount() {
        return rotationCounter.get();
    }

    /**
     * 缓存条目
     */
    private static class CacheEntry {
        private final String value;
        private final long expiryTime;

        public CacheEntry(String value, long expiryTime) {
            this.value = value;
            this.expiryTime = expiryTime;
        }

        public String getValue() {
            return value;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() > expiryTime;
        }
    }
}