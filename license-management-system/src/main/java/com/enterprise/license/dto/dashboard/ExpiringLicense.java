package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 即将过期授权DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "即将过期授权")
public class ExpiringLicense {

    @Schema(description = "授权ID", example = "1")
    private Long id;

    @Schema(description = "授权码", example = "LIC-ABC123-DEF456-GHI789")
    private String licenseKey;

    @Schema(description = "客户名称", example = "ABC科技有限公司")
    private String customerName;

    @Schema(description = "产品名称", example = "企业管理系统 v1.0")
    private String productName;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private String licenseType;

    @Schema(description = "结束日期")
    private LocalDateTime endDate;

    @Schema(description = "剩余天数", example = "15")
    private Long remainingDays;

    @Schema(description = "紧急程度", example = "HIGH")
    private String urgency;

}