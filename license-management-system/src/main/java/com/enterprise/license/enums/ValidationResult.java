package com.enterprise.license.enums;

import lombok.Getter;

/**
 * 验证结果枚举
 */
@Getter
public enum ValidationResult {
    
    /**
     * 验证成功
     */
    SUCCESS("SUCCESS", "验证成功", true),
    
    /**
     * 授权不存在
     */
    LICENSE_NOT_FOUND("LICENSE_NOT_FOUND", "授权不存在", false),
    
    /**
     * 授权已过期
     */
    LICENSE_EXPIRED("LICENSE_EXPIRED", "授权已过期", false),
    
    /**
     * 授权已被撤销
     */
    LICENSE_REVOKED("LICENSE_REVOKED", "授权已被撤销", false),
    
    /**
     * 授权未激活
     */
    LICENSE_NOT_ACTIVE("LICENSE_NOT_ACTIVE", "授权未激活", false),
    
    /**
     * 硬件指纹不匹配
     */
    HARDWARE_MISMATCH("HARDWARE_MISMATCH", "硬件指纹不匹配", false),
    
    /**
     * 超出使用次数限制
     */
    USAGE_LIMIT_EXCEEDED("USAGE_LIMIT_EXCEEDED", "超出使用次数限制", false),
    
    /**
     * 超出并发用户限制
     */
    CONCURRENT_USER_LIMIT_EXCEEDED("CONCURRENT_USER_LIMIT_EXCEEDED", "超出并发用户限制", false),
    
    /**
     * 授权密钥无效
     */
    INVALID_LICENSE_KEY("INVALID_LICENSE_KEY", "授权密钥无效", false),
    
    /**
     * 签名验证失败
     */
    SIGNATURE_VERIFICATION_FAILED("SIGNATURE_VERIFICATION_FAILED", "签名验证失败", false),
    
    /**
     * 系统时间异常
     */
    SYSTEM_TIME_INVALID("SYSTEM_TIME_INVALID", "系统时间异常", false),
    
    /**
     * 网络验证失败
     */
    NETWORK_VALIDATION_FAILED("NETWORK_VALIDATION_FAILED", "网络验证失败", false),
    
    /**
     * 验证服务不可用
     */
    VALIDATION_SERVICE_UNAVAILABLE("VALIDATION_SERVICE_UNAVAILABLE", "验证服务不可用", false),
    
    /**
     * 未知错误
     */
    UNKNOWN_ERROR("UNKNOWN_ERROR", "未知错误", false);
    
    private final String code;
    private final String description;
    private final boolean isValid;
    
    ValidationResult(String code, String description, boolean isValid) {
        this.code = code;
        this.description = description;
        this.isValid = isValid;
    }
    
    /**
     * 根据代码获取枚举值
     * @param code 结果代码
     * @return ValidationResult枚举值
     */
    public static ValidationResult fromCode(String code) {
        for (ValidationResult result : values()) {
            if (result.getCode().equals(code)) {
                return result;
            }
        }
        throw new IllegalArgumentException("未知的验证结果代码: " + code);
    }
    
    /**
     * 判断验证是否成功
     * @return true如果验证成功
     */
    public boolean isSuccess() {
        return this == SUCCESS;
    }
}