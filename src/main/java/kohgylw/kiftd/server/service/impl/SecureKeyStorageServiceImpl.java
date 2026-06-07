package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.SecureKeyStorageService;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Enumeration;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.math.BigInteger;
import javax.security.auth.x500.X500Principal;

/**
 * 安全密钥存储服务实现
 * 基于Java KeyStore提供企业级密钥安全存储
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class SecureKeyStorageServiceImpl implements SecureKeyStorageService {
    
    private static final String KEYSTORE_TYPE = "PKCS12";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String CERTIFICATE_TYPE = "X.509";
    
    @Value("${security.keystore.path:./conf/security/keystore.p12}")
    private String keystorePath;
    
    @Value("${security.keystore.backup.path:./conf/security/backup/}")
    private String backupPath;
    
    private KeyStore keyStore;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
    private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
    
    @PostConstruct
    public void initialize() {
        try {
            initializeKeyStore();
            Printer.instance.print("安全密钥存储服务初始化完成，KeyStore路径: " + keystorePath);
        } catch (Exception e) {
            Printer.instance.print("安全密钥存储服务初始化失败: " + e.getMessage());
            throw new RuntimeException("Failed to initialize Secure Key Storage Service", e);
        }
    }
    
    private void initializeKeyStore() throws Exception {
        keyStore = KeyStore.getInstance(KEYSTORE_TYPE);
        
        File keystoreFile = new File(keystorePath);
        File keystoreDir = keystoreFile.getParentFile();
        
        // 创建目录
        if (!keystoreDir.exists()) {
            keystoreDir.mkdirs();
        }
        
        if (keystoreFile.exists()) {
            // 加载现有密钥库
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                // 注意：这里使用默认密码，实际部署时应该从安全的配置中获取
                keyStore.load(fis, getDefaultPassword());
            }
        } else {
            // 创建新的密钥库
            keyStore.load(null, null);
            saveKeyStore(getDefaultPassword());
        }
        
        // 创建备份目录
        File backupDir = new File(backupPath);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }
    }
    
    @Override
    public boolean storeKeyPair(String keyId, PublicKey publicKey, PrivateKey privateKey, char[] password) {
        writeLock.lock();
        try {
            // 创建自签名证书
            X509Certificate certificate = createSelfSignedCertificate(keyId, publicKey, privateKey);
            
            // 存储私钥和证书链
            Certificate[] certificateChain = {certificate};
            keyStore.setKeyEntry(keyId + "_private", privateKey, password, certificateChain);
            
            // 存储公钥证书
            keyStore.setCertificateEntry(keyId + "_public", certificate);
            
            // 保存密钥库
            saveKeyStore(getDefaultPassword());
            
            Printer.instance.print("密钥对已存储到安全存储，密钥ID: " + keyId);
            return true;
            
        } catch (Exception e) {
            Printer.instance.print("存储密钥对失败: " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public Optional<PublicKey> retrievePublicKey(String keyId) {
        readLock.lock();
        try {
            Certificate certificate = keyStore.getCertificate(keyId + "_public");
            if (certificate != null) {
                return Optional.of(certificate.getPublicKey());
            }
            return Optional.empty();
        } catch (Exception e) {
            Printer.instance.print("检索公钥失败: " + e.getMessage());
            return Optional.empty();
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public Optional<PrivateKey> retrievePrivateKey(String keyId, char[] password) {
        readLock.lock();
        try {
            Key key = keyStore.getKey(keyId + "_private", password);
            if (key instanceof PrivateKey) {
                return Optional.of((PrivateKey) key);
            }
            return Optional.empty();
        } catch (Exception e) {
            Printer.instance.print("检索私钥失败: " + e.getMessage());
            return Optional.empty();
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public boolean deleteKeyPair(String keyId, char[] password) {
        writeLock.lock();
        try {
            keyStore.deleteEntry(keyId + "_private");
            keyStore.deleteEntry(keyId + "_public");
            saveKeyStore(getDefaultPassword());
            
            Printer.instance.print("已删除密钥对，密钥ID: " + keyId);
            return true;
        } catch (Exception e) {
            Printer.instance.print("删除密钥对失败: " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public boolean keyExists(String keyId) {
        readLock.lock();
        try {
            return keyStore.containsAlias(keyId + "_private") || keyStore.containsAlias(keyId + "_public");
        } catch (Exception e) {
            Printer.instance.print("检查密钥存在性失败: " + e.getMessage());
            return false;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public boolean createBackup(String backupPath, char[] password) {
        readLock.lock();
        try {
            String timestamp = LocalDateTime.now().toString().replace(":", "-");
            String backupFileName = "keystore_backup_" + timestamp + ".p12";
            File backupFile = new File(backupPath, backupFileName);
            
            try (FileOutputStream fos = new FileOutputStream(backupFile)) {
                keyStore.store(fos, password);
            }
            
            Printer.instance.print("密钥库备份已创建: " + backupFile.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Printer.instance.print("创建密钥库备份失败: " + e.getMessage());
            return false;
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public boolean restoreFromBackup(String backupPath, char[] password) {
        writeLock.lock();
        try {
            File backupFile = new File(backupPath);
            if (!backupFile.exists()) {
                Printer.instance.print("备份文件不存在: " + backupPath);
                return false;
            }
            
            KeyStore backupKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            try (FileInputStream fis = new FileInputStream(backupFile)) {
                backupKeyStore.load(fis, password);
            }
            
            this.keyStore = backupKeyStore;
            saveKeyStore(getDefaultPassword());
            
            Printer.instance.print("从备份恢复密钥库成功: " + backupPath);
            return true;
        } catch (Exception e) {
            Printer.instance.print("从备份恢复密钥库失败: " + e.getMessage());
            return false;
        } finally {
            writeLock.unlock();
        }
    }
    
    @Override
    public String getKeyStoreInfo() {
        readLock.lock();
        try {
            StringBuilder info = new StringBuilder();
            info.append("密钥库信息:\n");
            info.append("类型: ").append(keyStore.getType()).append("\n");
            info.append("提供者: ").append(keyStore.getProvider().getName()).append("\n");
            info.append("大小: ").append(keyStore.size()).append("\n");
            info.append("条目:\n");
            
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                info.append("  - ").append(alias);
                if (keyStore.isKeyEntry(alias)) {
                    info.append(" (密钥条目)");
                } else if (keyStore.isCertificateEntry(alias)) {
                    info.append(" (证书条目)");
                }
                info.append("\n");
            }
            
            return info.toString();
        } catch (Exception e) {
            return "获取密钥库信息失败: " + e.getMessage();
        } finally {
            readLock.unlock();
        }
    }
    
    @Override
    public boolean verifyIntegrity(char[] password) {
        readLock.lock();
        try {
            // 尝试重新加载密钥库以验证完整性
            KeyStore testKeyStore = KeyStore.getInstance(KEYSTORE_TYPE);
            File keystoreFile = new File(keystorePath);
            
            if (!keystoreFile.exists()) {
                return false;
            }
            
            try (FileInputStream fis = new FileInputStream(keystoreFile)) {
                testKeyStore.load(fis, password);
            }
            
            Printer.instance.print("密钥库完整性验证通过");
            return true;
        } catch (Exception e) {
            Printer.instance.print("密钥库完整性验证失败: " + e.getMessage());
            return false;
        } finally {
            readLock.unlock();
        }
    }
    
    /**
     * 保存密钥库到文件
     */
    private void saveKeyStore(char[] password) throws Exception {
        File keystoreFile = new File(keystorePath);
        try (FileOutputStream fos = new FileOutputStream(keystoreFile)) {
            keyStore.store(fos, password);
        }
    }
    
    /**
     * 创建自签名证书
     */
    private X509Certificate createSelfSignedCertificate(String keyId, PublicKey publicKey, PrivateKey privateKey) 
            throws Exception {
        
        // 注意：这是一个简化的自签名证书创建方法
        // 在实际企业环境中，应该使用专业的证书颁发机构或更完善的证书生成库
        
        // 这里返回一个临时的证书实现
        // 实际应用中建议使用 BouncyCastle 等专业加密库
        return createTemporaryCertificate(keyId, publicKey, privateKey);
    }
    
    /**
     * 创建临时证书（简化实现）
     * 注意：这是一个基础实现，生产环境应使用专业的证书生成库
     */
    private X509Certificate createTemporaryCertificate(String keyId, PublicKey publicKey, PrivateKey privateKey) 
            throws Exception {
        
        // 创建基本的证书信息
        String distinguishedName = "CN=" + keyId + ", O=KIFTD Enterprise, C=CN";
        
        // 使用JDK内置的证书生成功能
        // 注意：这需要依赖内部API，生产环境建议使用BouncyCastle
        return new TemporaryCertificate(keyId, publicKey, distinguishedName);
    }
    
    /**
     * 获取默认密钥库密码
     * 注意：生产环境应该从安全的配置源获取密码
     */
    private char[] getDefaultPassword() {
        // 这里使用默认密码，实际部署时应该从环境变量或安全配置中获取
        return "kiftd-enterprise-2024".toCharArray();
    }
    
    /**
     * 临时证书实现（简化版）
     * 生产环境建议使用专业的证书库如BouncyCastle
     */
    private static class TemporaryCertificate extends X509Certificate {
        private final String keyId;
        private final PublicKey publicKey;
        private final String distinguishedName;
        private final Date validFrom;
        private final Date validTo;
        
        public TemporaryCertificate(String keyId, PublicKey publicKey, String distinguishedName) {
            this.keyId = keyId;
            this.publicKey = publicKey;
            this.distinguishedName = distinguishedName;
            this.validFrom = new Date();
            this.validTo = Date.from(LocalDateTime.now().plusYears(1).atZone(ZoneId.systemDefault()).toInstant());
        }
        
        @Override
        public void checkValidity() throws CertificateException {
            Date now = new Date();
            if (now.before(validFrom) || now.after(validTo)) {
                throw new CertificateException("Certificate is not valid");
            }
        }
        
        @Override
        public void checkValidity(Date date) throws CertificateException {
            if (date.before(validFrom) || date.after(validTo)) {
                throw new CertificateException("Certificate is not valid for date: " + date);
            }
        }
        
        @Override
        public int getVersion() { return 3; }
        
        @Override
        public BigInteger getSerialNumber() { return BigInteger.valueOf(keyId.hashCode()); }
        
        @Override
        public Principal getIssuerDN() { return new X500Principal(distinguishedName); }
        
        @Override
        public Principal getSubjectDN() { return new X500Principal(distinguishedName); }
        
        @Override
        public Date getNotBefore() { return validFrom; }
        
        @Override
        public Date getNotAfter() { return validTo; }
        
        @Override
        public byte[] getTBSCertificate() { return new byte[0]; }
        
        @Override
        public byte[] getSignature() { return new byte[0]; }
        
        @Override
        public String getSigAlgName() { return SIGNATURE_ALGORITHM; }
        
        @Override
        public String getSigAlgOID() { return "1.2.840.113549.1.1.11"; }
        
        @Override
        public byte[] getSigAlgParams() { return null; }
        
        @Override
        public boolean[] getIssuerUniqueID() { return null; }
        
        @Override
        public boolean[] getSubjectUniqueID() { return null; }
        
        @Override
        public boolean[] getKeyUsage() { return null; }
        
        @Override
        public int getBasicConstraints() { return -1; }
        
        @Override
        public byte[] getEncoded() { return new byte[0]; }
        
        @Override
        public void verify(PublicKey key) { }
        
        @Override
        public void verify(PublicKey key, String sigProvider) { }
        
        @Override
        public PublicKey getPublicKey() { return publicKey; }
        
        @Override
        public String toString() {
            return "TemporaryCertificate[keyId=" + keyId + ", dn=" + distinguishedName + "]";
        }
    }
}