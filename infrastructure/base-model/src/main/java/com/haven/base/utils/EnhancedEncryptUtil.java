package com.haven.base.utils;

import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import com.haven.base.security.KeyManager;
import lombok.extern.slf4j.Slf4j;

/**
 * 增强版加密工具类
 * 支持密钥管理器集成，提供更安全的密钥管理
 *
 * @author HavenButler
 */
@Slf4j
public final class EnhancedEncryptUtil {

    private static KeyManager keyManager;
    private static volatile boolean keyManagerEnabled = false;

    private EnhancedEncryptUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * 设置密钥管理器
     */
    public static void setKeyManager(KeyManager manager) {
        keyManager = manager;
        keyManagerEnabled = (manager != null);
        log.info("密钥管理器已{}，启用状态: {}",
            manager != null ? "设置" : "清除", keyManagerEnabled);
    }

    /**
     * 检查是否启用密钥管理器
     */
    public static boolean isKeyManagerEnabled() {
        return keyManagerEnabled;
    }

    /**
     * AES-256-GCM加密（支持密钥管理器）
     *
     * @param plainText 明文
     * @param keyOrKeyId 密钥或密钥ID（当启用密钥管理器时）
     * @return Base64编码的密文
     */
    public static String encryptAES(String plainText, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            // 使用密钥管理器获取密钥
            try {
                String actualKey = keyManager.getAESKey(keyOrKeyId);
                log.debug("使用密钥管理器获取AES密钥: {}", keyOrKeyId);
                return EncryptUtil.encryptAES(plainText, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取AES密钥失败，降级到默认处理: {}", e.getMessage());
                return handleKeyManagerFailure(plainText, keyOrKeyId, "AES");
            }
        } else {
            // 直接使用传统方式
            return EncryptUtil.encryptAES(plainText, keyOrKeyId);
        }
    }

    /**
     * AES-256-GCM解密（支持密钥管理器）
     *
     * @param cipherText Base64编码的密文
     * @param keyOrKeyId 密钥或密钥ID（当启用密钥管理器时）
     * @return 明文
     */
    public static String decryptAES(String cipherText, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            // 使用密钥管理器获取密钥
            try {
                String actualKey = keyManager.getAESKey(keyOrKeyId);
                log.debug("使用密钥管理器获取AES密钥: {}", keyOrKeyId);
                return EncryptUtil.decryptAES(cipherText, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取AES密钥失败，降级到默认处理: {}", e.getMessage());
                return handleKeyManagerFailureDecrypt(cipherText, keyOrKeyId, "AES");
            }
        } else {
            // 直接使用传统方式
            return EncryptUtil.decryptAES(cipherText, keyOrKeyId);
        }
    }

    /**
     * RSA公钥加密（支持密钥管理器）
     */
    public static String encryptRSA(String plainText, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            try {
                String actualKey = keyManager.getRSAPublicKey(keyOrKeyId);
                log.debug("使用密钥管理器获取RSA公钥: {}", keyOrKeyId);
                return EncryptUtil.encryptRSA(plainText, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取RSA公钥失败: {}", e.getMessage());
                throw new SystemException(ErrorCode.SYSTEM_ERROR, "RSA加密失败: " + e.getMessage());
            }
        } else {
            return EncryptUtil.encryptRSA(plainText, keyOrKeyId);
        }
    }

    /**
     * RSA私钥解密（支持密钥管理器）
     */
    public static String decryptRSA(String cipherText, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            try {
                String actualKey = keyManager.getRSAPrivateKey(keyOrKeyId);
                log.debug("使用密钥管理器获取RSA私钥: {}", keyOrKeyId);
                return EncryptUtil.decryptRSA(cipherText, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取RSA私钥失败: {}", e.getMessage());
                throw new SystemException(ErrorCode.SYSTEM_ERROR, "RSA解密失败: " + e.getMessage());
            }
        } else {
            return EncryptUtil.decryptRSA(cipherText, keyOrKeyId);
        }
    }

    /**
     * HMAC签名（支持密钥管理器）
     */
    public static String signHMAC(String data, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            try {
                String actualKey = keyManager.getSigningKey(keyOrKeyId);
                log.debug("使用密钥管理器获取HMAC密钥: {}", keyOrKeyId);
                return EncryptUtil.signHMAC(data, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取HMAC密钥失败: {}", e.getMessage());
                throw new SystemException(ErrorCode.SYSTEM_ERROR, "HMAC签名失败: " + e.getMessage());
            }
        } else {
            return EncryptUtil.signHMAC(data, keyOrKeyId);
        }
    }

    /**
     * HMAC验证（支持密钥管理器）
     */
    public static boolean verifyHMAC(String data, String signature, String keyOrKeyId) {
        if (keyManagerEnabled && !isBase64Key(keyOrKeyId)) {
            try {
                String actualKey = keyManager.getSigningKey(keyOrKeyId);
                return EncryptUtil.verifyHMAC(data, signature, actualKey);
            } catch (Exception e) {
                log.warn("密钥管理器获取HMAC密钥失败: {}", e.getMessage());
                return false;
            }
        } else {
            return EncryptUtil.verifyHMAC(data, signature, keyOrKeyId);
        }
    }

    /**
     * 生成安全的随机密钥（支持密钥管理器）
     */
    public static String generateSecureKey(String keyType, int keySize) {
        if (keyManagerEnabled) {
            try {
                String keyId = "generated-" + System.currentTimeMillis();
                String key = keyManager.generateSecureKey(keyType, keySize);
                log.info("通过密钥管理器生成新密钥，类型: {}, 长度: {}", keyType, keySize);
                return key;
            } catch (Exception e) {
                log.warn("密钥管理器生成密钥失败，降级到本地生成: {}", e.getMessage());
                return fallbackKeyGeneration(keyType, keySize);
            }
        } else {
            return fallbackKeyGeneration(keyType, keySize);
        }
    }

    /**
     * 轮换密钥
     */
    public static void rotateKey(String keyId) {
        if (!keyManagerEnabled) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥管理器未启用，无法轮换密钥");
        }

        try {
            keyManager.rotateKey(keyId);
            log.info("密钥轮换成功: {}", keyId);
        } catch (Exception e) {
            log.error("密钥轮换失败: {}", keyId, e);
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥轮换失败: " + keyId);
        }
    }

    /**
     * 检查密钥是否即将过期
     */
    public static boolean isKeyExpiringSoon(String keyId, int daysThreshold) {
        if (!keyManagerEnabled) {
            return false; // 没有启用密钥管理器时无法检查过期状态
        }

        try {
            return keyManager.isKeyExpiringSoon(keyId, daysThreshold);
        } catch (Exception e) {
            log.warn("检查密钥过期状态失败: {}", keyId, e);
            return true; // 出错时保守处理
        }
    }

    /**
     * 验证密钥强度
     */
    public static int validateKeyStrength(String key) {
        return EncryptUtil.validateKeyStrength(key);
    }

    // ========== 兼容性方法 ==========

    /**
     * 保持与原EncryptUtil的兼容性
     */
    public static String hashPassword(String password) {
        return EncryptUtil.hashPassword(password);
    }

    public static boolean verifyPassword(String password, String hash) {
        return EncryptUtil.verifyPassword(password, hash);
    }

    public static String md5(String data) {
        return EncryptUtil.md5(data);
    }

    public static String sha256(String data) {
        return EncryptUtil.sha256(data);
    }

    // ========== 私有辅助方法 ==========

    /**
     * 检查是否为Base64编码的密钥
     */
    private static boolean isBase64Key(String key) {
        try {
            // 简单检查：长度是4的倍数且只包含Base64字符
            return key != null &&
                   key.length() % 4 == 0 &&
                   key.matches("^[A-Za-z0-9+/]*={0,2}$");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 处理密钥管理器失败的情况（加密）
     */
    private static String handleKeyManagerFailure(String plainText, String keyId, String algorithm) {
        // 尝试使用默认密钥
        String defaultKey = generateDefaultKey(algorithm);
        log.warn("使用默认密钥进行{}加密", algorithm);
        return EncryptUtil.encryptAES(plainText, defaultKey);
    }

    /**
     * 处理密钥管理器失败的情况（解密）
     */
    private static String handleKeyManagerFailureDecrypt(String cipherText, String keyId, String algorithm) {
        // 尝试使用默认密钥
        String defaultKey = generateDefaultKey(algorithm);
        log.warn("使用默认密钥进行{}解密", algorithm);
        try {
            return EncryptUtil.decryptAES(cipherText, defaultKey);
        } catch (Exception e) {
            log.error("默认密钥解密也失败: {}", e.getMessage());
            throw new SystemException(ErrorCode.SYSTEM_ERROR, algorithm + "解密失败");
        }
    }

    /**
     * 生成默认密钥（降级使用）
     */
    private static String generateDefaultKey(String algorithm) {
        switch (algorithm) {
            case "AES":
                return EncryptUtil.generateAESKey(256);
            case "RSA":
                String[] keyPair = EncryptUtil.generateRSAKeyPair(2048);
                return keyPair[0]; // 返回公钥
            case "HMAC":
                return EncryptUtil.generateAESKey(256); // HMAC使用AES密钥
            default:
                return EncryptUtil.generateAESKey(256);
        }
    }

    /**
     * 降级密钥生成
     */
    private static String fallbackKeyGeneration(String keyType, int keySize) {
        switch (keyType.toUpperCase()) {
            case "AES":
                return EncryptUtil.generateAESKey(keySize);
            case "RSA":
                String[] keyPair = EncryptUtil.generateRSAKeyPair(keySize);
                return keyPair[0];
            default:
                return EncryptUtil.generateAESKey(256);
        }
    }
}