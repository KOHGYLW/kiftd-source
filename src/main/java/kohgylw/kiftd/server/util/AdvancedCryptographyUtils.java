package kohgylw.kiftd.server.util;

import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/**
 * 企业级加密解密工具类
 * 提供AES-256-GCM对称加密和RSA-4096非对称加密功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Component
public class AdvancedCryptographyUtils {
    
    // AES配置
    private static final String AES_ALGORITHM = "AES";
    private static final String AES_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int AES_KEY_LENGTH = 256;
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    
    // RSA配置
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_TRANSFORMATION = "RSA/ECB/OAEPWITHSHA-256ANDMGF1PADDING";
    private static final String RSA_SIGNATURE_ALGORITHM = "SHA256withRSA";
    private static final int RSA_KEY_LENGTH = 4096;
    
    // 哈希算法
    private static final String HASH_ALGORITHM = "SHA-256";
    
    private final SecureRandom secureRandom = new SecureRandom();
    private final Base64.Encoder encoder = Base64.getEncoder();
    private final Base64.Decoder decoder = Base64.getDecoder();
    
    // ==================== AES加密相关方法 ====================
    
    /**
     * 生成AES-256密钥
     * @return String Base64编码的密钥
     * @throws Exception 生成异常
     */
    public String generateAESKey() throws Exception {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_LENGTH, secureRandom);
        SecretKey secretKey = keyGenerator.generateKey();
        return encoder.encodeToString(secretKey.getEncoded());
    }
    
    /**
     * AES-256-GCM加密
     * @param plaintext 明文
     * @param base64Key Base64编码的密钥
     * @return String Base64编码的密文（包含IV）
     * @throws Exception 加密异常
     */
    public String encryptAES(String plaintext, String base64Key) throws Exception {
        if (plaintext == null || base64Key == null) {
            throw new IllegalArgumentException("Plaintext and key cannot be null");
        }
        
        byte[] keyBytes = decoder.decode(base64Key);
        SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        secureRandom.nextBytes(iv);
        
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // 组合IV和密文
        byte[] encryptedData = new byte[GCM_IV_LENGTH + ciphertext.length];
        System.arraycopy(iv, 0, encryptedData, 0, GCM_IV_LENGTH);
        System.arraycopy(ciphertext, 0, encryptedData, GCM_IV_LENGTH, ciphertext.length);
        
        return encoder.encodeToString(encryptedData);
    }
    
    /**
     * AES-256-GCM解密
     * @param ciphertext Base64编码的密文（包含IV）
     * @param base64Key Base64编码的密钥
     * @return String 明文
     * @throws Exception 解密异常
     */
    public String decryptAES(String ciphertext, String base64Key) throws Exception {
        if (ciphertext == null || base64Key == null) {
            throw new IllegalArgumentException("Ciphertext and key cannot be null");
        }
        
        byte[] encryptedData = decoder.decode(ciphertext);
        if (encryptedData.length < GCM_IV_LENGTH + GCM_TAG_LENGTH) {
            throw new IllegalArgumentException("Invalid encrypted data length");
        }
        
        // 提取IV和密文
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] ciphertextBytes = new byte[encryptedData.length - GCM_IV_LENGTH];
        System.arraycopy(encryptedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, GCM_IV_LENGTH, ciphertextBytes, 0, ciphertextBytes.length);
        
        byte[] keyBytes = decoder.decode(base64Key);
        SecretKey secretKey = new SecretKeySpec(keyBytes, AES_ALGORITHM);
        
        Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
        GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);
        
        byte[] plaintext = cipher.doFinal(ciphertextBytes);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    // ==================== RSA加密相关方法 ====================
    
    /**
     * 生成RSA-4096密钥对
     * @return KeyPair 密钥对
     * @throws Exception 生成异常
     */
    public KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_LENGTH, secureRandom);
        return keyPairGenerator.generateKeyPair();
    }
    
    /**
     * RSA公钥加密
     * @param plaintext 明文
     * @param base64PublicKey Base64编码的公钥
     * @return String Base64编码的密文
     * @throws Exception 加密异常
     */
    public String encryptRSA(String plaintext, String base64PublicKey) throws Exception {
        if (plaintext == null || base64PublicKey == null) {
            throw new IllegalArgumentException("Plaintext and public key cannot be null");
        }
        
        byte[] publicKeyBytes = decoder.decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return encoder.encodeToString(ciphertext);
    }
    
    /**
     * RSA私钥解密
     * @param ciphertext Base64编码的密文
     * @param base64PrivateKey Base64编码的私钥
     * @return String 明文
     * @throws Exception 解密异常
     */
    public String decryptRSA(String ciphertext, String base64PrivateKey) throws Exception {
        if (ciphertext == null || base64PrivateKey == null) {
            throw new IllegalArgumentException("Ciphertext and private key cannot be null");
        }
        
        byte[] privateKeyBytes = decoder.decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        
        Cipher cipher = Cipher.getInstance(RSA_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        
        byte[] ciphertextBytes = decoder.decode(ciphertext);
        byte[] plaintext = cipher.doFinal(ciphertextBytes);
        return new String(plaintext, StandardCharsets.UTF_8);
    }
    
    // ==================== 数字签名相关方法 ====================
    
    /**
     * RSA数字签名
     * @param data 要签名的数据
     * @param base64PrivateKey Base64编码的私钥
     * @return String Base64编码的签名
     * @throws Exception 签名异常
     */
    public String signRSA(String data, String base64PrivateKey) throws Exception {
        if (data == null || base64PrivateKey == null) {
            throw new IllegalArgumentException("Data and private key cannot be null");
        }
        
        byte[] privateKeyBytes = decoder.decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PrivateKey privateKey = keyFactory.generatePrivate(keySpec);
        
        Signature signature = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] signatureBytes = signature.sign();
        return encoder.encodeToString(signatureBytes);
    }
    
    /**
     * RSA数字签名验证
     * @param data 原始数据
     * @param signature Base64编码的签名
     * @param base64PublicKey Base64编码的公钥
     * @return boolean 签名是否有效
     * @throws Exception 验证异常
     */
    public boolean verifyRSA(String data, String signature, String base64PublicKey) throws Exception {
        if (data == null || signature == null || base64PublicKey == null) {
            throw new IllegalArgumentException("Data, signature and public key cannot be null");
        }
        
        byte[] publicKeyBytes = decoder.decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(publicKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PublicKey publicKey = keyFactory.generatePublic(keySpec);
        
        Signature sig = Signature.getInstance(RSA_SIGNATURE_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] signatureBytes = decoder.decode(signature);
        return sig.verify(signatureBytes);
    }
    
    // ==================== 哈希相关方法 ====================
    
    /**
     * 计算SHA-256哈希值
     * @param data 原始数据
     * @return String 十六进制哈希值
     * @throws Exception 计算异常
     */
    public String hashSHA256(String data) throws Exception {
        if (data == null) {
            throw new IllegalArgumentException("Data cannot be null");
        }
        
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        
        StringBuilder hexString = new StringBuilder();
        for (byte b : hashBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
    
    /**
     * 验证数据完整性
     * @param data 数据
     * @param expectedHash 期望的哈希值
     * @return boolean 数据是否完整
     * @throws Exception 验证异常
     */
    public boolean verifyIntegrity(String data, String expectedHash) throws Exception {
        String actualHash = hashSHA256(data);
        return actualHash.equalsIgnoreCase(expectedHash);
    }
    
    // ==================== 混合加密相关方法 ====================
    
    /**
     * 混合加密（RSA+AES）
     * 使用RSA加密AES密钥，使用AES加密数据
     * @param plaintext 明文
     * @param base64PublicKey RSA公钥
     * @return HybridEncryptionResult 混合加密结果
     * @throws Exception 加密异常
     */
    public HybridEncryptionResult hybridEncrypt(String plaintext, String base64PublicKey) throws Exception {
        // 生成AES密钥
        String aesKey = generateAESKey();
        
        // 使用AES加密数据
        String encryptedData = encryptAES(plaintext, aesKey);
        
        // 使用RSA加密AES密钥
        String encryptedKey = encryptRSA(aesKey, base64PublicKey);
        
        return new HybridEncryptionResult(encryptedData, encryptedKey);
    }
    
    /**
     * 混合解密（RSA+AES）
     * @param encryptedData AES加密的数据
     * @param encryptedKey RSA加密的AES密钥
     * @param base64PrivateKey RSA私钥
     * @return String 明文
     * @throws Exception 解密异常
     */
    public String hybridDecrypt(String encryptedData, String encryptedKey, String base64PrivateKey) throws Exception {
        // 使用RSA解密AES密钥
        String aesKey = decryptRSA(encryptedKey, base64PrivateKey);
        
        // 使用AES解密数据
        return decryptAES(encryptedData, aesKey);
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 密钥格式转换：将KeyPair转换为Base64字符串
     * @param keyPair 密钥对
     * @return KeyPairStrings Base64编码的密钥对
     */
    public KeyPairStrings keyPairToStrings(KeyPair keyPair) {
        String publicKey = encoder.encodeToString(keyPair.getPublic().getEncoded());
        String privateKey = encoder.encodeToString(keyPair.getPrivate().getEncoded());
        return new KeyPairStrings(publicKey, privateKey);
    }
    
    /**
     * 生成安全随机数
     * @param length 长度（字节）
     * @return String Base64编码的随机数
     */
    public String generateSecureRandom(int length) {
        byte[] randomBytes = new byte[length];
        secureRandom.nextBytes(randomBytes);
        return encoder.encodeToString(randomBytes);
    }
    
    /**
     * 验证密钥强度
     * @param base64Key Base64编码的密钥
     * @param expectedLength 期望的密钥长度（字节）
     * @return boolean 密钥是否满足强度要求
     */
    public boolean validateKeyStrength(String base64Key, int expectedLength) {
        try {
            byte[] keyBytes = decoder.decode(base64Key);
            return keyBytes.length >= expectedLength;
        } catch (Exception e) {
            return false;
        }
    }
    
    // ==================== 内部类 ====================
    
    /**
     * 混合加密结果
     */
    public static class HybridEncryptionResult {
        private final String encryptedData;
        private final String encryptedKey;
        
        public HybridEncryptionResult(String encryptedData, String encryptedKey) {
            this.encryptedData = encryptedData;
            this.encryptedKey = encryptedKey;
        }
        
        public String getEncryptedData() {
            return encryptedData;
        }
        
        public String getEncryptedKey() {
            return encryptedKey;
        }
    }
    
    /**
     * 密钥对字符串表示
     */
    public static class KeyPairStrings {
        private final String publicKey;
        private final String privateKey;
        
        public KeyPairStrings(String publicKey, String privateKey) {
            this.publicKey = publicKey;
            this.privateKey = privateKey;
        }
        
        public String getPublicKey() {
            return publicKey;
        }
        
        public String getPrivateKey() {
            return privateKey;
        }
    }
}