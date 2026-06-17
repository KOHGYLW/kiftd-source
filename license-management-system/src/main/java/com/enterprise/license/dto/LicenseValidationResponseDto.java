package com.enterprise.license.dto;

import com.enterprise.license.service.LicenseValidationService;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 授权验证响应DTO
 */
@Schema(description = "授权验证响应")
public class LicenseValidationResponseDto {

    @Schema(description = "验证结果")
    private LicenseValidationService.ValidationResult result;

    @Schema(description = "验证消息")
    private String message;

    @Schema(description = "是否有效", example = "true")
    private Boolean valid;

    @Schema(description = "授权ID")
    private String licenseId;

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品版本")
    private String productVersion;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "签发时间")
    private LocalDateTime issuedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间")
    private LocalDateTime expiresAt;

    @Schema(description = "剩余天数")
    private Long remainingDays;

    @Schema(description = "硬件指纹相似度", example = "0.95")
    private Double hardwareSimilarity;

    @Schema(description = "是否绑定硬件")
    private Boolean hardwareBound;

    @Schema(description = "功能列表")
    private Map<String, Object> features;

    @Schema(description = "限制条件")
    private Map<String, Object> restrictions;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "验证时间")
    private LocalDateTime validatedAt;

    @Schema(description = "验证类型（ONLINE/OFFLINE）")
    private String validationType;

    @Schema(description = "使用次数")
    private Integer usageCount;

    @Schema(description = "最大使用次数")
    private Integer maxUsageCount;

    @Schema(description = "元数据")
    private Map<String, Object> metadata;

    // Constructors
    public LicenseValidationResponseDto() {
        this.validatedAt = LocalDateTime.now();
    }

    public LicenseValidationResponseDto(LicenseValidationService.ValidationResult result, String message) {
        this();
        this.result = result;
        this.message = message;
        this.valid = result == LicenseValidationService.ValidationResult.VALID;
    }

    // Getters and Setters
    public LicenseValidationService.ValidationResult getResult() { return result; }
    public void setResult(LicenseValidationService.ValidationResult result) { 
        this.result = result; 
        this.valid = result == LicenseValidationService.ValidationResult.VALID;
    }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }

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

    public Long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Long remainingDays) { this.remainingDays = remainingDays; }

    public Double getHardwareSimilarity() { return hardwareSimilarity; }
    public void setHardwareSimilarity(Double hardwareSimilarity) { this.hardwareSimilarity = hardwareSimilarity; }

    public Boolean getHardwareBound() { return hardwareBound; }
    public void setHardwareBound(Boolean hardwareBound) { this.hardwareBound = hardwareBound; }

    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }

    public Map<String, Object> getRestrictions() { return restrictions; }
    public void setRestrictions(Map<String, Object> restrictions) { this.restrictions = restrictions; }

    public LocalDateTime getValidatedAt() { return validatedAt; }
    public void setValidatedAt(LocalDateTime validatedAt) { this.validatedAt = validatedAt; }

    public String getValidationType() { return validationType; }
    public void setValidationType(String validationType) { this.validationType = validationType; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Integer getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    @Override
    public String toString() {
        return "LicenseValidationResponseDto{" +
                "result=" + result +
                ", valid=" + valid +
                ", licenseId='" + licenseId + '\'' +
                ", message='" + message + '\'' +
                ", remainingDays=" + remainingDays +
                '}';
    }
}