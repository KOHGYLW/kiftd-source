package com.enterprise.license.dto;

import com.enterprise.license.entity.License;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 授权请求DTO
 */
@Schema(description = "授权请求")
public class LicenseRequestDto {

    @NotBlank(message = "客户ID不能为空")
    @Size(max = 50, message = "客户ID长度不能超过50个字符")
    @Schema(description = "客户ID", example = "CUST_12345")
    private String customerId;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Schema(description = "产品名称", example = "Enterprise License System")
    private String productName;

    @NotBlank(message = "产品版本不能为空")
    @Size(max = 20, message = "产品版本长度不能超过20个字符")
    @Schema(description = "产品版本", example = "1.0.0")
    private String productVersion;

    @NotNull(message = "授权类型不能为空")
    @Schema(description = "授权类型")
    private License.LicenseType licenseType;

    @Min(value = 1, message = "有效期天数必须大于0")
    @Max(value = 3650, message = "有效期天数不能超过10年")
    @Schema(description = "有效期（天数）", example = "365")
    private Integer validityDays;

    @Schema(description = "功能列表")
    private Map<String, Object> features;

    @Schema(description = "限制条件")
    private Map<String, Object> restrictions;

    @Schema(description = "是否绑定硬件", example = "true")
    private Boolean bindHardware = false;

    @Schema(description = "硬件指纹（绑定硬件时使用）")
    private String hardwareFingerprint;

    @Size(max = 500, message = "备注长度不能超过500个字符")
    @Schema(description = "备注")
    private String notes;

    @Min(value = 1, message = "最大使用次数必须大于0")
    @Schema(description = "最大使用次数")
    private Integer maxUsageCount;

    // Constructors
    public LicenseRequestDto() {}

    // Getters and Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public License.LicenseType getLicenseType() { return licenseType; }
    public void setLicenseType(License.LicenseType licenseType) { this.licenseType = licenseType; }

    public Integer getValidityDays() { return validityDays; }
    public void setValidityDays(Integer validityDays) { this.validityDays = validityDays; }

    public Map<String, Object> getFeatures() { return features; }
    public void setFeatures(Map<String, Object> features) { this.features = features; }

    public Map<String, Object> getRestrictions() { return restrictions; }
    public void setRestrictions(Map<String, Object> restrictions) { this.restrictions = restrictions; }

    public Boolean getBindHardware() { return bindHardware; }
    public void setBindHardware(Boolean bindHardware) { this.bindHardware = bindHardware; }

    public String getHardwareFingerprint() { return hardwareFingerprint; }
    public void setHardwareFingerprint(String hardwareFingerprint) { this.hardwareFingerprint = hardwareFingerprint; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Integer getMaxUsageCount() { return maxUsageCount; }
    public void setMaxUsageCount(Integer maxUsageCount) { this.maxUsageCount = maxUsageCount; }

    @Override
    public String toString() {
        return "LicenseRequestDto{" +
                "customerId='" + customerId + '\'' +
                ", productName='" + productName + '\'' +
                ", productVersion='" + productVersion + '\'' +
                ", licenseType=" + licenseType +
                ", validityDays=" + validityDays +
                '}';
    }
}