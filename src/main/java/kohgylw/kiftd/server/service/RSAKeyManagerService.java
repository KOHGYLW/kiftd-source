package kohgylw.kiftd.server.service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 企业级RSA密钥管理服务接口
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface RSAKeyManagerService {
    
    /**
     * 生成RSA 4096位密钥对
     * @return KeyPair 密钥对
     * @throws NoSuchAlgorithmException 算法异常
     */
    KeyPair generateKeyPair() throws NoSuchAlgorithmException;
    
    /**
     * 获取当前有效的公钥
     * @return String Base64编码的公钥
     */
    String getCurrentPublicKey();
    
    /**
     * 获取当前有效的私钥
     * @return String Base64编码的私钥
     */
    String getCurrentPrivateKey();
    
    /**
     * 根据密钥ID获取公钥
     * @param keyId 密钥ID
     * @return String Base64编码的公钥
     */
    String getPublicKeyById(String keyId);
    
    /**
     * 根据密钥ID获取私钥
     * @param keyId 密钥ID
     * @return String Base64编码的私钥
     */
    String getPrivateKeyById(String keyId);
    
    /**
     * 执行密钥轮换
     * @return String 新的密钥ID
     */
    String rotateKeys();
    
    /**
     * 检查是否需要密钥轮换
     * @return boolean 是否需要轮换
     */
    boolean needsKeyRotation();
    
    /**
     * 获取当前密钥ID
     * @return String 当前密钥ID
     */
    String getCurrentKeyId();
    
    /**
     * 密钥信息数据结构
     */
    class KeyInfo {
        private final KeyPair keyPair;
        private final String keyId;
        private final LocalDateTime createdAt;
        private final LocalDateTime expiresAt;
        private final boolean active;
        
        public KeyInfo(KeyPair keyPair, String keyId, LocalDateTime createdAt, 
                      LocalDateTime expiresAt, boolean active) {
            this.keyPair = keyPair;
            this.keyId = keyId;
            this.createdAt = createdAt;
            this.expiresAt = expiresAt;
            this.active = active;
        }
        
        // Getters
        public KeyPair getKeyPair() { return keyPair; }
        public String getKeyId() { return keyId; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public boolean isActive() { return active; }
        public PublicKey getPublicKey() { return keyPair.getPublic(); }
        public PrivateKey getPrivateKey() { return keyPair.getPrivate(); }
        
        public String getPublicKeyBase64() {
            return Base64.getEncoder().encodeToString(getPublicKey().getEncoded());
        }
        
        public String getPrivateKeyBase64() {
            return Base64.getEncoder().encodeToString(getPrivateKey().getEncoded());
        }
        
        public boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}