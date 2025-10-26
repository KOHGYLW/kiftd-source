package com.enterprise.license.exception;

/**
 * 授权验证异常
 */
public class LicenseValidationException extends BusinessException {

    public LicenseValidationException(String message) {
        super("LICENSE_VALIDATION_ERROR", message);
    }

    public LicenseValidationException(String licenseCode, String message) {
        super("LICENSE_VALIDATION_ERROR", String.format("License '%s': %s", licenseCode, message));
    }
}