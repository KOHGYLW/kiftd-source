package com.enterprise.license.service;

import com.enterprise.license.util.CryptoUtil;
import com.enterprise.license.util.SecureRandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

/**
 * RSA密钥生成服务
 * 负责生成、管理和轮换RSA-4096密钥对
 */
@Service
public class RsaKeyGeneratorService {

    private static final Logger logger = LoggerFactory.getLogger(RsaKeyGeneratorService.class);

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private SecureRandomUtil secureRandomUtil;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final ConcurrentHashMap<String, KeyPairInfo> keyPairCache = new ConcurrentHashMap<>();

    /**
     * 密钥对信息类
     */
    public static class KeyPairInfo {
        private final KeyPair keyPair;
        private final LocalDateTime createdAt;
        private final String keyId;
        private final String algorithm;
        private final int keySize;

        public KeyPairInfo(KeyPair keyPair, String keyId) {
            this.keyPair = keyPair;
            this.keyId = keyId;
            this.createdAt = LocalDateTime.now();
            this.algorithm = "RSA";
            this.keySize = 4096;
        }

        // Getters
        public KeyPair getKeyPair() { return keyPair; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getKeyId() { return keyId; }
        public String getAlgorithm() { return algorithm; }
        public int getKeySize() { return keySize; }
        public PublicKey getPublicKey() { return keyPair.getPublic(); }
        public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }
    }

    /**
     * 生成新的RSA-4096密钥对
     */
    public KeyPairInfo generateKeyPair() {
        try {
            logger.info("开始生成RSA-4096密钥对");
            long startTime = System.currentTimeMillis();

            KeyPair keyPair = cryptoUtil.generateRSAKeyPair();
            String keyId = generateKeyId();
            
            KeyPairInfo keyPairInfo = new KeyPairInfo(keyPair, keyId);
            keyPairCache.put(keyId, keyPairInfo);

            long endTime = System.currentTimeMillis();
            logger.info("RSA-4096密钥对生成完成，密钥ID: {}, 耗时: {}ms", keyId, (endTime - startTime));

            return keyPairInfo;
        } catch (Exception e) {
            logger.error("生成RSA密钥对失败", e);
            throw new RuntimeException("密钥对生成失败", e);
        }
    }

    /**
     * 异步生成密钥对
     */
    public CompletableFuture<KeyPairInfo> generateKeyPairAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return generateKeyPair();
            } catch (Exception e) {
                logger.error("异步生成密钥对失败", e);
                throw new RuntimeException("异步密钥对生成失败", e);
            }
        }, scheduler);
    }

    /**
     * 批量生成密钥对
     */
    public CompletableFuture<KeyPairInfo[]> generateKeyPairBatch(int count) {
        if (count <= 0 || count > 10) {
            throw new IllegalArgumentException("批量生成数量必须在1-10之间");
        }

        logger.info("开始批量生成{}个RSA密钥对", count);

        CompletableFuture<KeyPairInfo>[] futures = new CompletableFuture[count];
        for (int i = 0; i < count; i++) {
            futures[i] = generateKeyPairAsync();
        }

        return CompletableFuture.allOf(futures)
                .thenApply(v -> {
                    KeyPairInfo[] results = new KeyPairInfo[count];
                    for (int i = 0; i < count; i++) {
                        results[i] = futures[i].join();
                    }
                    logger.info("批量生成{}个RSA密钥对完成", count);
                    return results;
                });
    }

    /**
     * 根据密钥ID获取密钥对信息
     */
    public KeyPairInfo getKeyPairInfo(String keyId) {
        KeyPairInfo keyPairInfo = keyPairCache.get(keyId);
        if (keyPairInfo == null) {
            logger.warn("未找到密钥ID: {}", keyId);
            throw new IllegalArgumentException("密钥不存在: " + keyId);
        }
        return keyPairInfo;
    }

    /**
     * 获取公钥（Base64格式）
     */
    public String getPublicKeyString(String keyId) {
        KeyPairInfo keyPairInfo = getKeyPairInfo(keyId);
        return cryptoUtil.publicKeyToString(keyPairInfo.getPublicKey());
    }

    /**
     * 获取私钥（Base64格式）
     */
    public String getPrivateKeyString(String keyId) {
        KeyPairInfo keyPairInfo = getKeyPairInfo(keyId);
        return cryptoUtil.privateKeyToString(keyPairInfo.getPrivateKey());
    }

    /**
     * 验证密钥对的有效性
     */
    public boolean validateKeyPair(String keyId) {
        try {
            KeyPairInfo keyPairInfo = getKeyPairInfo(keyId);
            
            // 测试加密解密
            String testData = "密钥验证测试数据" + System.currentTimeMillis();
            String encrypted = cryptoUtil.encryptRSA(testData, keyPairInfo.getPublicKey());
            String decrypted = cryptoUtil.decryptRSA(encrypted, keyPairInfo.getPrivateKey());
            
            boolean isValid = testData.equals(decrypted);
            
            if (isValid) {
                logger.debug("密钥对验证成功，密钥ID: {}", keyId);
            } else {
                logger.warn("密钥对验证失败，密钥ID: {}", keyId);
            }
            
            return isValid;
        } catch (Exception e) {
            logger.error("密钥对验证过程中发生错误，密钥ID: {}", keyId, e);
            return false;
        }
    }

    /**
     * 测试签名和验证功能
     */
    public boolean testSignatureCapability(String keyId) {
        try {
            KeyPairInfo keyPairInfo = getKeyPairInfo(keyId);
            
            String testData = "签名测试数据" + System.currentTimeMillis();
            String signature = cryptoUtil.signRSA(testData, keyPairInfo.getPrivateKey());
            boolean verified = cryptoUtil.verifyRSA(testData, signature, keyPairInfo.getPublicKey());
            
            if (verified) {
                logger.debug("密钥对签名功能验证成功，密钥ID: {}", keyId);
            } else {
                logger.warn("密钥对签名功能验证失败，密钥ID: {}", keyId);
            }
            
            return verified;
        } catch (Exception e) {
            logger.error("密钥对签名功能验证过程中发生错误，密钥ID: {}", keyId, e);
            return false;
        }
    }

    /**
     * 移除密钥对
     */
    public boolean removeKeyPair(String keyId) {
        if (keyPairCache.containsKey(keyId)) {
            keyPairCache.remove(keyId);
            logger.info("密钥对已移除，密钥ID: {}", keyId);
            return true;
        }
        logger.warn("尝试移除不存在的密钥对，密钥ID: {}", keyId);
        return false;
    }

    /**
     * 清理过期密钥对
     */
    public int cleanupExpiredKeys(int maxAgeHours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(maxAgeHours);
        int removedCount = 0;

        keyPairCache.entrySet().removeIf(entry -> {
            if (entry.getValue().getCreatedAt().isBefore(cutoffTime)) {
                logger.info("清理过期密钥对，密钥ID: {}, 创建时间: {}", 
                    entry.getKey(), entry.getValue().getCreatedAt());
                return true;
            }
            return false;
        });

        if (removedCount > 0) {
            logger.info("清理了{}个过期密钥对", removedCount);
        }

        return removedCount;
    }

    /**
     * 获取密钥统计信息
     */
    public KeyStatistics getKeyStatistics() {
        int totalKeys = keyPairCache.size();
        LocalDateTime oldestKeyTime = keyPairCache.values().stream()
                .map(KeyPairInfo::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(null);
        
        LocalDateTime newestKeyTime = keyPairCache.values().stream()
                .map(KeyPairInfo::getCreatedAt)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new KeyStatistics(totalKeys, oldestKeyTime, newestKeyTime);
    }

    /**
     * 密钥统计信息类
     */
    public static class KeyStatistics {
        private final int totalKeys;
        private final LocalDateTime oldestKeyTime;
        private final LocalDateTime newestKeyTime;

        public KeyStatistics(int totalKeys, LocalDateTime oldestKeyTime, LocalDateTime newestKeyTime) {
            this.totalKeys = totalKeys;
            this.oldestKeyTime = oldestKeyTime;
            this.newestKeyTime = newestKeyTime;
        }

        public int getTotalKeys() { return totalKeys; }
        public LocalDateTime getOldestKeyTime() { return oldestKeyTime; }
        public LocalDateTime getNewestKeyTime() { return newestKeyTime; }
    }

    /**
     * 生成唯一的密钥ID
     */
    private String generateKeyId() {
        return "RSA4096_" + secureRandomUtil.generateUUIDWithoutHyphens().substring(0, 16).toUpperCase();
    }

    /**
     * 关闭服务时清理资源
     */
    public void shutdown() {
        scheduler.shutdown();
        keyPairCache.clear();
        logger.info("RSA密钥生成服务已关闭");
    }
}