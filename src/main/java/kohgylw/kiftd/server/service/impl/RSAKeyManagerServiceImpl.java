package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.RSAKeyManagerService;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 企业级RSA密钥管理服务实现
 * 支持RSA 4096位密钥对生成、密钥轮换、安全存储
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class RSAKeyManagerServiceImpl implements RSAKeyManagerService {
    
    private static final int RSA_KEY_SIZE = 4096;
    private static final String ALGORITHM = "RSA";
    
    // 密钥有效期（小时）
    @Value("${security.rsa.key.validity.hours:168}") // 默认7天
    private int keyValidityHours;
    
    // 密钥轮换检查间隔（小时）
    @Value("${security.rsa.key.rotation.check.hours:24}") // 默认24小时检查一次
    private int rotationCheckHours;
    
    // 是否启用自动密钥轮换
    @Value("${security.rsa.key.auto.rotation.enabled:true}")
    private boolean autoRotationEnabled;
    
    // 密钥存储
    private final ConcurrentHashMap<String, KeyInfo> keyStore = new ConcurrentHashMap<>();
    private volatile String currentKeyId;
    
    // 读写锁
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    
    // 定时任务执行器
    private ScheduledExecutorService scheduler;
    
    // 安全随机数生成器
    private final SecureRandom secureRandom = new SecureRandom();
    
    @PostConstruct
    public void initialize() {
        try {
            Printer.instance.print("初始化企业级RSA密钥管理服务...");
            
            // 生成初始密钥对
            String initialKeyId = generateInitialKeyPair();
            Printer.instance.print("生成初始RSA 4096位密钥对，密钥ID: " + initialKeyId);
            
            // 启动自动密钥轮换调度器
            if (autoRotationEnabled) {
                startKeyRotationScheduler();
                Printer.instance.print("自动密钥轮换调度器已启动，检查间隔: " + rotationCheckHours + " 小时");
            }
            
            Printer.instance.print("RSA密钥管理服务初始化完成");
            
        } catch (Exception e) {
            Printer.instance.print("RSA密钥管理服务初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize RSA Key Manager Service", e);
        }
    }
    
    @PreDestroy
    public void cleanup() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // 清理敏感数据
        keyStore.clear();
        Printer.instance.print("RSA密钥管理服务已清理");
    }
    
    @Override
    public KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(ALGORITHM);
        generator.initialize(RSA_KEY_SIZE, secureRandom);
        return generator.generateKeyPair();
    }
    
    @Override
    public String getCurrentPublicKey() {
        readLock.lock();
        try {
            KeyInfo keyInfo = keyStore.get(currentKeyId);
            return keyInfo != null ? keyInfo.getPublicKeyBase64() : null;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public String getCurrentPrivateKey() {
        readLock.lock();
        try {
            KeyInfo keyInfo = keyStore.get(currentKeyId);
            return keyInfo != null ? keyInfo.getPrivateKeyBase64() : null;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public String getPublicKeyById(String keyId) {
        readLock.lock();
        try {
            KeyInfo keyInfo = keyStore.get(keyId);
            return keyInfo != null ? keyInfo.getPublicKeyBase64() : null;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public String getPrivateKeyById(String keyId) {
        readLock.lock();
        try {
            KeyInfo keyInfo = keyStore.get(keyId);
            return keyInfo != null ? keyInfo.getPrivateKeyBase64() : null;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public String rotateKeys() {
        writeLock.lock();
        try {
            String newKeyId = generateNewKeyPair();
            
            // 标记旧密钥为非活跃状态
            if (currentKeyId != null) {
                KeyInfo oldKeyInfo = keyStore.get(currentKeyId);
                if (oldKeyInfo != null) {
                    KeyInfo updatedOldKeyInfo = new KeyInfo(
                        oldKeyInfo.getKeyPair(),
                        oldKeyInfo.getKeyId(),
                        oldKeyInfo.getCreatedAt(),
                        oldKeyInfo.getExpiresAt(),
                        false // 设为非活跃
                    );
                    keyStore.put(currentKeyId, updatedOldKeyInfo);
                }
            }
            
            currentKeyId = newKeyId;
            
            // 清理过期密钥
            cleanupExpiredKeys();
            
            Printer.instance.print("密钥轮换完成，新密钥ID: " + newKeyId);
            return newKeyId;
            
        } catch (Exception e) {
            Printer.instance.print("密钥轮换失败: " + e.getMessage());
            throw new RuntimeException("Key rotation failed", e);
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public boolean needsKeyRotation() {
        readLock.lock();
        try {
            if (currentKeyId == null) {
                return true;
            }
            
            KeyInfo currentKeyInfo = keyStore.get(currentKeyId);
            if (currentKeyInfo == null) {
                return true;
            }
            
            return currentKeyInfo.isExpired();
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public String getCurrentKeyId() {
        return currentKeyId;
    }
    
    /**
     * 生成初始密钥对
     */
    private String generateInitialKeyPair() throws NoSuchAlgorithmException {
        return generateNewKeyPair();
    }
    
    /**
     * 生成新的密钥对
     */
    private String generateNewKeyPair() throws NoSuchAlgorithmException {
        KeyPair keyPair = generateKeyPair();
        String keyId = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusHours(keyValidityHours);
        
        KeyInfo keyInfo = new KeyInfo(keyPair, keyId, now, expiresAt, true);
        keyStore.put(keyId, keyInfo);
        
        return keyId;
    }
    
    /**
     * 启动密钥轮换调度器
     */
    private void startKeyRotationScheduler() {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "RSA-Key-Rotation-Scheduler");
            t.setDaemon(true);
            return t;
        });
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (needsKeyRotation()) {
                    rotateKeys();
                }
            } catch (Exception e) {
                Printer.instance.print("自动密钥轮换失败: " + e.getMessage());
            }
        }, rotationCheckHours, rotationCheckHours, TimeUnit.HOURS);
    }
    
    /**
     * 清理过期密钥
     */
    private void cleanupExpiredKeys() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(keyValidityHours * 2); // 保留2倍有效期的密钥
        
        keyStore.entrySet().removeIf(entry -> {
            KeyInfo keyInfo = entry.getValue();
            boolean shouldRemove = keyInfo.getCreatedAt().isBefore(cutoffTime) && !keyInfo.isActive();
            if (shouldRemove) {
                Printer.instance.print("清理过期密钥: " + entry.getKey());
            }
            return shouldRemove;
        });
    }
    
    /**
     * 获取密钥存储统计信息
     */
    public String getKeyStoreStats() {
        readLock.lock();
        try {
            long activeKeys = keyStore.values().stream().mapToLong(k -> k.isActive() ? 1 : 0).sum();
            long expiredKeys = keyStore.values().stream().mapToLong(k -> k.isExpired() ? 1 : 0).sum();
            
            return String.format("密钥存储统计 - 总计: %d, 活跃: %d, 过期: %d, 当前密钥ID: %s",
                    keyStore.size(), activeKeys, expiredKeys, currentKeyId);
        } finally {
            readLock.unlock();
        }
    }
}