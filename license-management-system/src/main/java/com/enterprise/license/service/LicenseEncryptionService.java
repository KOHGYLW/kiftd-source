package com.enterprise.license.service;

import com.enterprise.license.util.CryptoUtil;
import com.enterprise.license.util.HashUtil;
import com.enterprise.license.util.HardwareFingerprintUtil;
import com.enterprise.license.util.SecureRandomUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * 授权码加密服务
 * 使用AES-256-GCM对称加密和RSA数字签名确保授权码的安全性
 */
@Service
public class LicenseEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(LicenseEncryptionService.class);

    @Autowired
    private CryptoUtil cryptoUtil;

    @Autowired
    private HashUtil hashUtil;

    @Autowired
    private SecureRandomUtil secureRandomUtil;

    @Autowired
    private HardwareFingerprintUtil hardwareFingerprintUtil;

    @Autowired
    private KeyManagerService keyManagerService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 授权数据结构
     */
    public static class LicenseData {
        private String licenseId;
        private String customerId;
        private String productName;
        private String productVersion;
        private LocalDateTime issuedAt;
        private LocalDateTime expiresAt;
        private String hardwareFingerprint;
        private Map<String, Object> features;
        private Map<String, Object> restrictions;
        private String issuer;
        private String checksum;

        public LicenseData() {
            this.features = new HashMap<>();
            this.restrictions = new HashMap<>();
        }

        // Getters and Setters
        public String getLicenseId() { return licenseId; }
        public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

        public String getCustomerId() { return customerId; }
        public void setCustomerId(String customerId) { this.customerId = customerId; }

        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }

        public String getProductVersion() { return productVersion; }
        public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

        public LocalDateTime getIssuedAt() { return issuedAt; }
        public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

        public String getHardwareFingerprint() { return hardwareFingerprint; }
        public void setHardwareFingerprint(String hardwareFingerprint) { this.hardwareFingerprint = hardwareFingerprint; }

        public Map<String, Object> getFeatures() { return features; }
        public void setFeatures(Map<String, Object> features) { this.features = features; }

        public Map<String, Object> getRestrictions() { return restrictions; }
        public void setRestrictions(Map<String, Object> restrictions) { this.restrictions = restrictions; }

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }

        public String getChecksum() { return checksum; }
        public void setChecksum(String checksum) { this.checksum = checksum; }
    }

    /**
     * 加密的授权码结构
     */
    public static class EncryptedLicense {
        private String encryptedData;
        private String signature;
        private String keyId;
        private String algorithm;
        private LocalDateTime createdAt;
        private String version;

        // Getters and Setters
        public String getEncryptedData() { return encryptedData; }
        public void setEncryptedData(String encryptedData) { this.encryptedData = encryptedData; }

        public String getSignature() { return signature; }
        public void setSignature(String signature) { this.signature = signature; }

        public String getKeyId() { return keyId; }
        public void setKeyId(String keyId) { this.keyId = keyId; }

        public String getAlgorithm() { return algorithm; }
        public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }

    /**
     * 创建授权数据
     */
    public LicenseData createLicenseData(String customerId, String productName, String productVersion,
                                        int validityDays, Map<String, Object> features,
                                        Map<String, Object> restrictions) {
        LicenseData licenseData = new LicenseData();
        
        licenseData.setLicenseId(generateLicenseId());
        licenseData.setCustomerId(customerId);
        licenseData.setProductName(productName);
        licenseData.setProductVersion(productVersion);
        licenseData.setIssuedAt(LocalDateTime.now());
        licenseData.setExpiresAt(LocalDateTime.now().plusDays(validityDays));
        licenseData.setHardwareFingerprint(""); // 将在绑定时设置
        licenseData.setFeatures(features != null ? features : new HashMap<>());
        licenseData.setRestrictions(restrictions != null ? restrictions : new HashMap<>());
        licenseData.setIssuer("Enterprise License System");

        // 计算校验和
        String dataForChecksum = createChecksumData(licenseData);
        licenseData.setChecksum(hashUtil.sha256(dataForChecksum));

        logger.info("创建授权数据，授权ID: {}, 客户ID: {}", licenseData.getLicenseId(), customerId);
        return licenseData;
    }

    /**
     * 加密授权数据
     */
    public String encryptLicense(LicenseData licenseData, String rsaKeyId) {
        try {
            // 生成AES密钥用于数据加密
            SecretKey aesKey = cryptoUtil.generateAESKey();
            
            // 序列化授权数据
            String jsonData = objectMapper.writeValueAsString(licenseData);
            
            // AES加密数据
            String encryptedData = cryptoUtil.encryptAES(jsonData, aesKey);
            
            // RSA加密AES密钥
            PublicKey publicKey = keyManagerService.getRSAPublicKey(rsaKeyId);
            String encryptedAESKey = cryptoUtil.encryptRSA(cryptoUtil.aesKeyToString(aesKey), publicKey);
            
            // 创建签名数据
            String signatureData = encryptedData + "|" + encryptedAESKey + "|" + rsaKeyId;
            PrivateKey privateKey = keyManagerService.getRSAPrivateKey(rsaKeyId);
            String signature = cryptoUtil.signRSA(signatureData, privateKey);
            
            // 创建加密授权结构
            EncryptedLicense encryptedLicense = new EncryptedLicense();
            encryptedLicense.setEncryptedData(encryptedData);
            encryptedLicense.setSignature(signature);
            encryptedLicense.setKeyId(rsaKeyId);
            encryptedLicense.setAlgorithm("AES-256-GCM+RSA-4096");
            encryptedLicense.setCreatedAt(LocalDateTime.now());
            encryptedLicense.setVersion("1.0");
            
            // 创建最终的授权码格式
            Map<String, Object> licensePackage = new HashMap<>();
            licensePackage.put("encryptedLicense", encryptedLicense);
            licensePackage.put("encryptedKey", encryptedAESKey);
            licensePackage.put("metadata", createLicenseMetadata(licenseData));
            
            String finalLicense = objectMapper.writeValueAsString(licensePackage);
            String base64License = Base64.getEncoder().encodeToString(finalLicense.getBytes());
            
            logger.info("授权码加密完成，授权ID: {}, 使用密钥: {}", licenseData.getLicenseId(), rsaKeyId);
            return formatLicenseKey(base64License);
            
        } catch (Exception e) {
            logger.error("加密授权数据失败，授权ID: {}", licenseData.getLicenseId(), e);
            throw new RuntimeException("授权码加密失败", e);
        }
    }

    /**
     * 解密授权数据
     */
    public LicenseData decryptLicense(String encryptedLicenseKey, String rsaKeyId) {
        try {
            // 解析授权码格式
            String base64License = unformatLicenseKey(encryptedLicenseKey);
            String jsonLicense = new String(Base64.getDecoder().decode(base64License));
            
            Map<String, Object> licensePackage = objectMapper.readValue(jsonLicense, Map.class);
            Map<String, Object> encryptedLicenseMap = (Map<String, Object>) licensePackage.get("encryptedLicense");
            String encryptedAESKey = (String) licensePackage.get("encryptedKey");
            
            EncryptedLicense encryptedLicense = objectMapper.convertValue(encryptedLicenseMap, EncryptedLicense.class);
            
            // 验证签名
            String signatureData = encryptedLicense.getEncryptedData() + "|" + encryptedAESKey + "|" + rsaKeyId;
            PublicKey publicKey = keyManagerService.getRSAPublicKey(rsaKeyId);
            boolean signatureValid = cryptoUtil.verifyRSA(signatureData, encryptedLicense.getSignature(), publicKey);
            
            if (!signatureValid) {
                throw new SecurityException("授权码签名验证失败");
            }
            
            // 解密AES密钥
            PrivateKey privateKey = keyManagerService.getRSAPrivateKey(rsaKeyId);
            String aesKeyStr = cryptoUtil.decryptRSA(encryptedAESKey, privateKey);
            SecretKey aesKey = cryptoUtil.getAESKeyFromString(aesKeyStr);
            
            // 解密授权数据
            String decryptedData = cryptoUtil.decryptAES(encryptedLicense.getEncryptedData(), aesKey);
            LicenseData licenseData = objectMapper.readValue(decryptedData, LicenseData.class);
            
            // 验证数据完整性
            String dataForChecksum = createChecksumData(licenseData);
            String expectedChecksum = hashUtil.sha256(dataForChecksum);
            
            if (!expectedChecksum.equals(licenseData.getChecksum())) {
                throw new SecurityException("授权数据完整性验证失败");
            }
            
            logger.info("授权码解密成功，授权ID: {}", licenseData.getLicenseId());
            return licenseData;
            
        } catch (Exception e) {
            logger.error("解密授权数据失败", e);
            throw new RuntimeException("授权码解密失败", e);
        }
    }

    /**
     * 绑定硬件指纹
     */
    public String bindHardwareFingerprint(String encryptedLicenseKey, String rsaKeyId) {
        try {
            // 解密授权数据
            LicenseData licenseData = decryptLicense(encryptedLicenseKey, rsaKeyId);
            
            // 获取当前硬件指纹
            String currentFingerprint = hardwareFingerprintUtil.generateHardwareFingerprint();
            licenseData.setHardwareFingerprint(currentFingerprint);
            
            // 重新计算校验和
            String dataForChecksum = createChecksumData(licenseData);
            licenseData.setChecksum(hashUtil.sha256(dataForChecksum));
            
            // 重新加密
            String boundLicense = encryptLicense(licenseData, rsaKeyId);
            
            logger.info("硬件指纹绑定完成，授权ID: {}, 指纹: {}", 
                licenseData.getLicenseId(), currentFingerprint.substring(0, 8) + "...");
            
            return boundLicense;
            
        } catch (Exception e) {
            logger.error("硬件指纹绑定失败", e);
            throw new RuntimeException("硬件指纹绑定失败", e);
        }
    }

    /**
     * 验证授权码格式
     */
    public boolean validateLicenseFormat(String licenseKey) {
        try {
            String base64License = unformatLicenseKey(licenseKey);
            String jsonLicense = new String(Base64.getDecoder().decode(base64License));
            
            Map<String, Object> licensePackage = objectMapper.readValue(jsonLicense, Map.class);
            
            return licensePackage.containsKey("encryptedLicense") &&
                   licensePackage.containsKey("encryptedKey") &&
                   licensePackage.containsKey("metadata");
                   
        } catch (Exception e) {
            logger.debug("授权码格式验证失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取授权码元数据（不解密敏感数据）
     */
    public Map<String, Object> getLicenseMetadata(String licenseKey) {
        try {
            String base64License = unformatLicenseKey(licenseKey);
            String jsonLicense = new String(Base64.getDecoder().decode(base64License));
            
            Map<String, Object> licensePackage = objectMapper.readValue(jsonLicense, Map.class);
            return (Map<String, Object>) licensePackage.get("metadata");
            
        } catch (Exception e) {
            logger.error("获取授权码元数据失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 生成授权ID
     */
    private String generateLicenseId() {
        return "LIC_" + secureRandomUtil.generateUUIDWithoutHyphens().substring(0, 16).toUpperCase();
    }

    /**
     * 创建校验和数据
     */
    private String createChecksumData(LicenseData licenseData) {
        StringBuilder sb = new StringBuilder();
        sb.append(licenseData.getLicenseId());
        sb.append(licenseData.getCustomerId());
        sb.append(licenseData.getProductName());
        sb.append(licenseData.getProductVersion());
        sb.append(licenseData.getIssuedAt().toEpochSecond(ZoneOffset.UTC));
        sb.append(licenseData.getExpiresAt().toEpochSecond(ZoneOffset.UTC));
        sb.append(licenseData.getHardwareFingerprint());
        sb.append(licenseData.getIssuer());
        
        // 添加features和restrictions的哈希
        if (licenseData.getFeatures() != null) {
            sb.append(hashUtil.sha256(licenseData.getFeatures().toString()));
        }
        if (licenseData.getRestrictions() != null) {
            sb.append(hashUtil.sha256(licenseData.getRestrictions().toString()));
        }
        
        return sb.toString();
    }

    /**
     * 创建授权元数据
     */
    private Map<String, Object> createLicenseMetadata(LicenseData licenseData) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("licenseId", licenseData.getLicenseId());
        metadata.put("productName", licenseData.getProductName());
        metadata.put("productVersion", licenseData.getProductVersion());
        metadata.put("issuedAt", licenseData.getIssuedAt());
        metadata.put("expiresAt", licenseData.getExpiresAt());
        metadata.put("issuer", licenseData.getIssuer());
        metadata.put("hasHardwareBinding", !licenseData.getHardwareFingerprint().isEmpty());
        return metadata;
    }

    /**
     * 格式化授权码（添加分隔符）
     */
    private String formatLicenseKey(String base64License) {
        // 每64个字符添加一个换行符，便于显示和复制
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < base64License.length(); i += 64) {
            if (i > 0) {
                formatted.append("\n");
            }
            formatted.append(base64License.substring(i, Math.min(i + 64, base64License.length())));
        }
        return formatted.toString();
    }

    /**
     * 反格式化授权码（移除分隔符）
     */
    private String unformatLicenseKey(String formattedLicenseKey) {
        return formattedLicenseKey.replaceAll("\\s+", "");
    }
}