package com.enterprise.license.service;

import com.enterprise.license.util.CryptoUtil;
import com.enterprise.license.util.HashUtil;
import com.enterprise.license.util.SecureRandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 密钥管理服务
 * 负责密钥的安全存储、轮换和生命周期管理
 */
@Service
public class KeyManagerService {

    private static final Logger logger = LoggerFactory.getLogger(KeyManagerService.class);

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private HashUtil hashUtil;

    @Autowired
    private SecureRandomUtil secureRandomUtil;

    @Autowired
    private RsaKeyGeneratorService rsaKeyGeneratorService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Value("${license.security.key-rotation-interval:24}")
    private int keyRotationIntervalHours;

    @Value("${license.security.max-key-age:168}")
    private int maxKeyAgeHours;

    private static final String REDIS_KEY_PREFIX = "license:keys:";
    private static final String REDIS_MASTER_KEY_PREFIX = "license:master:";
    private static final String REDIS_KEY_METADATA_PREFIX = "license:key-metadata:";

    /**
     * 密钥信息类
     */
    public static class KeyInfo {
        private String keyId;
        private String keyType;
        private String algorithm;
        private int keySize;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private boolean isActive;
        private String fingerprint;
        private Map<String, Object> metadata;

        public KeyInfo() {
            this.metadata = new HashMap<>();
        }

        // Getters and Setters
        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }

        public String getKeyType() { return keyType; }
        public void setKeyType(String keyType) { this.keyType = keyType; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public int getKeySize() { return keySize; }
        public void setKeySize(int keySize) { this.keySize = keySize; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

        public boolean isActive() { return isActive; }
        public void setActive(boolean active) { isActive = active; }

        public String getFingerprint() { return fingerprint; }
        public void setFingerprint(String fingerprint) { this.fingerprint = fingerprint; }

        public Map<String, Object> getMetadata() { return metadata; }
        public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }
    }

    /**
     * 存储RSA密钥对
     */
    public String storeRSAKeyPair(RsaKeyGeneratorService.KeyPairInfo keyPairInfo, int expirationHours) {
        try {
            String keyId = keyPairInfo.getKeyId();
            
            // 加密存储私钥
            String masterKey = getMasterKey();
            SecretKey aesKey = cryptoUtil.getAESKeyFromString(masterKey);
            
            String publicKeyStr = cryptoUtil.publicKeyToString(keyPairInfo.getPublicKey());
            String privateKeyStr = cryptoUtil.privateKeyToString(keyPairInfo.getPrivateKey());
            String encryptedPrivateKey = cryptoUtil.encryptAES(privateKeyStr, aesKey);

            // 存储到Redis
            Map<String, Object> keyData = new HashMap<>();
            keyData.put("publicKey", publicKeyStr);
            keyData.put("privateKey", encryptedPrivateKey);
            keyData.put("algorithm", keyPairInfo.getAlgorithm());
            keyData.put("keySize", keyPairInfo.getKeySize());
            keyData.put("createdAt", keyPairInfo.getCreatedAt().toEpochSecond(ZoneOffset.UTC));

            redisTemplate.opsForHash().putAll(REDIS_KEY_PREFIX + keyId, keyData);
            redisTemplate.expire(REDIS_KEY_PREFIX + keyId, expirationHours, TimeUnit.HOURS);

            // 存储密钥元数据
            KeyInfo keyInfo = createKeyInfo(keyId, "RSA_KEYPAIR", keyPairInfo);
            keyInfo.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
            storeKeyMetadata(keyInfo);

            logger.info("RSA密钥对已安全存储，密钥ID: {}, 过期时间: {}小时", keyId, expirationHours);
            return keyId;

        } catch (Exception e) {
            logger.error("存储RSA密钥对失败", e);
            throw new RuntimeException("密钥存储失败", e);
        }
    }

    /**
     * 获取RSA公钥
     */
    public PublicKey getRSAPublicKey(String keyId) {
        try {
            String publicKeyStr = (String) redisTemplate.opsForHash().get(REDIS_KEY_PREFIX + keyId, "publicKey");
            if (publicKeyStr == null) {
                throw new IllegalArgumentException("密钥不存在: " + keyId);
            }
            return cryptoUtil.getPublicKeyFromString(publicKeyStr);
        } catch (Exception e) {
            logger.error("获取RSA公钥失败，密钥ID: {}", keyId, e);
            throw new RuntimeException("获取公钥失败", e);
        }
    }

    /**
     * 获取RSA私钥
     */
    public PrivateKey getRSAPrivateKey(String keyId) {
        try {
            String encryptedPrivateKey = (String) redisTemplate.opsForHash().get(REDIS_KEY_PREFIX + keyId, "privateKey");
            if (encryptedPrivateKey == null) {
                throw new IllegalArgumentException("密钥不存在: " + keyId);
            }

            // 解密私钥
            String masterKey = getMasterKey();
            SecretKey aesKey = cryptoUtil.getAESKeyFromString(masterKey);
            String privateKeyStr = cryptoUtil.decryptAES(encryptedPrivateKey, aesKey);

            return cryptoUtil.getPrivateKeyFromString(privateKeyStr);
        } catch (Exception e) {
            logger.error("获取RSA私钥失败，密钥ID: {}", keyId, e);
            throw new RuntimeException("获取私钥失败", e);
        }
    }

    /**
     * 存储AES密钥
     */
    public String storeAESKey(SecretKey secretKey, int expirationHours) {
        try {
            String keyId = generateKeyId("AES");
            
            // 加密存储AES密钥
            String masterKey = getMasterKey();
            SecretKey masterAesKey = cryptoUtil.getAESKeyFromString(masterKey);
            
            String keyStr = cryptoUtil.aesKeyToString(secretKey);
            String encryptedKey = cryptoUtil.encryptAES(keyStr, masterAesKey);

            // 存储到Redis
            Map<String, Object> keyData = new HashMap<>();
            keyData.put("encryptedKey", encryptedKey);
            keyData.put("algorithm", "AES");
            keyData.put("keySize", 256);
            keyData.put("createdAt", System.currentTimeMillis() / 1000);

            redisTemplate.opsForHash().putAll(REDIS_KEY_PREFIX + keyId, keyData);
            redisTemplate.expire(REDIS_KEY_PREFIX + keyId, expirationHours, TimeUnit.HOURS);

            // 存储密钥元数据
            KeyInfo keyInfo = new KeyInfo();
            keyInfo.setKeyId(keyId);
            keyInfo.setKeyType("AES");
            keyInfo.setAlgorithm("AES");
            keyInfo.setKeySize(256);
            keyInfo.setCreatedAt(LocalDateTime.now());
            keyInfo.setExpiresAt(LocalDateTime.now().plusHours(expirationHours));
            keyInfo.setActive(true);
            keyInfo.setFingerprint(hashUtil.sha256(keyStr).substring(0, 16));
            storeKeyMetadata(keyInfo);

            logger.info("AES密钥已安全存储，密钥ID: {}", keyId);
            return keyId;

        } catch (Exception e) {
            logger.error("存储AES密钥失败", e);
            throw new RuntimeException("AES密钥存储失败", e);
        }
    }

    /**
     * 获取AES密钥
     */
    public SecretKey getAESKey(String keyId) {
        try {
            String encryptedKey = (String) redisTemplate.opsForHash().get(REDIS_KEY_PREFIX + keyId, "encryptedKey");
            if (encryptedKey == null) {
                throw new IllegalArgumentException("密钥不存在: " + keyId);
            }

            // 解密AES密钥
            String masterKey = getMasterKey();
            SecretKey masterAesKey = cryptoUtil.getAESKeyFromString(masterKey);
            String keyStr = cryptoUtil.decryptAES(encryptedKey, masterAesKey);

            return cryptoUtil.getAESKeyFromString(keyStr);
        } catch (Exception e) {
            logger.error("获取AES密钥失败，密钥ID: {}", keyId, e);
            throw new RuntimeException("获取AES密钥失败", e);
        }
    }

    /**
     * 轮换密钥
     */
    public Map<String, String> rotateKeys() {
        Map<String, String> rotationResult = new HashMap<>();
        
        try {
            // 生成新的RSA密钥对
            RsaKeyGeneratorService.KeyPairInfo newKeyPair = rsaKeyGeneratorService.generateKeyPair();
            String newRsaKeyId = storeRSAKeyPair(newKeyPair, maxKeyAgeHours);
            rotationResult.put("newRsaKeyId", newRsaKeyId);

            // 生成新的AES密钥
            SecretKey newAesKey = cryptoUtil.generateAESKey();
            String newAesKeyId = storeAESKey(newAesKey, maxKeyAgeHours);
            rotationResult.put("newAesKeyId", newAesKeyId);

            // 标记旧密钥为非活跃状态
            deactivateOldKeys();

            logger.info("密钥轮换完成，新RSA密钥ID: {}, 新AES密钥ID: {}", newRsaKeyId, newAesKeyId);
            
        } catch (Exception e) {
            logger.error("密钥轮换失败", e);
            throw new RuntimeException("密钥轮换失败", e);
        }

        return rotationResult;
    }

    /**
     * 获取活跃的密钥列表
     */
    public List<KeyInfo> getActiveKeys() {
        try {
            Set<String> keyIds = redisTemplate.keys(REDIS_KEY_METADATA_PREFIX + "*");
            List<KeyInfo> activeKeys = new ArrayList<>();

            if (keyIds != null) {
                for (String keyMetadataKey : keyIds) {
                    KeyInfo keyInfo = (KeyInfo) redisTemplate.opsForValue().get(keyMetadataKey);
                    if (keyInfo != null && keyInfo.isActive() && 
                        keyInfo.getExpiresAt().isAfter(LocalDateTime.now())) {
                        activeKeys.add(keyInfo);
                    }
                }
            }

            return activeKeys;
        } catch (Exception e) {
            logger.error("获取活跃密钥列表失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 删除密钥
     */
    public boolean deleteKey(String keyId) {
        try {
            boolean keyDeleted = redisTemplate.delete(REDIS_KEY_PREFIX + keyId);
            boolean metadataDeleted = redisTemplate.delete(REDIS_KEY_METADATA_PREFIX + keyId);
            
            if (keyDeleted || metadataDeleted) {
                logger.info("密钥已删除，密钥ID: {}", keyId);
                return true;
            } else {
                logger.warn("尝试删除不存在的密钥，密钥ID: {}", keyId);
                return false;
            }
        } catch (Exception e) {
            logger.error("删除密钥失败，密钥ID: {}", keyId, e);
            return false;
        }
    }

    /**
     * 清理过期密钥
     */
    public int cleanupExpiredKeys() {
        int cleanedCount = 0;
        try {
            Set<String> keyIds = redisTemplate.keys(REDIS_KEY_METADATA_PREFIX + "*");
            if (keyIds != null) {
                for (String keyMetadataKey : keyIds) {
                    KeyInfo keyInfo = (KeyInfo) redisTemplate.opsForValue().get(keyMetadataKey);
                    if (keyInfo != null && keyInfo.getExpiresAt().isBefore(LocalDateTime.now())) {
                        String keyId = keyInfo.getKeyId();
                        if (deleteKey(keyId)) {
                            cleanedCount++;
                            logger.debug("清理过期密钥: {}", keyId);
                        }
                    }
                }
            }
            
            if (cleanedCount > 0) {
                logger.info("清理了{}个过期密钥", cleanedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理过期密钥时发生错误", e);
        }
        
        return cleanedCount;
    }

    /**
     * 获取主密钥
     */
    private String getMasterKey() {
        String masterKey = (String) redisTemplate.opsForValue().get(REDIS_MASTER_KEY_PREFIX + "current");
        if (masterKey == null) {
            // 生成新的主密钥
            masterKey = generateMasterKey();
            redisTemplate.opsForValue().set(REDIS_MASTER_KEY_PREFIX + "current", masterKey);
            logger.info("生成了新的主密钥");
        }
        return masterKey;
    }

    /**
     * 生成主密钥
     */
    private String generateMasterKey() {
        try {
            SecretKey masterKey = cryptoUtil.generateAESKey();
            return cryptoUtil.aesKeyToString(masterKey);
        } catch (Exception e) {
            logger.error("生成主密钥失败", e);
            throw new RuntimeException("主密钥生成失败", e);
        }
    }

    /**
     * 存储密钥元数据
     */
    private void storeKeyMetadata(KeyInfo keyInfo) {
        redisTemplate.opsForValue().set(
            REDIS_KEY_METADATA_PREFIX + keyInfo.getKeyId(), 
            keyInfo, 
            keyInfo.getExpiresAt().toEpochSecond(ZoneOffset.UTC) - 
            LocalDateTime.now().toEpochSecond(ZoneOffset.UTC), 
            TimeUnit.SECONDS
        );
    }

    /**
     * 创建密钥信息
     */
    private KeyInfo createKeyInfo(String keyId, String keyType, RsaKeyGeneratorService.KeyPairInfo keyPairInfo) {
        KeyInfo keyInfo = new KeyInfo();
        keyInfo.setKeyId(keyId);
        keyInfo.setKeyType(keyType);
        keyInfo.setAlgorithm(keyPairInfo.getAlgorithm());
        keyInfo.setKeySize(keyPairInfo.getKeySize());
        keyInfo.setCreatedAt(keyPairInfo.getCreatedAt());
        keyInfo.setActive(true);
        
        // 生成指纹
        String publicKeyStr = cryptoUtil.publicKeyToString(keyPairInfo.getPublicKey());
        keyInfo.setFingerprint(hashUtil.sha256(publicKeyStr).substring(0, 16));
        
        return keyInfo;
    }

    /**
     * 停用旧密钥
     */
    private void deactivateOldKeys() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(keyRotationIntervalHours);
        
        Set<String> keyIds = redisTemplate.keys(REDIS_KEY_METADATA_PREFIX + "*");
        if (keyIds != null) {
            for (String keyMetadataKey : keyIds) {
                KeyInfo keyInfo = (KeyInfo) redisTemplate.opsForValue().get(keyMetadataKey);
                if (keyInfo != null && keyInfo.isActive() && 
                    keyInfo.getCreatedAt().isBefore(cutoffTime)) {
                    keyInfo.setActive(false);
                    storeKeyMetadata(keyInfo);
                    logger.debug("停用旧密钥: {}", keyInfo.getKeyId());
                }
            }
        }
    }

    /**
     * 生成密钥ID
     */
    private String generateKeyId(String prefix) {
        return prefix + "_" + secureRandomUtil.generateUUIDWithoutHyphens().substring(0, 12).toUpperCase();
    }
}