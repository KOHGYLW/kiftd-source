package com.enterprise.license.exception;

/**
 * 授权相关异常
 */
public class LicenseException extends BusinessException {

    public static final String LICENSE_NOT_FOUND = "LICENSE_NOT_FOUND";
    public static final String LICENSE_EXPIRED = "LICENSE_EXPIRED";  
    public static final String LICENSE_INACTIVE = "LICENSE_INACTIVE";
    public static final String LICENSE_REVOKED = "LICENSE_REVOKED";
    public static final String LICENSE_SUSPENDED = "LICENSE_SUSPENDED";
    public static final String LICENSE_INVALID_HARDWARE = "LICENSE_INVALID_HARDWARE";
    public static final String LICENSE_MAX_ACTIVATION_EXCEEDED = "LICENSE_MAX_ACTIVATION_EXCEEDED";
    public static final String LICENSE_GENERATION_FAILED = "LICENSE_GENERATION_FAILED";

    public LicenseException(String message) {
        super(message);
    }

    public LicenseException(String code, String message) {
        super(code, message);
    }

    public static LicenseException notFound(String licenseKey) {
        return new LicenseException(LICENSE_NOT_FOUND, "授权码不存在: " + licenseKey);
    }

    public static LicenseException expired(String licenseKey) {
        return new LicenseException(LICENSE_EXPIRED, "授权已过期: " + licenseKey);
    }

    public static LicenseException inactive(String licenseKey) {
        return new LicenseException(LICENSE_INACTIVE, "授权未激活: " + licenseKey);
    }

    public static LicenseException revoked(String licenseKey) {
        return new LicenseException(LICENSE_REVOKED, "授权已撤销: " + licenseKey);
    }

    public static LicenseException suspended(String licenseKey) {
        return new LicenseException(LICENSE_SUSPENDED, "授权已暂停: " + licenseKey);
    }

    public static LicenseException invalidHardware() {
        return new LicenseException(LICENSE_INVALID_HARDWARE, "硬件指纹不匹配");
    }

    public static LicenseException maxActivationExceeded() {
        return new LicenseException(LICENSE_MAX_ACTIVATION_EXCEEDED, "超过最大激活次数限制");
    }

    public static LicenseException generationFailed(String reason) {
        return new LicenseException(LICENSE_GENERATION_FAILED, "授权生成失败: " + reason);
    }

}