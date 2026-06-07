package kohgylw.kiftd.server.service.impl;

import kohgylw.kiftd.server.service.LicenseValidationService;
import kohgylw.kiftd.server.service.LicenseEncryptionService;
import kohgylw.kiftd.server.service.DigitalSignatureService;
import kohgylw.kiftd.server.service.HardwareBindingService;
import kohgylw.kiftd.server.pojo.LicenseData;
import kohgylw.kiftd.server.pojo.HardwareFingerprint;
import kohgylw.kiftd.printer.Printer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 授权验证服务实现
 * 提供完整的授权码验证流程
 * 
 * @author 企业安全专家
 * @version 2.0
 */
@Service
public class LicenseValidationServiceImpl implements LicenseValidationService {
    
    @Autowired
    private LicenseEncryptionService licenseEncryptionService;
    
    @Autowired
    private DigitalSignatureService digitalSignatureService;
    
    @Autowired
    private HardwareBindingService hardwareBindingService;
    
    @Value("${license.validation.strict.mode:true}")
    private boolean strictValidationMode;
    
    @Value("${license.validation.online.enabled:false}")
    private boolean onlineValidationEnabled;
    
    @Value("${license.validation.hardware.binding.enabled:true}")
    private boolean hardwareBindingEnabled;
    
    @Override
    public ValidationResult validateLicense(String licenseString, String encryptionKey, String publicKey) {
        if (onlineValidationEnabled) {
            return validateLicenseOnline(licenseString, encryptionKey, publicKey);
        } else {
            return validateLicenseOffline(licenseString, encryptionKey, publicKey);
        }
    }
    
    @Override
    public ValidationResult validateLicenseOffline(String licenseString, String encryptionKey, String publicKey) {
        try {
            Printer.instance.print("开始离线授权验证...");
            
            // 1. 基本参数验证
            if (licenseString == null || licenseString.trim().isEmpty()) {
                return ValidationResult.failure(ValidationStatus.INVALID_FORMAT, "授权码不能为空");
            }
            
            if (encryptionKey == null || encryptionKey.trim().isEmpty()) {
                return ValidationResult.failure(ValidationStatus.DECRYPTION_FAILED, "解密密钥不能为空");
            }
            
            if (publicKey == null || publicKey.trim().isEmpty()) {
                return ValidationResult.failure(ValidationStatus.SIGNATURE_VERIFICATION_FAILED, "公钥不能为空");
            }
            
            // 2. 解密授权码
            LicenseData licenseData;
            try {
                licenseData = licenseEncryptionService.parseLicenseString(licenseString, encryptionKey);
                if (licenseData == null) {
                    return ValidationResult.failure(ValidationStatus.DECRYPTION_FAILED, "授权码解密失败");
                }
            } catch (Exception e) {
                Printer.instance.print("授权码解密失败: " + e.getMessage());
                return ValidationResult.failure(ValidationStatus.DECRYPTION_FAILED, 
                                               "授权码解密失败: " + e.getMessage());
            }
            
            // 3. 验证数字签名
            if (licenseData.getDigitalSignature() == null || licenseData.getDigitalSignature().isEmpty()) {
                return ValidationResult.failure(ValidationStatus.SIGNATURE_VERIFICATION_FAILED, "授权码缺少数字签名");
            }
            
            try {
                boolean signatureValid = digitalSignatureService.verifyLicense(
                    licenseData, licenseData.getDigitalSignature(), publicKey);
                if (!signatureValid) {
                    return ValidationResult.failure(ValidationStatus.SIGNATURE_VERIFICATION_FAILED, "数字签名验证失败");
                }
            } catch (Exception e) {
                Printer.instance.print("数字签名验证异常: " + e.getMessage());
                return ValidationResult.failure(ValidationStatus.SIGNATURE_VERIFICATION_FAILED, 
                                               "数字签名验证异常: " + e.getMessage());
            }
            
            // 4. 验证授权时效性
            if (!validateExpiration(licenseData)) {
                if (!licenseData.isActive()) {
                    if (licenseData.getStartTime() != null && LocalDateTime.now().isBefore(licenseData.getStartTime())) {
                        return ValidationResult.failure(ValidationStatus.NOT_YET_VALID, "授权尚未生效");
                    }
                    return ValidationResult.failure(ValidationStatus.EXPIRED, "授权已过期");
                }
            }
            
            // 5. 验证硬件绑定（如果启用）
            if (hardwareBindingEnabled && !validateHardwareBinding(licenseData)) {
                return ValidationResult.failure(ValidationStatus.HARDWARE_BINDING_FAILED, "硬件绑定验证失败");
            }
            
            // 6. 验证通过
            String successMessage = String.format("授权验证成功 - 客户: %s, 产品: %s, 剩余: %d天",
                    licenseData.getCustomerName(), licenseData.getProductId(), licenseData.getRemainingDays());
            
            Printer.instance.print(successMessage);
            return ValidationResult.success(licenseData, successMessage);
            
        } catch (Exception e) {
            Printer.instance.print("授权验证发生未知错误: " + e.getMessage());
            return ValidationResult.failure(ValidationStatus.UNKNOWN_ERROR, 
                                           "授权验证发生未知错误: " + e.getMessage());
        }
    }
    
    @Override
    public ValidationResult validateLicenseOnline(String licenseString, String encryptionKey, String publicKey) {
        // 首先执行离线验证
        ValidationResult offlineResult = validateLicenseOffline(licenseString, encryptionKey, publicKey);
        
        if (!offlineResult.isValid()) {
            return offlineResult;
        }
        
        // 执行在线验证（这里可以扩展为实际的服务器验证）
        try {
            Printer.instance.print("执行在线授权验证...");
            
            // TODO: 实现实际的在线验证逻辑
            // 例如：向授权服务器发送验证请求
            boolean onlineValid = performOnlineValidation(offlineResult.getLicenseData());
            
            if (!onlineValid) {
                return ValidationResult.failure(ValidationStatus.ONLINE_VERIFICATION_FAILED, "在线验证失败");
            }
            
            Printer.instance.print("在线授权验证通过");
            return offlineResult;
            
        } catch (Exception e) {
            Printer.instance.print("在线验证发生异常: " + e.getMessage());
            return ValidationResult.failure(ValidationStatus.ONLINE_VERIFICATION_FAILED, 
                                           "在线验证异常: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateExpiration(LicenseData licenseData) {
        if (licenseData == null) {
            return false;
        }
        
        // 检查是否是永久授权
        if (licenseData.getExpirationTime() == null) {
            return true; // 永久授权
        }
        
        LocalDateTime now = LocalDateTime.now();
        
        // 检查开始时间
        if (licenseData.getStartTime() != null && now.isBefore(licenseData.getStartTime())) {
            return false; // 授权尚未生效
        }
        
        // 检查过期时间
        return now.isBefore(licenseData.getExpirationTime());
    }
    
    @Override
    public boolean validateHardwareBinding(LicenseData licenseData) {
        if (licenseData == null || licenseData.getHardwareFingerprint() == null) {
            return !hardwareBindingEnabled; // 如果没有硬件绑定信息，根据配置决定是否通过
        }
        
        HardwareFingerprint.BindingPolicy bindingPolicy = licenseData.getHardwareFingerprint().getBindingPolicy();
        
        // 如果绑定策略为NONE，直接通过
        if (bindingPolicy == HardwareFingerprint.BindingPolicy.NONE) {
            return true;
        }
        
        try {
            // 获取当前系统的硬件指纹
            HardwareFingerprint currentFingerprint = 
                hardwareBindingService.getCurrentHardwareFingerprint(bindingPolicy);
            
            // 验证硬件绑定
            boolean bindingValid = hardwareBindingService.verifyHardwareBinding(
                licenseData.getHardwareFingerprint(), currentFingerprint);
            
            if (!bindingValid && !strictValidationMode) {
                // 非严格模式下，计算相似度评分
                int similarityScore = hardwareBindingService.calculateSimilarityScore(
                    licenseData.getHardwareFingerprint(), currentFingerprint);
                
                // 如果相似度评分超过阈值，也认为验证通过
                int threshold = getBindingThreshold(bindingPolicy);
                bindingValid = similarityScore >= threshold;
                
                Printer.instance.print("硬件绑定相似度评分: " + similarityScore + "%, 阈值: " + threshold + "%");
            }
            
            return bindingValid;
            
        } catch (Exception e) {
            Printer.instance.print("硬件绑定验证异常: " + e.getMessage());
            return !strictValidationMode; // 严格模式下异常即失败，非严格模式下异常可通过
        }
    }
    
    @Override
    public boolean validatePermission(LicenseData licenseData, String requiredPermission) {
        if (licenseData == null || requiredPermission == null) {
            return false;
        }
        
        return licenseData.hasPermission(requiredPermission);
    }
    
    @Override
    public boolean validateUserLimit(LicenseData licenseData, int currentUserCount) {
        if (licenseData == null) {
            return false;
        }
        
        Integer maxUsers = licenseData.getMaxUsers();
        if (maxUsers == null || maxUsers <= 0) {
            return true; // 无用户数限制
        }
        
        return currentUserCount <= maxUsers;
    }
    
    @Override
    public String generateValidationReport(LicenseData licenseData) {
        if (licenseData == null) {
            return "无效的授权码数据";
        }
        
        StringBuilder report = new StringBuilder();
        report.append("授权验证报告\n");
        report.append("==============\n\n");
        
        report.append("客户信息:\n");
        report.append("  客户ID: ").append(licenseData.getCustomerId()).append("\n");
        report.append("  客户名称: ").append(licenseData.getCustomerName()).append("\n");
        
        report.append("\n産品信息:\n");
        report.append("  产品ID: ").append(licenseData.getProductId()).append("\n");
        report.append("  产品版本: ").append(licenseData.getProductVersion()).append("\n");
        report.append("  授权类型: ").append(licenseData.getLicenseType().getDescription()).append("\n");
        
        report.append("\n时效信息:\n");
        report.append("  开始时间: ").append(licenseData.getStartTime()).append("\n");
        report.append("  到期时间: ").append(licenseData.getExpirationTime()).append("\n");
        report.append("  是否有效: ").append(licenseData.isActive() ? "是" : "否").append("\n");
        report.append("  剩余天数: ").append(licenseData.getRemainingDays()).append("天\n");
        
        report.append("\n权限信息:\n");
        report.append("  最大用户数: ").append(licenseData.getMaxUsers()).append("\n");
        if (licenseData.getPermissions() != null && !licenseData.getPermissions().isEmpty()) {
            report.append("  权限列表:\n");
            for (String permission : licenseData.getPermissions()) {
                report.append("    - ").append(permission).append("\n");
            }
        }
        
        report.append("\n硬件绑定:\n");
        if (licenseData.getHardwareFingerprint() != null) {
            HardwareFingerprint fingerprint = licenseData.getHardwareFingerprint();
            report.append("  绑定策略: ").append(fingerprint.getBindingPolicy().getDescription()).append("\n");
            
            try {
                HardwareFingerprint currentFingerprint = 
                    hardwareBindingService.getCurrentHardwareFingerprint(fingerprint.getBindingPolicy());
                String bindingReport = hardwareBindingService.generateBindingReport(fingerprint, currentFingerprint);
                report.append("\n").append(bindingReport);
            } catch (Exception e) {
                report.append("  绑定验证失败: ").append(e.getMessage()).append("\n");
            }
        } else {
            report.append("  无硬件绑定\n");
        }
        
        report.append("\n其他信息:\n");
        report.append("  生成时间: ").append(licenseData.getGeneratedAt()).append("\n");
        report.append("  颁发者: ").append(licenseData.getIssuer()).append("\n");
        report.append("  版本: ").append(licenseData.getVersion()).append("\n");
        
        return report.toString();
    }
    
    @Override
    public boolean isLicenseExpiringSoon(LicenseData licenseData, int warningDays) {
        if (licenseData == null || licenseData.getExpirationTime() == null) {
            return false; // 永久授权不会过期
        }
        
        long remainingDays = licenseData.getRemainingDays();
        return remainingDays >= 0 && remainingDays <= warningDays;
    }
    
    /**
     * 执行在线验证（模拟实现）
     */
    private boolean performOnlineValidation(LicenseData licenseData) {
        // TODO: 实现实际的在线验证逻辑
        // 例如：
        // 1. 向授权服务器发送HTTP请求
        // 2. 验证授权码状态
        // 3. 检查是否被撤销
        // 4. 更新使用统计
        
        try {
            // 模拟网络延迟
            Thread.sleep(100);
            
            // 模拟验证逻辑（实际应该连接真实的授权服务器）
            return licenseData.getCustomerId() != null && !licenseData.getCustomerId().isEmpty();
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    /**
     * 根据绑定策略获取相似度阈值
     */
    private int getBindingThreshold(HardwareFingerprint.BindingPolicy bindingPolicy) {
        switch (bindingPolicy) {
            case STRICT:
                return 95; // 严格模式要求95%以上相似度
            case MODERATE:
                return 80; // 中等模式要求80%以上相似度
            case LOOSE:
                return 60; // 宽松模式要求60%以上相似度
            case NONE:
            default:
                return 0;  // 无绑定模式不需要相似度
        }
    }
}