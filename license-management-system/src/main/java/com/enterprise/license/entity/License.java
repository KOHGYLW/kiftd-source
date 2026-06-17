package com.enterprise.license.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 授权实体类
 */
@Entity
@Table(name = "licenses")
public class License extends BaseEntity {

    @Column(name = "license_id", unique = true, nullable = false, length = 50)
    private String licenseId;

    @Column(name = "customer_id", nullable = false, length = 50)
    private String customerId;

    @Column(name = "product_name", nullable = false, length = 100)
    private String productName;

    @Column(name = "product_version", nullable = false, length = 20)
    private String productVersion;

    @Column(name = "license_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LicenseType licenseType;

    @Column(name = "license_status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private LicenseStatus licenseStatus;

    @Column(name = "issued_at", nullable = false)
    private LocalDateTime issuedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "hardware_fingerprint", length = 255)
    private String hardwareFingerprint;

    @Column(name = "encrypted_license", columnDefinition = "TEXT")
    private String encryptedLicense;

    @Column(name = "rsa_key_id", length = 50)
    private String rsaKeyId;

    @ElementCollection
    @CollectionTable(name = "license_features", joinColumns = @JoinColumn(name = "license_id"))
    @MapKeyColumn(name = "feature_name")
    @Column(name = "feature_value")
    private Map<String, String> features;

    @ElementCollection
    @CollectionTable(name = "license_restrictions", joinColumns = @JoinColumn(name = "license_id"))
    @MapKeyColumn(name = "restriction_name")
    @Column(name = "restriction_value")
    private Map<String, String> restrictions;

    @Column(name = "issuer", length = 100)
    private String issuer;

    @Column(name = "checksum", length = 255)
    private String checksum;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @Column(name = "revoke_reason", length = 500)
    private String revokeReason;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * 授权类型枚举
     */
    public enum LicenseType {
        TRIAL("试用版"),
        STANDARD("标准版"),
        PROFESSIONAL("专业版"),
        ENTERPRISE("企业版"),
        CUSTOM("定制版");

        private final String description;

        LicenseType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 授权状态枚举
     */
    public enum LicenseStatus {
        ACTIVE("激活"),
        EXPIRED("过期"),
        REVOKED("吊销"),
        SUSPENDED("暂停"),
        PENDING("待激活");

        private final String description;

        LicenseStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // Constructors
    public License() {}

    public License(String licenseId, String customerId, String productName) {
        this.licenseId = licenseId;
        this.customerId = customerId;
        this.productName = productName;
        this.licenseStatus = LicenseStatus.PENDING;
        this.issuedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public String getLicenseId() { return licenseId; }
    public void setLicenseId(String licenseId) { this.licenseId = licenseId; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(LicenseType licenseType) { this.licenseType = licenseType; }

    public LicenseStatus getLicenseStatus() { return licenseStatus; }
    public void setLicenseStatus(LicenseStatus licenseStatus) { this.licenseStatus = licenseStatus; }

    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }

    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    public String getHardwareFingerprint() { return hardwareFingerprint; }
    public void setHardwareFingerprint(String hardwareFingerprint) { this.hardwareFingerprint = hardwareFingerprint; }

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

    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void setRevokedAt(LocalDateTime revokedAt) { this.revokedAt = revokedAt; }

    public String getRevokeReason() { return revokeReason; }
    public void setRevokeReason(String revokeReason) { this.revokeReason = revokeReason; }

    public Integer getUsageCount() { return usageCount; }
    public void setUsageCount(Integer usageCount) { this.usageCount = usageCount; }

    public Integer getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    // Business methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return licenseStatus == LicenseStatus.ACTIVE && !isExpired();
    }

    public boolean isRevoked() {
        return licenseStatus == LicenseStatus.REVOKED;
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
        this.lastUsedAt = LocalDateTime.now();
    }

    public long getDaysUntilExpiration() {
        return java.time.temporal.ChronoUnit.DAYS.between(LocalDateTime.now(), expiresAt);
    }

    public boolean isUsageLimitReached() {
        return maxUsageCount != null && usageCount != null && usageCount >= maxUsageCount;
    }

    @Override
    public String toString() {
        return "License{" +
                "licenseId='" + licenseId + '\'' +
                ", customerId='" + customerId + '\'' +
                ", productName='" + productName + '\'' +
                ", licenseType=" + licenseType +
                ", licenseStatus=" + licenseStatus +
                ", expiresAt=" + expiresAt +
                '}';
    }
}