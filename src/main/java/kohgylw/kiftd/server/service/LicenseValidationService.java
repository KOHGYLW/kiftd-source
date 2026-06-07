package kohgylw.kiftd.server.service;

import kohgylw.kiftd.server.pojo.LicenseData;
import kohgylw.kiftd.server.pojo.HardwareFingerprint;

/**
 * 授权验证服务接口
 * 提供完整的授权码验证功能
 * 
 * @author 企业安全专家
 * @version 2.0
 */
public interface LicenseValidationService {
    
    /**
     * 验证授权码
     * @param licenseString 授权码字符串
     * @param encryptionKey 解密密钥
     * @param publicKey 公钥（用于数字签名验证）
     * @return ValidationResult 验证结果
     */
    ValidationResult validateLicense(String licenseString, String encryptionKey, String publicKey);
    
    /**
     * 离线验证授权码（不检查在线状态）
     * @param licenseString 授权码字符串
     * @param encryptionKey 解密密钥
     * @param publicKey 公钥
     * @return ValidationResult 验证结果
     */
    ValidationResult validateLicenseOffline(String licenseString, String encryptionKey, String publicKey);
    
    /**
     * 在线验证授权码（包含服务器验证）
     * @param licenseString 授权码字符串
     * @param encryptionKey 解密密钥
     * @param publicKey 公钥
     * @return ValidationResult 验证结果
     */
    ValidationResult validateLicenseOnline(String licenseString, String encryptionKey, String publicKey);
    
    /**
     * 验证授权码的时效性
     * @param licenseData 授权码数据
     * @return boolean 是否在有效期内
     */
    boolean validateExpiration(LicenseData licenseData);
    
    /**
     * 验证硬件绑定
     * @param licenseData 授权码数据
     * @return boolean 硬件绑定是否通过
     */
    boolean validateHardwareBinding(LicenseData licenseData);
    
    /**
     * 验证用户权限
     * @param licenseData 授权码数据
     * @param requiredPermission 所需权限
     * @return boolean 是否有权限
     */
    boolean validatePermission(LicenseData licenseData, String requiredPermission);
    
    /**
     * 验证用户数限制
     * @param licenseData 授权码数据
     * @param currentUserCount 当前用户数
     * @return boolean 是否超出用户数限制
     */
    boolean validateUserLimit(LicenseData licenseData, int currentUserCount);
    
    /**
     * 生成验证报告
     * @param licenseData 授权码数据
     * @return String 验证报告
     */
    String generateValidationReport(LicenseData licenseData);
    
    /**
     * 检查授权码是否即将过期
     * @param licenseData 授权码数据
     * @param warningDays 提前警告天数
     * @return boolean 是否即将过期
     */
    boolean isLicenseExpiringSoon(LicenseData licenseData, int warningDays);
    
    /**
     * 验证结果枚举
     */
    enum ValidationStatus {
        /** 验证成功 */
        SUCCESS("验证成功"),
        /** 授权码格式错误 */
        INVALID_FORMAT("授权码格式错误"),
        /** 解密失败 */
        DECRYPTION_FAILED("解密失败"),
        /** 数字签名验证失败 */
        SIGNATURE_VERIFICATION_FAILED("数字签名验证失败"),
        /** 授权已过期 */
        EXPIRED("授权已过期"),
        /** 硬件绑定失败 */
        HARDWARE_BINDING_FAILED("硬件绑定失败"),
        /** 权限不足 */
        INSUFFICIENT_PERMISSION("权限不足"),
        /** 超出用户数限制 */
        USER_LIMIT_EXCEEDED("超出用户数限制"),
        /** 授权未生效 */
        NOT_YET_VALID("授权未生效"),
        /** 在线验证失败 */
        ONLINE_VERIFICATION_FAILED("在线验证失败"),
        /** 未知错误 */
        UNKNOWN_ERROR("未知错误");
        
        private final String description;
        
        ValidationStatus(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 验证结果数据结构
     */
    class ValidationResult {
        private final ValidationStatus status;
        private final boolean valid;
        private final String message;
        private final LicenseData licenseData;
        private final String details;
        private final long remainingDays;
        
        public ValidationResult(ValidationStatus status, boolean valid, String message, 
                              LicenseData licenseData, String details, long remainingDays) {
            this.status = status;
            this.valid = valid;
            this.message = message;
            this.licenseData = licenseData;
            this.details = details;
            this.remainingDays = remainingDays;
        }
        
        // Static factory methods
        public static ValidationResult success(LicenseData licenseData, String message) {
            long remainingDays = licenseData != null ? licenseData.getRemainingDays() : 0;
            return new ValidationResult(ValidationStatus.SUCCESS, true, message, licenseData, "", remainingDays);
        }
        
        public static ValidationResult failure(ValidationStatus status, String message) {
            return new ValidationResult(status, false, message, null, "", 0);
        }
        
        public static ValidationResult failure(ValidationStatus status, String message, String details) {
            return new ValidationResult(status, false, message, null, details, 0);
        }
        
        // Getters
        public ValidationStatus getStatus() {
            return status;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public String getMessage() {
            return message;
        }
        
        public LicenseData getLicenseData() {
            return licenseData;
        }
        
        public String getDetails() {
            return details;
        }
        
        public long getRemainingDays() {
            return remainingDays;
        }
        
        @Override
        public String toString() {
            return "ValidationResult{" +
                    "status=" + status +
                    ", valid=" + valid +
                    ", message='" + message + '\'' +
                    ", remainingDays=" + remainingDays +
                    '}';
        }
    }
}