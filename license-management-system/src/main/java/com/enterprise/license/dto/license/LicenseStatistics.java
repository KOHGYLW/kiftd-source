package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 授权统计信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授权统计信息")
public class LicenseStatistics {

    @Schema(description = "总授权数", example = "100")
    private Long totalLicenses;

    @Schema(description = "活跃授权数", example = "85")
    private Long activeLicenses;

    @Schema(description = "过期授权数", example = "10")
    private Long expiredLicenses;

    @Schema(description = "暂停授权数", example = "3")
    private Long suspendedLicenses;

    @Schema(description = "撤销授权数", example = "2")
    private Long revokedLicenses;

    @Schema(description = "30天内即将过期数", example = "15")
    private Long expiringSoonLicenses;

    @Schema(description = "试用授权数", example = "20")
    private Long trialLicenses;

    @Schema(description = "商业授权数", example = "60")
    private Long commercialLicenses;

    @Schema(description = "今日新增授权数", example = "3")
    private Long todayNewLicenses;

    @Schema(description = "本月新增授权数", example = "25")
    private Long thisMonthNewLicenses;

}