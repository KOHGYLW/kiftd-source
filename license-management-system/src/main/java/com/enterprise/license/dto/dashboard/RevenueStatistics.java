package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 收入统计DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "收入统计")
public class RevenueStatistics {

    @Schema(description = "统计类型", example = "MONTHLY")
    private String statisticsType;

    @Schema(description = "总收入", example = "1500000.00")
    private BigDecimal totalRevenue;

    @Schema(description = "本期收入", example = "120000.00")
    private BigDecimal currentPeriodRevenue;

    @Schema(description = "上期收入", example = "100000.00")
    private BigDecimal previousPeriodRevenue;

    @Schema(description = "增长率", example = "20.0")
    private Double growthRate;

    @Schema(description = "按产品分类收入")
    private List<ProductRevenue> productRevenues;

    @Schema(description = "按授权类型分类收入")
    private List<TypeRevenue> typeRevenues;

    @Schema(description = "收入趋势数据")
    private List<RevenuePoint> trendData;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "产品收入")
    public static class ProductRevenue {
        @Schema(description = "产品名称", example = "企业管理系统")
        private String productName;

        @Schema(description = "收入", example = "500000.00")
        private BigDecimal revenue;

        @Schema(description = "占比", example = "33.3")
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "类型收入")
    public static class TypeRevenue {
        @Schema(description = "授权类型", example = "COMMERCIAL")
        private String licenseType;

        @Schema(description = "收入", example = "800000.00")
        private BigDecimal revenue;

        @Schema(description = "占比", example = "53.3")
        private Double percentage;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "收入数据点")
    public static class RevenuePoint {
        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "收入", example = "50000.00")
        private BigDecimal revenue;
    }

}