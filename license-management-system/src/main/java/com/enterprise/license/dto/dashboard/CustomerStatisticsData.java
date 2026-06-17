package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 客户统计数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户统计数据")
public class CustomerStatisticsData {

    @Schema(description = "总客户数", example = "150")
    private Long totalCustomers;

    @Schema(description = "按状态统计")
    private Map<String, Long> statusCounts;

    @Schema(description = "按类型统计")
    private Map<String, Long> typeCounts;

    @Schema(description = "按行业统计")
    private Map<String, Long> industryCounts;

    @Schema(description = "本月新增", example = "12")
    private Long monthlyNewCustomers;

    @Schema(description = "活跃客户数", example = "120")
    private Long activeCustomers;

    @Schema(description = "客户保留率", example = "85.5")
    private Double retentionRate;

}