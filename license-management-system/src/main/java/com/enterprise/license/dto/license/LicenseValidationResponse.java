package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权验证响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授权验证响应")
public class LicenseValidationResponse {

    @Schema(description = "验证结果", example = "true")
    private Boolean valid;

    @Schema(description = "验证消息", example = "授权验证成功")
    private String message;

    @Schema(description = "错误码", example = "LICENSE_EXPIRED")
    private String errorCode;

    @Schema(description = "授权信息")
    private LicenseInfo licenseInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "授权信息")
    public static class LicenseInfo {
        @Schema(description = "授权码", example = "LIC-ABC123-DEF456-GHI789")
        private String licenseKey;

        @Schema(description = "客户名称", example = "ABC科技有限公司")
        private String customerName;

        @Schema(description = "产品名称", example = "企业管理系统 v1.0")
        private String productName;

        @Schema(description = "产品版本", example = "1.0.0")
        private String productVersion;

        @Schema(description = "授权类型", example = "COMMERCIAL")
        private String licenseType;

        @Schema(description = "最大用户数", example = "100")
        private Integer maxUsers;

        @Schema(description = "最大设备数", example = "50")
        private Integer maxDevices;

        @Schema(description = "授权开始日期")
        private LocalDateTime startDate;

        @Schema(description = "授权结束日期")
        private LocalDateTime endDate;

        @Schema(description = "授权功能列表")
        private List<String> features;

        @Schema(description = "剩余天数", example = "365")
        private Long remainingDays;
    }

    public static LicenseValidationResponse success(LicenseInfo licenseInfo) {
        return new LicenseValidationResponse(true, "授权验证成功", null, licenseInfo);
    }

    public static LicenseValidationResponse failure(String message, String errorCode) {
        return new LicenseValidationResponse(false, message, errorCode, null);
    }

}