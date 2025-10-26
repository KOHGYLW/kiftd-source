package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 客户授权信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户授权信息")
public class CustomerLicenseInfo {

    @Schema(description = "授权ID", example = "1")
    private Long id;

    @Schema(description = "授权码", example = "LIC-ABC123-DEF456-GHI789")
    private String licenseKey;

    @Schema(description = "产品名称", example = "企业管理系统 v1.0")
    private String productName;

    @Schema(description = "产品版本", example = "1.0.0")
    private String productVersion;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private String licenseType;

    @Schema(description = "授权状态", example = "ACTIVE")
    private String status;

    @Schema(description = "授权开始日期")
    private LocalDateTime startDate;

    @Schema(description = "授权结束日期")
    private LocalDateTime endDate;

    @Schema(description = "是否有效")
    private Boolean isValid;

    @Schema(description = "剩余天数", example = "365")
    private Long remainingDays;

}