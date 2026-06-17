package com.enterprise.license.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 统计结果对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "统计结果")
public class StatisticsResult {

    /**
     * 仪表板数据
     */
    @Data
    @Schema(description = "仪表板数据")
    public static class DashboardData {

        @Schema(description = "总客户数")
        private Long totalCustomers;

        @Schema(description = "活跃客户数")
        private Long activeCustomers;

        @Schema(description = "总授权数")
        private Long totalLicenses;

        @Schema(description = "有效授权数")
        private Long activeLicenses;

        @Schema(description = "即将过期授权数")
        private Long expiringLicenses;

        @Schema(description = "过期授权数")
        private Long expiredLicenses;

        @Schema(description = "今日验证次数")
        private Long todayValidations;

        @Schema(description = "本月验证次数")
        private Long monthValidations;

        @Schema(description = "总收入")
        private BigDecimal totalRevenue;

        @Schema(description = "本月收入")
        private BigDecimal monthRevenue;

        @Schema(description = "客户状态分布")
        private Map<String, Long> customerStatusDistribution;

        @Schema(description = "授权状态分布")
        private Map<String, Long> licenseStatusDistribution;

        @Schema(description = "授权类型分布")
        private Map<String, Long> licenseTypeDistribution;

        @Schema(description = "最近7天验证趋势")
        private List<DailyValidation> validationTrend;

        @Schema(description = "最近30天收入趋势")
        private List<DailyRevenue> revenueTrend;
    }

    /**
     * 每日验证统计
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "每日验证统计")
    public static class DailyValidation {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "验证次数")
        private Long count;

        @Schema(description = "成功次数")
        private Long successCount;

        @Schema(description = "失败次数")
        private Long failedCount;
    }

    /**
     * 每日收入统计
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "每日收入统计")
    public static class DailyRevenue {

        @Schema(description = "日期")
        private String date;

        @Schema(description = "收入金额")
        private BigDecimal amount;

        @Schema(description = "授权数量")
        private Long licenseCount;
    }

    /**
     * 客户活跃度统计
     */
    @Data
    @Schema(description = "客户活跃度统计")
    public static class CustomerActivity {

        @Schema(description = "客户ID")
        private Long customerId;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "授权数量")
        private Long licenseCount;

        @Schema(description = "验证次数")
        private Long validationCount;

        @Schema(description = "最后验证时间")
        private LocalDateTime lastValidationTime;

        @Schema(description = "活跃度评分")
        private Double activityScore;
    }

    /**
     * 产品使用统计
     */
    @Data
    @Schema(description = "产品使用统计")
    public static class ProductUsage {

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "授权数量")
        private Long licenseCount;

        @Schema(description = "活跃授权数")
        private Long activeLicenseCount;

        @Schema(description = "验证次数")
        private Long validationCount;

        @Schema(description = "收入")
        private BigDecimal revenue;
    }

    /**
     * 地域分布统计
     */
    @Data
    @Schema(description = "地域分布统计")
    public static class RegionDistribution {

        @Schema(description = "地区")
        private String region;

        @Schema(description = "客户数量")
        private Long customerCount;

        @Schema(description = "授权数量")
        private Long licenseCount;

        @Schema(description = "验证次数")
        private Long validationCount;
    }
}