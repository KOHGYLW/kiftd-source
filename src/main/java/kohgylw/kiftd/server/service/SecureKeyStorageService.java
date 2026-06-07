package kohgylw.kiftd.server.service;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.util.Optional;

/**
 * 安全密钥存储服务接口
 * 提供企业级的密钥安全存储和检索功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface SecureKeyStorageService {
    
    /**
     * 存储RSA密钥对到安全存储
     * @param keyId 密钥ID
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @param password 密钥库密码
     * @return boolean 存储是否成功
     */
    boolean storeKeyPair(String keyId, PublicKey publicKey, PrivateKey privateKey, char[] password);
    
    /**
     * 从安全存储中检索公钥
     * @param keyId 密钥ID
     * @return Optional<PublicKey> 公钥
     */
    Optional<PublicKey> retrievePublicKey(String keyId);
    
    /**
     * 从安全存储中检索私钥
     * @param keyId 密钥ID
     * @param password 密钥库密码
     * @return Optional<PrivateKey> 私钥
     */
    Optional<PrivateKey> retrievePrivateKey(String keyId, char[] password);
    
    /**
     * 删除指定的密钥对
     * @param keyId 密钥ID
     * @param password 密钥库密码
     * @return boolean 删除是否成功
     */
    boolean deleteKeyPair(String keyId, char[] password);
    
    /**
     * 检查密钥是否存在
     * @param keyId 密钥ID
     * @return boolean 密钥是否存在
     */
    boolean keyExists(String keyId);
    
    /**
     * 创建密钥库备份
     * @param backupPath 备份路径
     * @param password 密钥库密码
     * @return boolean 备份是否成功
     */
    boolean createBackup(String backupPath, char[] password);
    
    /**
     * 从备份恢复密钥库
     * @param backupPath 备份路径
     * @param password 密钥库密码
     * @return boolean 恢复是否成功
     */
    boolean restoreFromBackup(String backupPath, char[] password);
    
    /**
     * 获取密钥库信息
     * @return String 密钥库统计信息
     */
    String getKeyStoreInfo();
    
    /**
     * 验证密钥库完整性
     * @param password 密钥库密码
     * @return boolean 验证是否通过
     */
    boolean verifyIntegrity(char[] password);
}