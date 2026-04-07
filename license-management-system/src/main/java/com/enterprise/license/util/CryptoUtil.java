package com.enterprise.license.util;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
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
 * 提供AES-256-GCM对称加密和RSA-4096非对称加密
 */
@Component
public class CryptoUtil {

    private static final String AES_ALGORITHM = "AES";
    private static final String AES_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String RSA_ALGORITHM = "RSA";
    private static final String RSA_CIPHER_ALGORITHM = "RSA/OAEP/SHA-256";
    private static final int GCM_IV_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 16;
    private static final int AES_KEY_LENGTH = 256;
    private static final int RSA_KEY_LENGTH = 4096;

    static {
        // 添加BouncyCastle安全提供者
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    /**
     * 生成AES-256密钥
     */
    public SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(AES_ALGORITHM);
        keyGenerator.init(AES_KEY_LENGTH);
        return keyGenerator.generateKey();
    }

    /**
     * 生成RSA-4096密钥对
     */
    public KeyPair generateRSAKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        keyPairGenerator.initialize(RSA_KEY_LENGTH, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    /**
     * AES-GCM加密
     */
    public String encryptAES(String plaintext, SecretKey key) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        
        // 生成随机IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        new SecureRandom().nextBytes(iv);
        
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, gcmParameterSpec);
        
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        
        // 将IV和加密数据合并
        byte[] encryptedWithIv = new byte[GCM_IV_LENGTH + encryptedData.length];
        System.arraycopy(iv, 0, encryptedWithIv, 0, GCM_IV_LENGTH);
        System.arraycopy(encryptedData, 0, encryptedWithIv, GCM_IV_LENGTH, encryptedData.length);
        
        return Base64.getEncoder().encodeToString(encryptedWithIv);
    }

    /**
     * AES-GCM解密
     */
    public String decryptAES(String encryptedData, SecretKey key) throws Exception {
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        
        // 提取IV和加密数据
        byte[] iv = new byte[GCM_IV_LENGTH];
        byte[] encrypted = new byte[decodedData.length - GCM_IV_LENGTH];
        
        System.arraycopy(decodedData, 0, iv, 0, GCM_IV_LENGTH);
        System.arraycopy(decodedData, GCM_IV_LENGTH, encrypted, 0, encrypted.length);
        
        Cipher cipher = Cipher.getInstance(AES_CIPHER_ALGORITHM);
        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
        cipher.init(Cipher.DECRYPT_MODE, key, gcmParameterSpec);
        
        byte[] decryptedData = cipher.doFinal(encrypted);
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * RSA公钥加密
     */
    public String encryptRSA(String plaintext, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        
        byte[] encryptedData = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encryptedData);
    }

    /**
     * RSA私钥解密
     */
    public String decryptRSA(String encryptedData, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_CIPHER_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        
        byte[] decodedData = Base64.getDecoder().decode(encryptedData);
        byte[] decryptedData = cipher.doFinal(decodedData);
        
        return new String(decryptedData, StandardCharsets.UTF_8);
    }

    /**
     * 从Base64字符串恢复公钥
     */
    public PublicKey getPublicKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePublic(spec);
    }

    /**
     * 从Base64字符串恢复私钥
     */
    public PrivateKey getPrivateKeyFromString(String key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        return keyFactory.generatePrivate(spec);
    }

    /**
     * 从Base64字符串恢复AES密钥
     */
    public SecretKey getAESKeyFromString(String key) {
        byte[] keyBytes = Base64.getDecoder().decode(key);
        return new SecretKeySpec(keyBytes, AES_ALGORITHM);
    }

    /**
     * 将公钥转换为Base64字符串
     */
    public String publicKeyToString(PublicKey publicKey) {
        return Base64.getEncoder().encodeToString(publicKey.getEncoded());
    }

    /**
     * 将私钥转换为Base64字符串
     */
    public String privateKeyToString(PrivateKey privateKey) {
        return Base64.getEncoder().encodeToString(privateKey.getEncoded());
    }

    /**
     * 将AES密钥转换为Base64字符串
     */
    public String aesKeyToString(SecretKey secretKey) {
        return Base64.getEncoder().encodeToString(secretKey.getEncoded());
    }

    /**
     * RSA数字签名
     */
    public String signRSA(String data, PrivateKey privateKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] signatureBytes = signature.sign();
        return Base64.getEncoder().encodeToString(signatureBytes);
    }

    /**
     * RSA签名验证
     */
    public boolean verifyRSA(String data, String signatureStr, PublicKey publicKey) throws Exception {
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initVerify(publicKey);
        signature.update(data.getBytes(StandardCharsets.UTF_8));
        
        byte[] signatureBytes = Base64.getDecoder().decode(signatureStr);
        return signature.verify(signatureBytes);
    }

    /**
     * 生成安全随机字节数组
     */
    public byte[] generateRandomBytes(int length) {
        byte[] randomBytes = new byte[length];
        new SecureRandom().nextBytes(randomBytes);
        return randomBytes;
    }

    /**
     * 生成安全随机字符串
     */
    public String generateRandomString(int length) {
        byte[] randomBytes = generateRandomBytes(length);
        return Base64.getEncoder().encodeToString(randomBytes).substring(0, length);
    }
}