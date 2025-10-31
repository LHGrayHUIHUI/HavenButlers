package com.haven.base.security;

/**
 * 密钥管理器接口
 * 提供统一的密钥管理功能，支持密钥获取、轮换、验证等
 *
 * @author HavenButler
 */
public interface KeyManager {

    /**
     * 获取加密密钥
     *
     * @param keyId 密钥标识
     * @return Base64编码的密钥
     */
    String getEncryptionKey(String keyId);

    /**
     * 获取签名密钥
     *
     * @param keyId 密钥标识
     * @return Base64编码的密钥
     */
    String getSigningKey(String keyId);

    /**
     * 获取AES密钥
     *
     * @param keyId 密钥标识
     * @return Base64编码的AES密钥
     */
    String getAESKey(String keyId);

    /**
     * 获取RSA公钥
     *
     * @param keyId 密钥标识
     * @return Base64编码的公钥
     */
    String getRSAPublicKey(String keyId);

    /**
     * 获取RSA私钥
     *
     * @param keyId 密钥标识
     * @return Base64编码的私钥
     */
    String getRSAPrivateKey(String keyId);

    /**
     * 轮换密钥
     *
     * @param keyId 密钥标识
     */
    void rotateKey(String keyId);

    /**
     * 验证密钥强度
     *
     * @param key 密钥
     * @return 密钥强度评分 (0-100)
     */
    int validateKeyStrength(String key);

    /**
     * 检查密钥是否即将过期
     *
     * @param keyId 密钥标识
     * @param daysThreshold 天数阈值
     * @return 是否即将过期
     */
    boolean isKeyExpiringSoon(String keyId, int daysThreshold);

    /**
     * 生成新的安全密钥
     *
     * @param keyType 密钥类型 (AES, RSA, HMAC等)
     * @param keySize 密钥长度
     * @return Base64编码的新密钥
     */
    String generateSecureKey(String keyType, int keySize);
}