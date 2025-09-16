package com.haven.base.utils;

import com.haven.base.common.exception.SystemException;
import com.haven.base.common.response.ErrorCode;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
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
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    private static final BCryptPasswordEncoder PASSWORD_ENCODER = new BCryptPasswordEncoder();

    private EncryptUtil() {
        throw new AssertionError("不允许实例化");
    }

    /**
     * AES-256-GCM加密
     *
     * @param plainText 明文
     * @param key       密钥（256位）
     * @return Base64编码的密文（包含IV）
     */
    public static String encryptAES(String plainText, String key) {
        try {
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            // 生成随机IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 加密
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, parameterSpec);
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 将IV和密文合并
            byte[] cipherTextWithIv = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, cipherTextWithIv, 0, iv.length);
            System.arraycopy(cipherText, 0, cipherTextWithIv, iv.length, cipherText.length);

            return Base64.getEncoder().encodeToString(cipherTextWithIv);
        } catch (Exception e) {
            throw new SystemException(ErrorCode.SYSTEM_ERROR, "AES加密失败");
        }
    }

    /**
     * AES-256-GCM解密
     *
     * @param cipherText Base64编码的密文（包含IV）
     * @param key        密钥（256位）
     * @return 明文
     */
    public static String decryptAES(String cipherText, String key) {
        try {
            byte[] cipherTextWithIv = Base64.getDecoder().decode(cipherText);
            byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
            SecretKeySpec secretKey = new SecretKeySpec(keyBytes, "AES");

            // 提取IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            System.arraycopy(cipherTextWithIv, 0, iv, 0, iv.length);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);

            // 提取密文
            byte[] cipherTextBytes = new byte[cipherTextWithIv.length - iv.length];
            System.arraycopy(cipherTextWithIv, iv.length, cipherTextBytes, 0, cipherTextBytes.length);

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
}