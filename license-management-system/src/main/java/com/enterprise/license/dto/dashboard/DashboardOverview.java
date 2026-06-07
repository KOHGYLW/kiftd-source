package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 仪表板总览数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "仪表板总览数据")
public class DashboardOverview {

    @Schema(description = "总客户数", example = "150")
    private Long totalCustomers;

    @Schema(description = "活跃客户数", example = "120")
    private Long activeCustomers;

    @Schema(description = "总授权数", example = "300")
    private Long totalLicenses;

    @Schema(description = "活跃授权数", example = "250")
    private Long activeLicenses;

    @Schema(description = "即将过期授权数", example = "15")
    private Long expiringSoonLicenses;

    @Schema(description = "今日新增客户", example = "3")
    private Long todayNewCustomers;

    @Schema(description = "今日新增授权", example = "5")
    private Long todayNewLicenses;

    @Schema(description = "本月收入", example = "50000.00")
    private BigDecimal monthlyRevenue;

    @Schema(description = "授权使用率", example = "83.5")
    private Double licenseUtilization;

    @Schema(description = "客户满意度", example = "4.2")
    private Double customerSatisfaction;

}