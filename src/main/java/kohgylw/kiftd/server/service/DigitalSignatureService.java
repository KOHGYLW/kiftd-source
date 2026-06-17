package kohgylw.kiftd.server.service;

import kohgylw.kiftd.server.pojo.LicenseData;

/**
 * 数字签名服务接口
 * 提供RSA数字签名和验证功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface DigitalSignatureService {
    
    /**
     * 对数据进行数字签名
     * @param data 要签名的数据
     * @param privateKey 私钥（Base64编码）
     * @return String 数字签名（Base64编码）
     * @throws Exception 签名异常
     */
    String sign(String data, String privateKey) throws Exception;
    
    /**
     * 验证数字签名
     * @param data 原始数据
     * @param signature 数字签名（Base64编码）
     * @param publicKey 公钥（Base64编码）
     * @return boolean 签名是否有效
     * @throws Exception 验证异常
     */
    boolean verify(String data, String signature, String publicKey) throws Exception;
    
    /**
     * 对授权码数据进行签名
     * @param licenseData 授权码数据
     * @param privateKey 私钥（Base64编码）
     * @return String 数字签名（Base64编码）
     * @throws Exception 签名异常
     */
    String signLicense(LicenseData licenseData, String privateKey) throws Exception;
    
    /**
     * 验证授权码数字签名
     * @param licenseData 授权码数据
     * @param signature 数字签名（Base64编码）
     * @param publicKey 公钥（Base64编码）
     * @return boolean 签名是否有效
     * @throws Exception 验证异常
     */
    boolean verifyLicense(LicenseData licenseData, String signature, String publicKey) throws Exception;
    
    /**
     * 计算数据的SHA-256哈希值
     * @param data 原始数据
     * @return String 哈希值（十六进制字符串）
     * @throws Exception 计算异常
     */
    String calculateHash(String data) throws Exception;
    
    /**
     * 计算数据的SHA-256哈希值（字节数组输入）
     * @param data 原始数据
     * @return String 哈希值（十六进制字符串）
     * @throws Exception 计算异常
     */
    String calculateHash(byte[] data) throws Exception;
    
    /**
     * 验证数据完整性
     * @param data 数据
     * @param expectedHash 期望的哈希值
     * @return boolean 数据是否完整
     * @throws Exception 验证异常
     */
    boolean verifyIntegrity(String data, String expectedHash) throws Exception;
}