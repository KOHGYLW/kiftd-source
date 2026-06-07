package com.enterprise.license.util;

import org.springframework.stereotype.Component;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Random;
import java.util.UUID;

/**
 * 安全随机数生成工具类
 * 提供加密级安全的随机数生成功能
 */
@Component
public class SecureRandomUtil {

    private static final String SECURE_RANDOM_ALGORITHM = "SHA1PRNG";
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String NUMERIC_CHARS = "0123456789";
    private static final String ALPHABET_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final String SPECIAL_CHARS = "!@#$%^&*()_+-=[]{}|;:,.<>?";

    private final SecureRandom secureRandom;

    public SecureRandomUtil() {
        try {
            this.secureRandom = SecureRandom.getInstance(SECURE_RANDOM_ALGORITHM);
            // 种子初始化
            this.secureRandom.setSeed(System.currentTimeMillis());
            this.secureRandom.setSeed(System.nanoTime());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("无法初始化SecureRandom", e);
        }
    }

    /**
     * 生成随机字节数组
     */
    public byte[] generateRandomBytes(int length) {
        byte[] bytes = new byte[length];
        secureRandom.nextBytes(bytes);
        return bytes;
    }

    /**
     * 生成Base64编码的随机字符串
     */
    public String generateRandomBase64String(int byteLength) {
        byte[] randomBytes = generateRandomBytes(byteLength);
        return Base64.getEncoder().encodeToString(randomBytes);
    }

    /**
     * 生成字母数字随机字符串
     */
    public String generateAlphanumericString(int length) {
        return generateRandomString(ALPHANUMERIC_CHARS, length);
    }

    /**
     * 生成纯数字随机字符串
     */
    public String generateNumericString(int length) {
        return generateRandomString(NUMERIC_CHARS, length);
    }

    /**
     * 生成纯字母随机字符串
     */
    public String generateAlphabetString(int length) {
        return generateRandomString(ALPHABET_CHARS, length);
    }

    /**
     * 生成包含特殊字符的强密码
     */
    public String generateStrongPassword(int length) {
        String allChars = ALPHANUMERIC_CHARS + SPECIAL_CHARS;
        StringBuilder password = new StringBuilder();
        
        // 确保至少包含一个大写字母、小写字母、数字和特殊字符
        password.append(generateRandomString("ABCDEFGHIJKLMNOPQRSTUVWXYZ", 1));
        password.append(generateRandomString("abcdefghijklmnopqrstuvwxyz", 1));
        password.append(generateRandomString(NUMERIC_CHARS, 1));
        password.append(generateRandomString(SPECIAL_CHARS, 1));
        
        // 填充剩余长度
        for (int i = 4; i < length; i++) {
            password.append(generateRandomString(allChars, 1));
        }
        
        // 打乱字符顺序
        return shuffleString(password.toString());
    }

    /**
     * 生成UUID
     */
    public String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不带横线的UUID
     */
    public String generateUUIDWithoutHyphens() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成随机整数（包含边界）
     */
    public int generateRandomInt(int min, int max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        return secureRandom.nextInt(max - min + 1) + min;
    }

    /**
     * 生成随机长整数（包含边界）
     */
    public long generateRandomLong(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        return min + (long) (secureRandom.nextDouble() * (max - min + 1));
    }

    /**
     * 生成随机浮点数（0.0到1.0之间）
     */
    public double generateRandomDouble() {
        return secureRandom.nextDouble();
    }

    /**
     * 生成随机浮点数（指定范围）
     */
    public double generateRandomDouble(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("最小值不能大于最大值");
        }
        return min + secureRandom.nextDouble() * (max - min);
    }

    /**
     * 生成随机布尔值
     */
    public boolean generateRandomBoolean() {
        return secureRandom.nextBoolean();
    }

    /**
     * 生成授权码（格式：XXXX-XXXX-XXXX-XXXX）
     */
    public String generateLicenseKey() {
        StringBuilder licenseKey = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i > 0) {
                licenseKey.append("-");
            }
            licenseKey.append(generateAlphanumericString(4).toUpperCase());
        }
        return licenseKey.toString();
    }

    /**
     * 生成会话令牌
     */
    public String generateSessionToken() {
        return generateRandomBase64String(32);
    }

    /**
     * 生成API密钥
     */
    public String generateApiKey() {
        return generateRandomBase64String(48);
    }

    /**
     * 生成盐值
     */
    public String generateSalt() {
        return generateRandomBase64String(32);
    }

    /**
     * 生成初始化向量（IV）
     */
    public byte[] generateIV(int length) {
        return generateRandomBytes(length);
    }

    /**
     * 生成随机端口号（1024-65535范围内）
     */
    public int generateRandomPort() {
        return generateRandomInt(1024, 65535);
    }

    /**
     * 生成验证码
     */
    public String generateVerificationCode(int length) {
        return generateNumericString(length);
    }

    /**
     * 从字符集生成随机字符串
     */
    private String generateRandomString(String charset, int length) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(charset.length());
            result.append(charset.charAt(index));
        }
        return result.toString();
    }

    /**
     * 打乱字符串字符顺序
     */
    private String shuffleString(String input) {
        char[] chars = input.toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = secureRandom.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }
        return new String(chars);
    }

    /**
     * 验证随机性（简单的统计测试）
     */
    public boolean testRandomness(int sampleSize) {
        int[] counts = new int[256];
        byte[] sample = generateRandomBytes(sampleSize);
        
        for (byte b : sample) {
            counts[b & 0xFF]++;
        }
        
        // 简单的卡方检验
        double expected = sampleSize / 256.0;
        double chiSquare = 0;
        
        for (int count : counts) {
            double diff = count - expected;
            chiSquare += (diff * diff) / expected;
        }
        
        // 自由度为255的卡方分布，显著性水平0.05的临界值约为293.25
        return chiSquare < 293.25;
    }

    /**
     * 重新初始化随机数生成器
     */
    public void reseed() {
        secureRandom.setSeed(System.currentTimeMillis());
        secureRandom.setSeed(System.nanoTime());
        secureRandom.setSeed(generateRandomBytes(32));
    }
}