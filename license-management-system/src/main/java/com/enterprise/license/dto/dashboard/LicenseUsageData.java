package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权使用情况DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授权使用情况")
public class LicenseUsageData {

    @Schema(description = "产品名称", example = "企业管理系统")
    private String productName;

    @Schema(description = "总授权数", example = "100")
    private Long totalLicenses;

    @Schema(description = "活跃授权数", example = "85")
    private Long activeLicenses;

    @Schema(description = "使用率", example = "85.0")
    private Double usageRate;

    @Schema(description = "平均激活次数", example = "2.3")
    private Double averageActivations;

    @Schema(description = "最大用户数", example = "5000")
    private Long maxUsers;

    @Schema(description = "当前用户数", example = "4200")
    private Long currentUsers;

}