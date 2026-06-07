package com.enterprise.license.util;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 哈希工具类
 * 提供SHA-256、SHA-512、PBKDF2等安全哈希功能
 */
@Component
public class HashUtil {

    private static final String SHA256_ALGORITHM = "SHA-256";
    private static final String SHA512_ALGORITHM = "SHA-512";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int SALT_LENGTH = 32;
    private static final int PBKDF2_ITERATIONS = 100000;
    private static final int PBKDF2_KEY_LENGTH = 256;

    /**
     * SHA-256哈希
     */
    public String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256算法不可用", e);
        }
    }

    /**
     * SHA-256哈希（带盐）
     */
    public String sha256WithSalt(String input, String salt) {
        return sha256(input + salt);
    }

    /**
     * SHA-512哈希
     */
    public String sha512(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA512_ALGORITHM);
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-512算法不可用", e);
        }
    }

    /**
     * SHA-512哈希（带盐）
     */
    public String sha512WithSalt(String input, String salt) {
        return sha512(input + salt);
    }

    /**
     * PBKDF2密码哈希
     */
    public String pbkdf2Hash(String password, String salt) {
        try {
            javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec(
                password.toCharArray(),
                salt.getBytes(StandardCharsets.UTF_8),
                PBKDF2_ITERATIONS,
                PBKDF2_KEY_LENGTH
            );
            
            javax.crypto.SecretKeyFactory factory = javax.crypto.SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] hash = factory.generateSecret(spec).getEncoded();
            
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("PBKDF2哈希失败", e);
        }
    }

    /**
     * 验证PBKDF2密码
     */
    public boolean verifyPbkdf2(String password, String salt, String hash) {
        String computedHash = pbkdf2Hash(password, salt);
        return slowEquals(computedHash, hash);
    }

    /**
     * 生成随机盐
     */
    public String generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return Base64.getEncoder().encodeToString(salt);
    }

    /**
     * 生成文件内容哈希
     */
    public String fileHash(byte[] fileContent) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA256_ALGORITHM);
            byte[] hash = digest.digest(fileContent);
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("文件哈希计算失败", e);
        }
    }

    /**
     * 计算HMAC-SHA256
     */
    public String hmacSha256(String data, String key) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
            javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"
            );
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new RuntimeException("HMAC-SHA256计算失败", e);
        }
    }

    /**
     * 计算多轮哈希（增强安全性）
     */
    public String multiRoundHash(String input, int rounds) {
        String result = input;
        for (int i = 0; i < rounds; i++) {
            result = sha256(result);
        }
        return result;
    }

    /**
     * 生成硬件指纹哈希
     */
    public String hardwareFingerprintHash(String... components) {
        StringBuilder sb = new StringBuilder();
        for (String component : components) {
            if (component != null) {
                sb.append(component);
            }
        }
        return sha256(sb.toString());
    }

    /**
     * 字节数组转十六进制字符串
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    /**
     * 安全的字符串比较（防止时间攻击）
     */
    private boolean slowEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }
        
        if (a.length() != b.length()) {
            return false;
        }
        
        int diff = 0;
        for (int i = 0; i < a.length(); i++) {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        
        return diff == 0;
    }

    /**
     * 生成校验和
     */
    public String generateChecksum(String data) {
        return sha256(data).substring(0, 8);
    }

    /**
     * 验证数据完整性
     */
    public boolean verifyIntegrity(String data, String expectedHash) {
        String actualHash = sha256(data);
        return slowEquals(actualHash, expectedHash);
    }
}