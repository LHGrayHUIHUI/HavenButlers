package com.haven.base.utils;

import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 加密工具类
 * 提供AES、RSA、HMAC等加密算法的实现
 *
 * @author HavenButler
 */
public final class EncryptUtil {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int SALT_LENGTH = 16;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private EncryptUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * AES-256-GCM加密
     * 支持Base64密钥或字符串密钥（自动使用PBKDF2派生）
     *
     * @param plainText 明文
     * @param key       密钥（Base64编码的密钥或普通字符串）
     * @return Base64编码的密文（包含IV和salt）
     */
    public static String encryptAES(String plainText, String key) {
        try {
            SecretKeySpec secretKey = prepareAESKey(key);
            byte[] salt = null;

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // 如果key不是标准AES密钥，需要包含salt
            if (!isValidBase64AESKey(key)) {
                salt = new byte[SALT_LENGTH];
                new SecureRandom().nextBytes(salt);
                secretKey = deriveAESKeyFromPassword(key, salt);
            }

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 加密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 构建最终数据：[salt(可选)][iv][cipherText]
            byte[] result;
            if (salt != null) {
                result = new byte[salt.length + iv.length + cipherText.length];
                System.arraycopy(salt, 0, result, 0, salt.length);
                System.arraycopy(iv, 0, result, salt.length, iv.length);
                System.arraycopy(cipherText, 0, result, salt.length + iv.length, cipherText.length);
            } else {
                result = new byte[iv.length + cipherText.length];
                System.arraycopy(iv, 0, result, 0, iv.length);
                System.arraycopy(cipherText, 0, result, iv.length, cipherText.length);
            }

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "AES加密失败");
        }
    }

    /**
     * AES-256-GCM解密
     *
     * @param cipherText Base64编码的密文（包含salt、IV和密文）
     * @param key        密钥（Base64编码的密钥或普通字符串）
     * @return 明文
     */
    public static String decryptAES(String cipherText, String key) {
        try {
            byte[] encryptedData = Base64.getDecoder().decode(cipherText);

            SecretKeySpec secretKey;
            int offset = 0;

            // 检查是否包含salt（基于密钥类型判断）
            if (!isValidBase64AESKey(key)) {
                // 提取salt
                byte[] salt = new byte[SALT_LENGTH];
                System.arraycopy(encryptedData, 0, salt, 0, SALT_LENGTH);
                offset += SALT_LENGTH;

                // 使用PBKDF2派生密钥
                secretKey = deriveAESKeyFromPassword(key, salt);
            } else {
                // 使用原始密钥
                secretKey = prepareAESKey(key);
            }

            // 提取IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(encryptedData, offset, iv, 0, GCM_IV_LENGTH);
            offset += GCM_IV_LENGTH;

            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 提取密文
            byte[] cipherTextBytes = new byte[encryptedData.length - offset];
            System.arraycopy(encryptedData, offset, cipherTextBytes, 0, cipherTextBytes.length);

            // 解密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, parameterSpec);
            byte[] plainTextBytes = cipher.doFinal(cipherTextBytes);

            return new String(plainTextBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "AES解密失败");
        }
    }

    /**
     * RSA公钥加密
     *
     * @param plainText 明文
     * @param publicKey Base64编码的公钥
     * @return Base64编码的密文
     */
    public static String encryptRSA(String plainText, String publicKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKey);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PublicKey pubKey = keyFactory.generatePublic(spec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, pubKey);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(cipherText);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "RSA加密失败");
        }
    }

    /**
     * RSA私钥解密
     *
     * @param cipherText Base64编码的密文
     * @param privateKey Base64编码的私钥
     * @return 明文
     */
    public static String decryptRSA(String cipherText, String privateKey) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKey);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PrivateKey privKey = keyFactory.generatePrivate(spec);

            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] plainText = cipher.doFinal(Base64.getDecoder().decode(cipherText));

            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "RSA解密失败");
        }
    }

    /**
     * HMAC-SHA256签名
     *
     * @param data   待签名数据
     * @param secret 密钥
     * @return Base64编码的签名
     */
    public static String signHMAC(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec secretKeySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
            mac.init(secretKeySpec);
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "HMAC签名失败");
        }
    }

    /**
     * 验证HMAC-SHA256签名
     *
     * @param data      待验证数据
     * @param signature Base64编码的签名
     * @param secret    密钥
     * @return 是否验证通过
     */
    public static boolean verifyHMAC(String data, String signature, String secret) {
        try {
            String calculatedSignature = signHMAC(data, secret);
            return calculatedSignature.equals(signature);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * BCrypt密码加密
     *
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String hashPassword(String password) {
        return PASSWORD_ENCODER.encode(password);
    }

    /**
     * BCrypt密码验证
     *
     * @param password 原始密码
     * @param hash     加密后的密码
     * @return 是否匹配
     */
    public static boolean verifyPassword(String password, String hash) {
        return PASSWORD_ENCODER.matches(password, hash);
    }

    /**
     * MD5加密（不推荐用于敏感信息）
     *
     * @param data 待加密数据
     * @return MD5值（32位小写）
     */
    public static String md5(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "MD5加密失败");
        }
    }

    /**
     * SHA256加密
     *
     * @param data 待加密数据
     * @return SHA256值（64位小写）
     */
    public static String sha256(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "SHA256加密失败");
        }
    }

    /**
     * 生成AES密钥
     *
     * @param keySize 密钥长度（128/192/256）
     * @return Base64编码的密钥
     */
    public static String generateAESKey(int keySize) {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(keySize);
            SecretKey secretKey = keyGenerator.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "生成AES密钥失败");
        }
    }

    /**
     * 生成RSA密钥对
     *
     * @param keySize 密钥长度（1024/2048/4096）
     * @return [公钥, 私钥]的Base64编码
     */
    public static String[] generateRSAKeyPair(int keySize) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(keySize);
            KeyPair keyPair = keyPairGenerator.generateKeyPair();

            String publicKey = Base64.getEncoder().encodeToString(
                keyPair.getPublic().getEncoded());
            String privateKey = Base64.getEncoder().encodeToString(
                keyPair.getPrivate().getEncoded());

            return new String[]{publicKey, privateKey};
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "生成RSA密钥对失败");
        }
    }

    // ========== 密钥处理辅助方法 ==========

    /**
     * 验证是否为有效的Base64编码AES密钥
     */
    private static boolean isValidBase64AESKey(String key) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            return keyBytes.length == 16 || keyBytes.length == 24 || keyBytes.length == 32; // AES-128/192/256
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 准备AES密钥
     * 如果是Base64密钥直接解码，否则抛出异常要求使用PBKDF2
     */
    private static SecretKeySpec prepareAESKey(String key) {
        if (isValidBase64AESKey(key)) {
            byte[] keyBytes = Base64.getDecoder().decode(key);
            return new SecretKeySpec(keyBytes, "AES");
        } else {
            // 对于非标准密钥，这个方法不应该被调用
            throw new SystemException(ErrorCode.PARAM_ERROR,
                "密钥格式不正确，请使用Base64编码的AES密钥或调用带salt的加密方法");
        }
    }

    /**
     * 使用PBKDF2从密码派生AES密钥
     */
    private static SecretKeySpec deriveAESKeyFromPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            SecretKey secretKey = factory.generateSecret(spec);
            return new SecretKeySpec(secretKey.getEncoded(), "AES");
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥派生失败");
        }
    }

    /**
     * 生成安全的随机salt
     */
    public static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    /**
     * 使用PBKDF2派生密钥（用于密码存储）
     */
    public static String deriveKeyFromPassword(String password, byte[] salt) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATIONS, 256);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            SecretKey secretKey = factory.generateSecret(spec);
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "密钥派生失败");
        }
    }

    /**
     * 验证密钥强度
     *
     * @param key 密钥字符串
     * @return 密钥强度评分 (0-100)
     */
    public static int validateKeyStrength(String key) {
        if (key == null || key.length() < 8) {
            return 0;
        }

        int score = 0;

        // 长度评分
        if (key.length() >= 8) score += 25;
        if (key.length() >= 12) score += 25;
        if (key.length() >= 16) score += 25;

        // 字符类型评分
        boolean hasLower = key.matches(".*[a-z].*");
        boolean hasUpper = key.matches(".*[A-Z].*");
        boolean hasDigit = key.matches(".*\\d.*");
        boolean hasSpecial = key.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>\\/?].*");

        if (hasLower) score += 5;
        if (hasUpper) score += 5;
        if (hasDigit) score += 5;
        if (hasSpecial) score += 10;

        return Math.min(score, 100);
    }

    /**
     * 常量时间字符串比较（防止时序攻击）
     */
    public static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        if (a.length() != b.length()) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }

        return result == 0;
    }
}