package com.enterprise.license.dto;

import com.enterprise.license.entity.License;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 授权响应DTO
 */
@Schema(description = "授权响应")
public class LicenseResponseDto {

    @Schema(description = "授权ID", example = "LIC_123456789ABCDEF0")
    private String licenseId;

    @Schema(description = "客户ID", example = "CUST_12345")
    private String customerId;

    @Schema(description = "产品名称", example = "Enterprise License System")
    private String productName;

    @Schema(description = "产品版本", example = "1.0.0")
    private String productVersion;

    @Schema(description = "授权类型")
    private License.LicenseType licenseType;

    @Schema(description = "授权状态")
    private License.LicenseStatus licenseStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "签发时间", example = "2024-01-01 00:00:00")
    private LocalDateTime issuedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "过期时间", example = "2024-12-31 23:59:59")
    private LocalDateTime expiresAt;

    @Schema(description = "加密的授权码")
    private String encryptedLicense;

    @Schema(description = "RSA密钥ID")
    private String rsaKeyId;

    @Schema(description = "功能列表")
    private Map<String, String> features;

    @Schema(description = "限制条件")
    private Map<String, String> restrictions;

    @Schema(description = "签发者", example = "Enterprise License System")
    private String issuer;

    @Schema(description = "校验和")
    private String checksum;

    @Schema(description = "使用次数", example = "0")
    private Integer usageCount;

    @Schema(description = "最大使用次数")
    private Integer maxUsageCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后使用时间")
    private LocalDateTime lastUsedAt;

    @Schema(description = "是否已绑定硬件")
    private Boolean hardwareBound;

    @Schema(description = "剩余天数")
    private Long remainingDays;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "吊销时间")
    private LocalDateTime revokedAt;

    @Schema(description = "吊销原因")
    private String revokeReason;

    @Schema(description = "备注")
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // Constructors
    public LicenseResponseDto() {}

    // Getters and Setters
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public License.LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(License.LicenseType licenseType) { this.licenseType = licenseType; }

    public License.LicenseStatus getLicenseStatus() { return licenseStatus; }
    public void setLicenseStatus(License.LicenseStatus licenseStatus) { this.licenseStatus = licenseStatus; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getEncryptedLicense() { return encryptedLicense; }
    public void setEncryptedLicense(String encryptedLicense) { this.encryptedLicense = encryptedLicense; }

    public String getRsaKeyId() { return rsaKeyId; }
    public void setRsaKeyId(String rsaKeyId) { this.rsaKeyId = rsaKeyId; }

    public Map<String, String> getFeatures() { return features; }
    public void setFeatures(Map<String, String> features) { this.features = features; }

    public Map<String, String> getRestrictions() { return restrictions; }
    public void setRestrictions(Map<String, String> restrictions) { this.restrictions = restrictions; }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public String getChecksum() { return checksum; }
    public void setChecksum(String checksum) { this.checksum = checksum; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Integer getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public Boolean getHardwareBound() { return hardwareBound; }
    public void setHardwareBound(Boolean hardwareBound) { this.hardwareBound = hardwareBound; }

    public Long getRemainingDays() { return remainingDays; }
    public void setRemainingDays(Long remainingDays) { this.remainingDays = remainingDays; }

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "LicenseResponseDto{" +
                "licenseId='" + licenseId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", productName='" + productName + '\'' +
                ", licenseType=" + licenseType +
                ", licenseStatus=" + licenseStatus +
                ", expiresAt=" + expiresAt +
                '}';
    }
}