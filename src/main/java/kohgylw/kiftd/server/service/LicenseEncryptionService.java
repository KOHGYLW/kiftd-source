package kohgylw.kiftd.server.service;

import kohgylw.kiftd.server.pojo.LicenseData;

/**
 * 企业级授权码加密服务接口
 * 支持AES-256-GCM对称加密
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface LicenseEncryptionService {
    
    /**
     * 加密授权码数据
     * @param licenseData 授权码数据
     * @param encryptionKey 加密密钥（Base64编码）
     * @return String 加密后的授权码（Base64编码）
     * @throws Exception 加密异常
     */
    String encryptLicense(LicenseData licenseData, String encryptionKey) throws Exception;
    
    /**
     * 解密授权码数据
     * @param encryptedLicense 加密的授权码（Base64编码）
     * @param encryptionKey 解密密钥（Base64编码）
     * @return LicenseData 解密后的授权码数据
     * @throws Exception 解密异常
     */
    LicenseData decryptLicense(String encryptedLicense, String encryptionKey) throws Exception;
    
    /**
     * 生成AES-256密钥
     * @return String Base64编码的密钥
     * @throws Exception 密钥生成异常
     */
    String generateEncryptionKey() throws Exception;
    
    /**
     * 验证加密密钥的有效性
     * @param encryptionKey 加密密钥（Base64编码）
     * @return boolean 密钥是否有效
     */
    boolean validateEncryptionKey(String encryptionKey);
    
    /**
     * 生成完整的授权码字符串（包含加密数据和元信息）
     * @param licenseData 授权码数据
     * @param encryptionKey 加密密钥
     * @return String 完整的授权码字符串
     * @throws Exception 生成异常
     */
    String generateLicenseString(LicenseData licenseData, String encryptionKey) throws Exception;
    
    /**
     * 解析完整的授权码字符串
     * @param licenseString 完整的授权码字符串
     * @param encryptionKey 解密密钥
     * @return LicenseData 解析后的授权码数据
     * @throws Exception 解析异常
     */
    LicenseData parseLicenseString(String licenseString, String encryptionKey) throws Exception;
}