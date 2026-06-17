package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 授权统计数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授权统计数据")
public class LicenseStatisticsData {

    @Schema(description = "总授权数", example = "300")
    private Long totalLicenses;

    @Schema(description = "按状态统计")
    private Map<String, Long> statusCounts;

    @Schema(description = "按类型统计")
    private Map<String, Long> typeCounts;

    @Schema(description = "按产品统计")
    private Map<String, Long> productCounts;

    @Schema(description = "本月新增", example = "25")
    private Long monthlyNewLicenses;

    @Schema(description = "本月到期", example = "8")
    private Long monthlyExpiredLicenses;

    @Schema(description = "平均授权时长（天）", example = "365")
    private Double averageLicenseDuration;

}