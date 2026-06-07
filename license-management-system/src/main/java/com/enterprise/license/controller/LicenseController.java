package com.enterprise.license.controller;

import com.enterprise.license.dto.*;
import com.enterprise.license.entity.License;
import com.enterprise.license.service.*;
import com.enterprise.license.util.HardwareFingerprintUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 授权管理控制器
 */
@RestController
@RequestMapping("/api/licenses")
@Tag(name = "授权管理", description = "授权码生成、验证和管理相关接口")
public class LicenseController {

    private static final Logger logger = LoggerFactory.getLogger(LicenseController.class);

    @Autowired
    private LicenseEncryptionService licenseEncryptionService;

    @Autowired
    private LicenseValidationService licenseValidationService;

    @Autowired
    private RsaKeyGeneratorService rsaKeyGeneratorService;

    @Autowired
    private KeyManagerService keyManagerService;

    @Autowired
    private HardwareFingerprintUtil hardwareFingerprintUtil;

    /**
     * 生成授权码
     */
    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LICENSE_MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "生成授权码", description = "根据客户信息和产品信息生成加密的授权码")
    public ResponseEntity<ApiResponse<LicenseResponseDto>> generateLicense(
            @Valid @RequestBody LicenseRequestDto request) {

        try {
            logger.info("开始生成授权码，客户ID: {}, 产品: {}", request.getCustomerId(), request.getProductName());

            // 1. 生成RSA密钥对
            RsaKeyGeneratorService.KeyPairInfo keyPair = rsaKeyGeneratorService.generateKeyPair();
            String rsaKeyId = keyManagerService.storeRSAKeyPair(keyPair, 8760); // 存储1年

            // 2. 创建授权数据
            LicenseEncryptionService.LicenseData licenseData = licenseEncryptionService.createLicenseData(
                    request.getCustomerId(),
                    request.getProductName(),
                    request.getProductVersion(),
                    request.getValidityDays(),
                    request.getFeatures(),
                    request.getRestrictions()
            );

            // 3. 如果需要绑定硬件，设置硬件指纹
            if (Boolean.TRUE.equals(request.getBindHardware())) {
                String hardwareFingerprint = request.getHardwareFingerprint();
                if (hardwareFingerprint == null || hardwareFingerprint.trim().isEmpty()) {
                    // 如果未提供硬件指纹，使用当前系统的指纹
                    hardwareFingerprint = hardwareFingerprintUtil.generateHardwareFingerprint();
                }
                licenseData.setHardwareFingerprint(hardwareFingerprint);
            }

            // 4. 加密授权数据
            String encryptedLicense = licenseEncryptionService.encryptLicense(licenseData, rsaKeyId);

            // 5. 构建响应
            LicenseResponseDto response = new LicenseResponseDto();
            response.setLicenseId(licenseData.getLicenseId());
            response.setCustomerId(licenseData.getCustomerId());
            response.setProductName(licenseData.getProductName());
            response.setProductVersion(licenseData.getProductVersion());
            response.setLicenseType(request.getLicenseType());
            response.setLicenseStatus(License.LicenseStatus.ACTIVE);
            response.setIssuedAt(licenseData.getIssuedAt());
            response.setExpiresAt(licenseData.getExpiresAt());
            response.setEncryptedLicense(encryptedLicense);
            response.setRsaKeyId(rsaKeyId);
            response.setIssuer(licenseData.getIssuer());
            response.setChecksum(licenseData.getChecksum());
            response.setUsageCount(0);
            response.setMaxUsageCount(request.getMaxUsageCount());
            response.setHardwareBound(!licenseData.getHardwareFingerprint().isEmpty());
            response.setRemainingDays(licenseData.getExpiresAt().toLocalDate().toEpochDay() - 
                                    licenseData.getIssuedAt().toLocalDate().toEpochDay());
            response.setNotes(request.getNotes());

            logger.info("授权码生成成功，授权ID: {}", licenseData.getLicenseId());
            return ResponseEntity.ok(ApiResponse.success(response, "授权码生成成功"));

        } catch (Exception e) {
            logger.error("授权码生成失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("授权码生成失败: " + e.getMessage()));
        }
    }

    /**
     * 在线验证授权码
     */
    @PostMapping("/validate")
    @Operation(summary = "在线验证授权码", description = "验证授权码的有效性，支持完整的在线验证")
    public ResponseEntity<ApiResponse<LicenseValidationResponseDto>> validateLicense(
            @Valid @RequestBody LicenseValidationRequestDto request) {

        try {
            logger.info("开始在线验证授权码，产品: {}", request.getProductName());

            // 确定使用的RSA密钥ID
            String rsaKeyId = request.getRsaKeyId();
            if (rsaKeyId == null || rsaKeyId.trim().isEmpty()) {
                // 如果未指定密钥ID，可以从授权码元数据中获取
                Map<String, Object> metadata = licenseEncryptionService.getLicenseMetadata(request.getLicenseKey());
                // 这里需要根据实际实现调整
                rsaKeyId = "DEFAULT_KEY"; // 临时方案
            }

            // 执行在线验证
            LicenseValidationService.ValidationDetails details = licenseValidationService.validateLicenseOnline(
                    request.getLicenseKey(),
                    rsaKeyId,
                    request.getProductName(),
                    request.getProductVersion()
            );

            // 构建响应
            LicenseValidationResponseDto response = new LicenseValidationResponseDto(
                    details.getResult(), details.getMessage());

            if (details.getLicenseData() != null) {
                LicenseEncryptionService.LicenseData licenseData = details.getLicenseData();
                response.setLicenseId(licenseData.getLicenseId());
                response.setCustomerId(licenseData.getCustomerId());
                response.setProductName(licenseData.getProductName());
                response.setProductVersion(licenseData.getProductVersion());
                response.setIssuedAt(licenseData.getIssuedAt());
                response.setExpiresAt(licenseData.getExpiresAt());
                response.setFeatures(licenseData.getFeatures());
                response.setRestrictions(licenseData.getRestrictions());
                response.setHardwareBound(!licenseData.getHardwareFingerprint().isEmpty());
            }

            response.setRemainingDays(details.getRemainingDays());
            response.setHardwareSimilarity(details.getHardwareSimilarity());
            response.setValidationType("ONLINE");
            response.setMetadata(details.getMetadata());

            // 如果需要验证特定功能
            if (request.getFeatureName() != null && details.getLicenseData() != null) {
                boolean featureAllowed = licenseValidationService.validateFeature(
                        details.getLicenseData(), request.getFeatureName());
                response.getMetadata().put("featureAllowed", featureAllowed);
            }

            logger.info("授权码在线验证完成，结果: {}", details.getResult());
            return ResponseEntity.ok(ApiResponse.success(response, "授权码验证完成"));

        } catch (Exception e) {
            logger.error("授权码在线验证失败", e);
            LicenseValidationResponseDto errorResponse = new LicenseValidationResponseDto(
                    LicenseValidationService.ValidationResult.UNKNOWN_ERROR, 
                    "验证过程中发生错误: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorResponse, "授权码验证失败"));
        }
    }

    /**
     * 离线验证授权码
     */
    @PostMapping("/validate-offline")
    @Operation(summary = "离线验证授权码", description = "基础的离线授权验证，不依赖网络连接")
    public ResponseEntity<ApiResponse<LicenseValidationResponseDto>> validateLicenseOffline(
            @Valid @RequestBody LicenseValidationRequestDto request) {

        try {
            logger.info("开始离线验证授权码，产品: {}", request.getProductName());

            // 确定使用的RSA密钥ID
            String rsaKeyId = request.getRsaKeyId();
            if (rsaKeyId == null || rsaKeyId.trim().isEmpty()) {
                rsaKeyId = "DEFAULT_KEY"; // 临时方案
            }

            // 执行离线验证
            LicenseValidationService.ValidationDetails details = licenseValidationService.validateLicenseOffline(
                    request.getLicenseKey(),
                    rsaKeyId,
                    request.getProductName(),
                    request.getProductVersion()
            );

            // 构建响应
            LicenseValidationResponseDto response = new LicenseValidationResponseDto(
                    details.getResult(), details.getMessage());

            if (details.getLicenseData() != null) {
                LicenseEncryptionService.LicenseData licenseData = details.getLicenseData();
                response.setLicenseId(licenseData.getLicenseId());
                response.setCustomerId(licenseData.getCustomerId());
                response.setProductName(licenseData.getProductName());
                response.setProductVersion(licenseData.getProductVersion());
                response.setIssuedAt(licenseData.getIssuedAt());
                response.setExpiresAt(licenseData.getExpiresAt());
                response.setFeatures(licenseData.getFeatures());
                response.setRestrictions(licenseData.getRestrictions());
                response.setHardwareBound(!licenseData.getHardwareFingerprint().isEmpty());
            }

            response.setRemainingDays(details.getRemainingDays());
            response.setHardwareSimilarity(details.getHardwareSimilarity());
            response.setValidationType("OFFLINE");
            response.setMetadata(details.getMetadata());

            logger.info("授权码离线验证完成，结果: {}", details.getResult());
            return ResponseEntity.ok(ApiResponse.success(response, "授权码验证完成"));

        } catch (Exception e) {
            logger.error("授权码离线验证失败", e);
            LicenseValidationResponseDto errorResponse = new LicenseValidationResponseDto(
                    LicenseValidationService.ValidationResult.UNKNOWN_ERROR, 
                    "验证过程中发生错误: " + e.getMessage());
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(errorResponse, "授权码验证失败"));
        }
    }

    /**
     * 绑定硬件指纹
     */
    @PostMapping("/bind-hardware")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LICENSE_MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "绑定硬件指纹", description = "将授权码与当前硬件设备绑定")
    public ResponseEntity<ApiResponse<Map<String, Object>>> bindHardware(
            @RequestBody Map<String, String> request) {

        try {
            String licenseKey = request.get("licenseKey");
            String rsaKeyId = request.get("rsaKeyId");

            if (licenseKey == null || licenseKey.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("授权码不能为空"));
            }

            if (rsaKeyId == null || rsaKeyId.trim().isEmpty()) {
                rsaKeyId = "DEFAULT_KEY"; // 临时方案
            }

            logger.info("开始绑定硬件指纹");

            String boundLicense = licenseEncryptionService.bindHardwareFingerprint(licenseKey, rsaKeyId);
            String currentFingerprint = hardwareFingerprintUtil.generateHardwareFingerprint();

            Map<String, Object> responseData = Map.of(
                    "boundLicense", boundLicense,
                    "hardwareFingerprint", currentFingerprint.substring(0, 8) + "...",
                    "bindTime", java.time.LocalDateTime.now()
            );

            logger.info("硬件指纹绑定成功");
            return ResponseEntity.ok(ApiResponse.success(responseData, "硬件指纹绑定成功"));

        } catch (Exception e) {
            logger.error("硬件指纹绑定失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("硬件指纹绑定失败: " + e.getMessage()));
        }
    }

    /**
     * 吊销授权码
     */
    @PostMapping("/revoke")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LICENSE_MANAGER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "吊销授权码", description = "吊销指定的授权码，使其失效")
    public ResponseEntity<ApiResponse<Void>> revokeLicense(
            @RequestBody Map<String, String> request) {

        try {
            String licenseId = request.get("licenseId");
            String reason = request.get("reason");

            if (licenseId == null || licenseId.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("授权ID不能为空"));
            }

            if (reason == null || reason.trim().isEmpty()) {
                reason = "管理员吊销";
            }

            logger.info("开始吊销授权码，授权ID: {}, 原因: {}", licenseId, reason);

            licenseValidationService.revokeLicense(licenseId, reason);

            logger.info("授权码吊销成功，授权ID: {}", licenseId);
            return ResponseEntity.ok(ApiResponse.success(null, "授权码吊销成功"));

        } catch (Exception e) {
            logger.error("授权码吊销失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("授权码吊销失败: " + e.getMessage()));
        }
    }

    /**
     * 获取当前硬件指纹
     */
    @GetMapping("/hardware-fingerprint")
    @Operation(summary = "获取硬件指纹", description = "获取当前设备的硬件指纹信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getHardwareFingerprint() {
        try {
            logger.info("获取硬件指纹请求");

            HardwareFingerprintUtil.HardwareInfo hardwareInfo = hardwareFingerprintUtil.getHardwareInfo();
            String fingerprint = hardwareFingerprintUtil.generateHardwareFingerprint();
            String deviceId = hardwareFingerprintUtil.generateDeviceId();

            Map<String, Object> responseData = Map.of(
                    "fingerprint", fingerprint,
                    "deviceId", deviceId,
                    "cpuId", hardwareInfo.getCpuId() != null ? hardwareInfo.getCpuId().substring(0, Math.min(16, hardwareInfo.getCpuId().length())) + "..." : "N/A",
                    "macAddress", hardwareInfo.getMacAddress() != null ? hardwareInfo.getMacAddress().substring(0, Math.min(12, hardwareInfo.getMacAddress().length())) + "..." : "N/A",
                    "hostname", hardwareInfo.getHostname(),
                    "osInfo", hardwareInfo.getOsInfo()
            );

            logger.info("硬件指纹获取成功");
            return ResponseEntity.ok(ApiResponse.success(responseData, "硬件指纹获取成功"));

        } catch (Exception e) {
            logger.error("获取硬件指纹失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取硬件指纹失败: " + e.getMessage()));
        }
    }
}