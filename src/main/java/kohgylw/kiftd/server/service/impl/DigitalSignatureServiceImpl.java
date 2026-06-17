package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.DigitalSignatureService;
import kohgylw.kiftd.server.pojo.LicenseData;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 数字签名服务实现
 * 使用RSA-SHA256提供数字签名和验证功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class DigitalSignatureServiceImpl implements DigitalSignatureService {
    
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String KEY_ALGORITHM = "RSA";
    
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    
    @Override
    public String sign(String data, String privateKey) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data to sign cannot be null or empty");
        }
        
        if (privateKey == null || privateKey.isEmpty()) {
            throw new IllegalArgumentException("Private key cannot be null or empty");
        }
        
        try {
            // 解码私钥
            byte[] privateKeyBytes = decoder.decode(privateKey);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PrivateKey key = keyFactory.generatePrivate(keySpec);
            
            // 初始化签名器
            Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
            signature.initSign(key);
            
            // 签名数据
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = signature.sign();
            
            String result = encoder.encodeToString(signatureBytes);
            Printer.instance.print("数据签名完成，签名长度: " + result.length());
            
            return result;
            
        } catch (Exception e) {
            Printer.instance.print("数据签名失败: " + e.getMessage());
            throw new Exception("Failed to sign data: " + e.getMessage(), e);
        }
    }
    
    @Override
    public boolean verify(String data, String signature, String publicKey) throws Exception {
        if (data == null || data.isEmpty()) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        if (signature == null || signature.isEmpty()) {
            throw new IllegalArgumentException("Signature cannot be null or empty");
        }
        
        if (publicKey == null || publicKey.isEmpty()) {
            throw new IllegalArgumentException("Public key cannot be null or empty");
        }
        
        try {
            // 解码公钥
            byte[] publicKeyBytes = decoder.decode(publicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
            PublicKey key = keyFactory.generatePublic(keySpec);
            
            // 初始化验证器
            Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
            sig.initVerify(key);
            
            // 验证签名
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            byte[] signatureBytes = decoder.decode(signature);
            boolean isValid = sig.verify(signatureBytes);
            
            Printer.instance.print("数字签名验证" + (isValid ? "通过" : "失败"));
            return isValid;
            
        } catch (Exception e) {
            Printer.instance.print("数字签名验证异常: " + e.getMessage());
            throw new Exception("Failed to verify signature: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String signLicense(LicenseData licenseData, String privateKey) throws Exception {
        if (licenseData == null) {
            throw new IllegalArgumentException("License data cannot be null");
        }
        
        // 使用授权码的JSON表示进行签名（不包含数字签名字段）
        String dataToSign = licenseData.toJsonForSigning();
        String signature = sign(dataToSign, privateKey);
        
        // 设置数字签名到授权码数据中
        licenseData.setDigitalSignature(signature);
        
        Printer.instance.print("授权码数字签名完成，客户ID: " + licenseData.getCustomerId());
        return signature;
    }
    
    @Override
    public boolean verifyLicense(LicenseData licenseData, String signature, String publicKey) throws Exception {
        if (licenseData == null) {
            throw new IllegalArgumentException("License data cannot be null");
        }
        
        // 暂时移除数字签名字段以进行验证
        String originalSignature = licenseData.getDigitalSignature();
        licenseData.setDigitalSignature(null);
        
        try {
            String dataToVerify = licenseData.toJsonForSigning();
            boolean isValid = verify(dataToVerify, signature, publicKey);
            
            Printer.instance.print("授权码数字签名验证" + (isValid ? "通过" : "失败") + 
                                 "，客户ID: " + licenseData.getCustomerId());
            return isValid;
            
        } finally {
            // 恢复原始签名
            licenseData.setDigitalSignature(originalSignature);
        }
    }
    
    @Override
    public String calculateHash(String data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        return calculateHash(data.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public String calculateHash(byte[] data) throws Exception {
        if (data == null || data.length == 0) {
            throw new IllegalArgumentException("Data cannot be null or empty");
        }
        
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            byte[] hashBytes = digest.digest(data);
            
            // 转换为十六进制字符串
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString().toLowerCase();
            
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Hash algorithm not available: " + HASH_ALGORITHM, e);
        }
    }
    
    @Override
    public boolean verifyIntegrity(String data, String expectedHash) throws Exception {
        if (data == null || expectedHash == null) {
            throw new IllegalArgumentException("Data and expected hash cannot be null");
        }
        
        String actualHash = calculateHash(data);
        boolean isValid = actualHash.equalsIgnoreCase(expectedHash);
        
        Printer.instance.print("数据完整性验证" + (isValid ? "通过" : "失败"));
        
        if (!isValid) {
            Printer.instance.print("期望哈希: " + expectedHash);
            Printer.instance.print("实际哈希: " + actualHash);
        }
        
        return isValid;
    }
    
    /**
     * 生成数据摘要用于签名预处理
     */
    public String generateDigest(String data) throws Exception {
        return calculateHash(data);
    }
    
    /**
     * 批量验证多个签名
     */
    public boolean verifyBatch(String[] dataArray, String[] signatureArray, String publicKey) throws Exception {
        if (dataArray == null || signatureArray == null) {
            throw new IllegalArgumentException("Data and signature arrays cannot be null");
        }
        
        if (dataArray.length != signatureArray.length) {
            throw new IllegalArgumentException("Data and signature arrays must have the same length");
        }
        
        boolean allValid = true;
        
        for (int i = 0; i < dataArray.length; i++) {
            try {
                if (!verify(dataArray[i], signatureArray[i], publicKey)) {
                    allValid = false;
                    Printer.instance.print("批量验证失败，索引: " + i);
                }
            } catch (Exception e) {
                allValid = false;
                Printer.instance.print("批量验证异常，索引: " + i + ", 错误: " + e.getMessage());
            }
        }
        
        Printer.instance.print("批量签名验证完成，结果: " + (allValid ? "全部通过" : "存在失败"));
        return allValid;
    }
    
    /**
     * 验证密钥对匹配性
     */
    public boolean verifyKeyPairMatch(String privateKey, String publicKey) throws Exception {
        try {
            String testData = "KIFTD_KEY_PAIR_VERIFICATION_TEST_" + System.currentTimeMillis();
            String signature = sign(testData, privateKey);
            return verify(testData, signature, publicKey);
        } catch (Exception e) {
            Printer.instance.print("密钥对匹配性验证失败: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * 生成时间戳签名（包含当前时间）
     */
    public String signWithTimestamp(String data, String privateKey) throws Exception {
        String timestampedData = data + "|TIMESTAMP:" + System.currentTimeMillis();
        return sign(timestampedData, privateKey);
    }
    
    /**
     * 验证时间戳签名
     */
    public boolean verifyWithTimestamp(String data, String signature, String publicKey, long maxAge) throws Exception {
        try {
            // 尝试不同的时间戳来验证
            long currentTime = System.currentTimeMillis();
            long startTime = currentTime - maxAge;
            
            // 在时间窗口内尝试验证
            for (long timestamp = currentTime; timestamp >= startTime; timestamp -= 1000) {
                String timestampedData = data + "|TIMESTAMP:" + timestamp;
                if (verify(timestampedData, signature, publicKey)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            throw new Exception("Failed to verify timestamp signature: " + e.getMessage(), e);
        }
    }
}