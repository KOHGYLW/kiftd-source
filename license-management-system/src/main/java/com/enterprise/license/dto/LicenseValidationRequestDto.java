package com.enterprise.license.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 授权验证请求DTO
 */
@Schema(description = "授权验证请求")
public class LicenseValidationRequestDto {

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "加密的授权码", required = true)
    private String licenseKey;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 100, message = "产品名称长度不能超过100个字符")
    @Schema(description = "产品名称", example = "Enterprise License System", required = true)
    private String productName;

    @NotBlank(message = "产品版本不能为空")
    @Size(max = 20, message = "产品版本长度不能超过20个字符")
    @Schema(description = "产品版本", example = "1.0.0", required = true)
    private String productVersion;

    @Schema(description = "验证特定功能")
    private String featureName;

    @Schema(description = "是否在线验证", example = "true")
    private Boolean online = true;

    @Schema(description = "RSA密钥ID（可选，用于指定解密密钥）")
    private String rsaKeyId;

    // Constructors
    public LicenseValidationRequestDto() {}

    public LicenseValidationRequestDto(String licenseKey, String productName, String productVersion) {
        this.licenseKey = licenseKey;
        this.productName = productName;
        this.productVersion = productVersion;
    }

    // Getters and Setters
    public String getLicenseKey() { return licenseKey; }
    public void setLicenseKey(String licenseKey) { this.licenseKey = licenseKey; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public String getProductVersion() { return productVersion; }
    public void setProductVersion(String productVersion) { this.productVersion = productVersion; }

    public String getFeatureName() { return featureName; }
    public void setFeatureName(String featureName) { this.featureName = featureName; }

    public Boolean getOnline() { return online; }
    public void setOnline(Boolean online) { this.online = online; }

    public String getRsaKeyId() { return rsaKeyId; }
    public void setRsaKeyId(String rsaKeyId) { this.rsaKeyId = rsaKeyId; }

    @Override
    public String toString() {
        return "LicenseValidationRequestDto{" +
                "productName='" + productName + '\'' +
                ", productVersion='" + productVersion + '\'' +
                ", featureName='" + featureName + '\'' +
                ", online=" + online +
                '}';
    }
}