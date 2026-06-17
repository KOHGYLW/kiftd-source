package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.LicenseEncryptionService;
import kohgylw.kiftd.server.pojo.LicenseData;
import kohgylw.kiftd.printer.Printer;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 企业级授权码加密服务实现
 * 使用AES-256-GCM提供高级加密标准
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class LicenseEncryptionServiceImpl implements LicenseEncryptionService {
    
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH = 256; // AES-256
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 16; // 128 bits
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    
    @Override
    public String encryptLicense(LicenseData licenseData, String encryptionKey) throws Exception {
        if (licenseData == null) {
            throw new IllegalArgumentException("License data cannot be null");
        }
        
        if (!validateEncryptionKey(encryptionKey)) {
            throw new IllegalArgumentException("Invalid encryption key");
        }
        
        // 将授权码数据转换为JSON
        String jsonData = licenseData.toJsonForSigning();
        byte[] plaintext = jsonData.getBytes(StandardCharsets.UTF_8);
        
        // 解码密钥
        byte[] keyBytes = decoder.decode(encryptionKey);
        SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        // 初始化密码器
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        
        // 执行加密
        byte[] ciphertext = cipher.doFinal(plaintext);
        
        // 组合IV和密文
        byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);
        
        String result = encoder.encodeToString(encryptedData);
        Printer.instance.print("授权码加密完成，数据长度: " + result.length());
        
        return result;
    }
    
    @Override
    public LicenseData decryptLicense(String encryptedLicense, String encryptionKey) throws Exception {
        if (encryptedLicense == null || encryptedLicense.trim().isEmpty()) {
            throw new IllegalArgumentException("Encrypted license cannot be null or empty");
        }
        
        if (!validateEncryptionKey(encryptionKey)) {
            throw new IllegalArgumentException("Invalid encryption key");
        }
        
        try {
            // 解码加密数据
            byte[] encryptedData = decoder.decode(encryptedLicense);
            
            if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
                throw new IllegalArgumentException("Invalid encrypted data length");
            }
            
            // 提取IV和密文
            byte[] iv = new byte[GCM_IV_LENGTH];
            byte[] ciphertext = new byte[encryptedData.length - GCM_IV_LENGTH];
            System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
            System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertext, 0, ciphertext.length);
            
            // 解码密钥
            byte[] keyBytes = decoder.decode(encryptionKey);
            SecretKey secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
            
            // 初始化密码器
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
            
            // 执行解密
            byte[] plaintext = cipher.doFinal(ciphertext);
            String jsonData = new String(plaintext, StandardCharsets.UTF_8);
            
            Printer.instance.print("授权码解密完成");
            return LicenseData.fromJson(jsonData);
            
        } catch (Exception e) {
            Printer.instance.print("授权码解密失败: " + e.getMessage());
            throw new Exception("Failed to decrypt license: " + e.getMessage(), e);
        }
    }
    
    @Override
    public String generateEncryptionKey() throws Exception {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance(ALGORITHM);
            keyGenerator.init(KEY_LENGTH, secureRandom);
            SecretKey secretKey = keyGenerator.generateKey();
            
            String keyBase64 = encoder.encodeToString(secretKey.getEncoded());
            Printer.instance.print("生成AES-256加密密钥");
            
            return keyBase64;
        } catch (NoSuchAlgorithmException e) {
            throw new Exception("Failed to generate encryption key", e);
        }
    }
    
    @Override
    public boolean validateEncryptionKey(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
            return false;
        }
        
        try {
            byte[] keyBytes = decoder.decode(encryptionKey);
            return keyBytes.length == KEY_LENGTH / 8; // 256 bits = 32 bytes
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    public String generateLicenseString(LicenseData licenseData, String encryptionKey) throws Exception {
        // 加密授权码数据
        String encryptedData = encryptLicense(licenseData, encryptionKey);
        
        // 创建授权码头部信息
        LicenseHeader header = new LicenseHeader();
        header.setVersion("2.0");
        header.setEncryption("AES-256-GCM");
        header.setCustomerId(licenseData.getCustomerId());
        header.setProductId(licenseData.getProductId());
        
        // 组合完整的授权码字符串
        StringBuilder licenseString = new StringBuilder();
        licenseString.append("-----BEGIN KIFTD LICENSE-----\n");
        licenseString.append("Version: ").append(header.getVersion()).append("\n");
        licenseString.append("Encryption: ").append(header.getEncryption()).append("\n");
        licenseString.append("Customer: ").append(header.getCustomerId()).append("\n");
        licenseString.append("Product: ").append(header.getProductId()).append("\n");
        licenseString.append("\n");
        
        // 分行输出加密数据（每行64字符）
        String data = encryptedData;
        for (int i = 0; i < data.length(); i += 64) {
            int end = Math.min(i + 64, data.length());
            licenseString.append(data.substring(i, end)).append("\n");
        }
        
        licenseString.append("-----END KIFTD LICENSE-----");
        
        return licenseString.toString();
    }
    
    @Override
    public LicenseData parseLicenseString(String licenseString, String encryptionKey) throws Exception {
        if (licenseString == null || licenseString.trim().isEmpty()) {
            throw new IllegalArgumentException("License string cannot be null or empty");
        }
        
        // 验证授权码格式
        if (!licenseString.contains("-----BEGIN KIFTD LICENSE-----") ||
            !licenseString.contains("-----END KIFTD LICENSE-----")) {
            throw new IllegalArgumentException("Invalid license format");
        }
        
        try {
            // 提取头部信息和数据部分
            String[] lines = licenseString.split("\n");
            StringBuilder dataBuilder = new StringBuilder();
            boolean inDataSection = false;
            
            LicenseHeader header = new LicenseHeader();
            
            for (String line : lines) {
                line = line.trim();
                
                if (line.equals("-----BEGIN KIFTD LICENSE-----")) {
                    continue;
                } else if (line.equals("-----END KIFTD LICENSE-----")) {
                    break;
                } else if (line.startsWith("Version:")) {
                    header.setVersion(line.substring(8).trim());
                } else if (line.startsWith("Encryption:")) {
                    header.setEncryption(line.substring(11).trim());
                } else if (line.startsWith("Customer:")) {
                    header.setCustomerId(line.substring(9).trim());
                } else if (line.startsWith("Product:")) {
                    header.setProductId(line.substring(8).trim());
                } else if (line.isEmpty()) {
                    inDataSection = true;
                } else if (inDataSection) {
                    dataBuilder.append(line);
                }
            }
            
            // 验证头部信息
            if (!"2.0".equals(header.getVersion())) {
                throw new IllegalArgumentException("Unsupported license version: " + header.getVersion());
            }
            
            if (!"AES-256-GCM".equals(header.getEncryption())) {
                throw new IllegalArgumentException("Unsupported encryption method: " + header.getEncryption());
            }
            
            String encryptedData = dataBuilder.toString();
            if (encryptedData.isEmpty()) {
                throw new IllegalArgumentException("No encrypted data found in license");
            }
            
            // 解密授权码数据
            return decryptLicense(encryptedData, encryptionKey);
            
        } catch (Exception e) {
            throw new Exception("Failed to parse license string: " + e.getMessage(), e);
        }
    }
    
    /**
     * 授权码头部信息
     */
    private static class LicenseHeader {
        private String version;
        private String encryption;
        private String customerId;
        private String productId;
        
        // Getters and Setters
        public String getVersion() {
            return version;
        }
        
        public void setVersion(String version) {
            this.version = version;
        }
        
        public String getEncryption() {
            return encryption;
        }
        
        public void setEncryption(String encryption) {
            this.encryption = encryption;
        }
        
        public String getCustomerId() {
            return customerId;
        }
        
        public void setCustomerId(String customerId) {
            this.customerId = customerId;
        }
        
        public String getProductId() {
            return productId;
        }
        
        public void setProductId(String productId) {
            this.productId = productId;
        }
    }
}