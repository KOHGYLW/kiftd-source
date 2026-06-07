package com.enterprise.license.service;

import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
import com.enterprise.license.exception.LicenseException;
import com.enterprise.license.util.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LicenseEncryptionService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LicenseEncryptionService 单元测试")
class LicenseEncryptionServiceTest {

    @Mock
    private KeyManagerService keyManagerService;

    @InjectMocks
    private LicenseEncryptionService licenseEncryptionService;

    private License testLicense;
    private Customer testCustomer;
    private KeyPair testKeyPair;

    @BeforeEach
    void setUp() throws Exception {
        // 初始化测试客户
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setCustomerCode("TEST001");
        testCustomer.setCustomerName("测试客户");
        testCustomer.setStatus(CustomerStatus.ACTIVE);

        // 初始化测试许可证
        testLicense = new License();
        testLicense.setId(1L);
        testLicense.setLicenseCode("LIC001");
        testLicense.setCustomer(testCustomer);
        testLicense.setLicenseType(LicenseType.STANDARD);
        testLicense.setProductName("测试产品");
        testLicense.setProductVersion("1.0.0");
        testLicense.setStatus(LicenseStatus.ACTIVE);
        testLicense.setMaxUsers(100);
        testLicense.setCurrentUsers(0);
        testLicense.setIssuedAt(LocalDateTime.now());
        testLicense.setExpiresAt(LocalDateTime.now().plusDays(365));

        // 生成测试密钥对
        testKeyPair = CryptoUtil.generateRSAKeyPair(2048);
        
        // 设置服务配置
        ReflectionTestUtils.setField(licenseEncryptionService, "rsaKeyLength", 2048);
        ReflectionTestUtils.setField(licenseEncryptionService, "aesKeyLength", 256);
        ReflectionTestUtils.setField(licenseEncryptionService, "signatureAlgorithm", "SHA256withRSA");
    }

    @Nested
    @DisplayName("许可证密钥生成测试")
    class LicenseKeyGenerationTests {

        @Test
        @DisplayName("成功生成许可证密钥")
        void shouldGenerateLicenseKeySuccessfully() {
            // Given
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);

            // When
            String licenseKey = licenseEncryptionService.generateLicenseKey(testLicense);

            // Then
            assertThat(licenseKey).isNotNull();
            assertThat(licenseKey).isNotEmpty();
            assertThat(licenseKey).contains("=="); // Base64编码的标识

            verify(keyManagerService).getCurrentKeyPair();
        }

        @Test
        @DisplayName("许可证密钥包含正确的许可证信息")
        void shouldIncludeCorrectLicenseInfoInKey() {
            // Given
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);

            // When
            String licenseKey = licenseEncryptionService.generateLicenseKey(testLicense);

            // Then
            assertThat(licenseKey).isNotNull();
            
            // 解密并验证包含的信息
            String decryptedData = licenseEncryptionService.decryptLicenseKey(
                    licenseKey, testKeyPair.getPrivate());
            
            assertThat(decryptedData).contains(testLicense.getLicenseCode());
            assertThat(decryptedData).contains(testLicense.getCustomer().getCustomerCode());
            assertThat(decryptedData).contains(testLicense.getProductName());
        }

        @Test
        @DisplayName("密钥管理服务异常时生成许可证密钥失败")
        void shouldFailWhenKeyManagerServiceThrowsException() {
            // Given
            when(keyManagerService.getCurrentKeyPair())
                    .thenThrow(new RuntimeException("密钥服务异常"));

            // When & Then
            assertThatThrownBy(() -> licenseEncryptionService.generateLicenseKey(testLicense))
                    .isInstanceOf(LicenseException.class)
                    .hasMessageContaining("生成许可证密钥失败");
        }

        @Test
        @DisplayName("许可证对象为空时抛出异常")
        void shouldThrowExceptionWhenLicenseIsNull() {
            // When & Then
            assertThatThrownBy(() -> licenseEncryptionService.generateLicenseKey(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("许可证不能为空");
        }
    }

    @Nested
    @DisplayName("许可证密钥签名测试")
    class LicenseKeySignatureTests {

        @Test
        @DisplayName("成功签名许可证密钥")
        void shouldSignLicenseKeySuccessfully() {
            // Given
            String licenseKeyData = "test-license-key-data";
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);

            // When
            String signature = licenseEncryptionService.signLicenseKey(licenseKeyData);

            // Then
            assertThat(signature).isNotNull();
            assertThat(signature).isNotEmpty();
            assertThat(signature).contains("=="); // Base64编码的标识

            verify(keyManagerService).getCurrentKeyPair();
        }

        @Test
        @DisplayName("验证许可证密钥签名成功")
        void shouldVerifyLicenseKeySignatureSuccessfully() {
            // Given
            String licenseKeyData = "test-license-key-data";
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);
            when(keyManagerService.getPublicKey(anyString())).thenReturn(testKeyPair.getPublic());

            String signature = licenseEncryptionService.signLicenseKey(licenseKeyData);

            // When
            boolean isValid = licenseEncryptionService.verifyLicenseKeySignature(
                    licenseKeyData, signature, "test-key-id");

            // Then
            assertThat(isValid).isTrue();

            verify(keyManagerService).getPublicKey("test-key-id");
        }

        @Test
        @DisplayName("篡改数据后签名验证失败")
        void shouldFailVerificationWhenDataIsTampered() {
            // Given
            String originalData = "original-license-key-data";
            String tamperedData = "tampered-license-key-data";
            
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);
            when(keyManagerService.getPublicKey(anyString())).thenReturn(testKeyPair.getPublic());

            String signature = licenseEncryptionService.signLicenseKey(originalData);

            // When
            boolean isValid = licenseEncryptionService.verifyLicenseKeySignature(
                    tamperedData, signature, "test-key-id");

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("使用错误的公钥验证签名失败")
        void shouldFailVerificationWithWrongPublicKey() throws Exception {
            // Given
            String licenseKeyData = "test-license-key-data";
            KeyPair wrongKeyPair = CryptoUtil.generateRSAKeyPair(2048);
            
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);
            when(keyManagerService.getPublicKey(anyString())).thenReturn(wrongKeyPair.getPublic());

            String signature = licenseEncryptionService.signLicenseKey(licenseKeyData);

            // When
            boolean isValid = licenseEncryptionService.verifyLicenseKeySignature(
                    licenseKeyData, signature, "wrong-key-id");

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("许可证密钥解密测试")
    class LicenseKeyDecryptionTests {

        @Test
        @DisplayName("成功解密许可证密钥")
        void shouldDecryptLicenseKeySuccessfully() {
            // Given
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);
            
            String licenseKey = licenseEncryptionService.generateLicenseKey(testLicense);

            // When
            String decryptedData = licenseEncryptionService.decryptLicenseKey(
                    licenseKey, testKeyPair.getPrivate());

            // Then
            assertThat(decryptedData).isNotNull();
            assertThat(decryptedData).isNotEmpty();
            assertThat(decryptedData).contains(testLicense.getLicenseCode());
        }

        @Test
        @DisplayName("使用错误的私钥解密失败")
        void shouldFailDecryptionWithWrongPrivateKey() throws Exception {
            // Given
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);
            
            String licenseKey = licenseEncryptionService.generateLicenseKey(testLicense);
            KeyPair wrongKeyPair = CryptoUtil.generateRSAKeyPair(2048);

            // When & Then
            assertThatThrownBy(() -> licenseEncryptionService.decryptLicenseKey(
                    licenseKey, wrongKeyPair.getPrivate()))
                    .isInstanceOf(LicenseException.class)
                    .hasMessageContaining("解密许可证密钥失败");
        }

        @Test
        @DisplayName("解密非法格式的许可证密钥失败")
        void shouldFailDecryptionWithInvalidFormat() {
            // Given
            String invalidLicenseKey = "invalid-license-key-format";

            // When & Then
            assertThatThrownBy(() -> licenseEncryptionService.decryptLicenseKey(
                    invalidLicenseKey, testKeyPair.getPrivate()))
                    .isInstanceOf(LicenseException.class)
                    .hasMessageContaining("解密许可证密钥失败");
        }
    }

    @Nested
    @DisplayName("硬件指纹处理测试")
    class HardwareFingerprintTests {

        @Test
        @DisplayName("成功加密硬件指纹")
        void shouldEncryptHardwareFingerprintSuccessfully() {
            // Given
            String hardwareFingerprint = "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345";
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);

            // When
            String encryptedFingerprint = licenseEncryptionService
                    .encryptHardwareFingerprint(hardwareFingerprint);

            // Then
            assertThat(encryptedFingerprint).isNotNull();
            assertThat(encryptedFingerprint).isNotEmpty();
            assertThat(encryptedFingerprint).isNotEqualTo(hardwareFingerprint);
        }

        @Test
        @DisplayName("成功解密硬件指纹")
        void shouldDecryptHardwareFingerprintSuccessfully() {
            // Given
            String originalFingerprint = "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345";
            when(keyManagerService.getCurrentKeyPair()).thenReturn(testKeyPair);

            String encryptedFingerprint = licenseEncryptionService
                    .encryptHardwareFingerprint(originalFingerprint);

            // When
            String decryptedFingerprint = licenseEncryptionService
                    .decryptHardwareFingerprint(encryptedFingerprint, testKeyPair.getPrivate());

            // Then
            assertThat(decryptedFingerprint).isEqualTo(originalFingerprint);
        }

        @Test
        @DisplayName("硬件指纹相似度计算正确")
        void shouldCalculateHardwareFingerprintSimilarityCorrectly() {
            // Given
            String fingerprint1 = "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345";
            String fingerprint2 = "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:67890"; // 部分不同
            String fingerprint3 = "MAC:11:22:33:44:55:66|CPU:AMD|DISK:99999";   // 完全不同

            // When
            double similarity1 = licenseEncryptionService
                    .calculateHardwareFingerprintSimilarity(fingerprint1, fingerprint2);
            double similarity2 = licenseEncryptionService
                    .calculateHardwareFingerprintSimilarity(fingerprint1, fingerprint3);
            double similarity3 = licenseEncryptionService
                    .calculateHardwareFingerprintSimilarity(fingerprint1, fingerprint1);

            // Then
            assertThat(similarity1).isBetween(0.6, 0.8); // 部分相似
            assertThat(similarity2).isBetween(0.0, 0.4); // 基本不相似
            assertThat(similarity3).isEqualTo(1.0);      // 完全相同
        }
    }

    @Nested
    @DisplayName("加密工具类测试")
    class CryptoUtilTests {

        @Test
        @DisplayName("RSA密钥对生成测试")
        void shouldGenerateRSAKeyPairSuccessfully() throws Exception {
            // When
            KeyPair keyPair = CryptoUtil.generateRSAKeyPair(2048);

            // Then
            assertThat(keyPair).isNotNull();
            assertThat(keyPair.getPrivate()).isNotNull();
            assertThat(keyPair.getPublic()).isNotNull();
            assertThat(keyPair.getPrivate().getAlgorithm()).isEqualTo("RSA");
            assertThat(keyPair.getPublic().getAlgorithm()).isEqualTo("RSA");
        }

        @Test
        @DisplayName("AES密钥生成测试")
        void shouldGenerateAESKeySuccessfully() {
            // When
            String aesKey = CryptoUtil.generateAESKey(256);

            // Then
            assertThat(aesKey).isNotNull();
            assertThat(aesKey).isNotEmpty();
            assertThat(aesKey.length()).isGreaterThan(32); // Base64编码后的长度
        }

        @Test
        @DisplayName("SHA256哈希计算测试")
        void shouldCalculateSHA256HashCorrectly() {
            // Given
            String input = "test-input-string";

            // When
            String hash1 = CryptoUtil.sha256Hash(input);
            String hash2 = CryptoUtil.sha256Hash(input);
            String hash3 = CryptoUtil.sha256Hash("different-input");

            // Then
            assertThat(hash1).isNotNull();
            assertThat(hash1).isEqualTo(hash2); // 相同输入产生相同哈希
            assertThat(hash1).isNotEqualTo(hash3); // 不同输入产生不同哈希
            assertThat(hash1.length()).isEqualTo(64); // SHA256哈希固定长度
        }

        @Test
        @DisplayName("Base64编码解码测试")
        void shouldEncodeDecodeBase64Correctly() {
            // Given
            String originalData = "test-data-for-base64-encoding";

            // When
            String encoded = CryptoUtil.base64Encode(originalData.getBytes());
            byte[] decoded = CryptoUtil.base64Decode(encoded);
            String decodedString = new String(decoded);

            // Then
            assertThat(encoded).isNotNull();
            assertThat(encoded).isNotEqualTo(originalData);
            assertThat(decodedString).isEqualTo(originalData);
        }
    }
}