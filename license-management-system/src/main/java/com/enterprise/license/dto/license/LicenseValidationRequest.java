package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 授权验证请求DTO
 */
@Data
@Schema(description = "授权验证请求")
public class LicenseValidationRequest {

    @NotBlank(message = "授权码不能为空")
    @Schema(description = "授权码", example = "LIC-ABC123-DEF456-GHI789", required = true)
    private String licenseKey;

    @Schema(description = "硬件指纹")
    private String hardwareId;

    @Schema(description = "MAC地址")
    private String macAddress;

    @Schema(description = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "域名", example = "example.com")
    private String domain;

    @Schema(description = "产品版本", example = "1.0.0")
    private String productVersion;

    @Schema(description = "客户端信息")
    private String clientInfo;

}