package com.enterprise.license.controller;

import com.enterprise.license.dto.ApiResponse;
import com.enterprise.license.dto.dashboard.*;
import com.enterprise.license.service.DashboardService;
import com.enterprise.license.util.Constants;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse as SwaggerApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 仪表板控制器
 */
@RestController
@RequestMapping(Constants.ApiPaths.DASHBOARD)
@RequiredArgsConstructor
@Slf4j
@Tag(name = "仪表板", description = "仪表板数据统计相关接口")
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/overview")
    @Operation(summary = "获取总览数据", description = "获取仪表板总览统计数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<DashboardOverview>> getOverview() {
        
        log.info("获取仪表板总览数据");
        
        DashboardOverview overview = dashboardService.getOverview();
        
        return ResponseEntity.ok(ApiResponse.success("获取总览数据成功", overview));
    }

    @GetMapping("/license-statistics")
    @Operation(summary = "获取授权统计", description = "获取授权相关的统计数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<LicenseStatisticsData>> getLicenseStatistics() {
        
        log.info("获取授权统计数据");
        
        LicenseStatisticsData statistics = dashboardService.getLicenseStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("获取授权统计成功", statistics));
    }

    @GetMapping("/customer-statistics")
    @Operation(summary = "获取客户统计", description = "获取客户相关的统计数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<CustomerStatisticsData>> getCustomerStatistics() {
        
        log.info("获取客户统计数据");
        
        CustomerStatisticsData statistics = dashboardService.getCustomerStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("获取客户统计成功", statistics));
    }

    @GetMapping("/trends")
    @Operation(summary = "获取趋势数据", description = "获取指定时间范围的趋势数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "400", description = "请求参数错误"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<TrendData>> getTrends(
            @Parameter(description = "开始日期") @RequestParam(required = false) LocalDate startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) LocalDate endDate,
            @Parameter(description = "数据类型") @RequestParam(defaultValue = "LICENSE") String dataType) {
        
        log.info("获取趋势数据: startDate={}, endDate={}, dataType={}", startDate, endDate, dataType);
        
        // 默认最近30天
        if (startDate == null) {
            startDate = LocalDate.now().minusDays(30);
        }
        if (endDate == null) {
            endDate = LocalDate.now();
        }
        
        TrendData trends = dashboardService.getTrends(startDate, endDate, dataType);
        
        return ResponseEntity.ok(ApiResponse.success("获取趋势数据成功", trends));
    }

    @GetMapping("/recent-activities")
    @Operation(summary = "获取最近活动", description = "获取最近的系统活动记录")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<List<ActivityRecord>>> getRecentActivities(
            @Parameter(description = "记录数量") @RequestParam(defaultValue = "20") Integer limit) {
        
        log.info("获取最近活动: limit={}", limit);
        
        List<ActivityRecord> activities = dashboardService.getRecentActivities(limit);
        
        return ResponseEntity.ok(ApiResponse.success("获取最近活动成功", activities));
    }

    @GetMapping("/alerts")
    @Operation(summary = "获取系统告警", description = "获取需要关注的系统告警信息")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<List<SystemAlert>>> getSystemAlerts() {
        
        log.info("获取系统告警");
        
        List<SystemAlert> alerts = dashboardService.getSystemAlerts();
        
        return ResponseEntity.ok(ApiResponse.success("获取系统告警成功", alerts));
    }

    @GetMapping("/expiring-licenses")
    @Operation(summary = "获取即将过期的授权", description = "获取即将过期的授权列表")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<List<ExpiringLicense>>> getExpiringLicenses(
            @Parameter(description = "提前天数") @RequestParam(defaultValue = "30") Integer days,
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("获取即将过期的授权: days={}, limit={}", days, limit);
        
        List<ExpiringLicense> licenses = dashboardService.getExpiringLicenses(days, limit);
        
        return ResponseEntity.ok(ApiResponse.success("获取即将过期的授权成功", licenses));
    }

    @GetMapping("/top-customers")
    @Operation(summary = "获取重要客户", description = "获取授权数量最多的客户列表")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<List<TopCustomer>>> getTopCustomers(
            @Parameter(description = "数量限制") @RequestParam(defaultValue = "10") Integer limit) {
        
        log.info("获取重要客户: limit={}", limit);
        
        List<TopCustomer> customers = dashboardService.getTopCustomers(limit);
        
        return ResponseEntity.ok(ApiResponse.success("获取重要客户成功", customers));
    }

    @GetMapping("/revenue-statistics")
    @Operation(summary = "获取收入统计", description = "获取收入相关的统计数据")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<RevenueStatistics>> getRevenueStatistics(
            @Parameter(description = "统计类型") @RequestParam(defaultValue = "MONTHLY") String type) {
        
        log.info("获取收入统计: type={}", type);
        
        RevenueStatistics statistics = dashboardService.getRevenueStatistics(type);
        
        return ResponseEntity.ok(ApiResponse.success("获取收入统计成功", statistics));
    }

    @GetMapping("/license-usage")
    @Operation(summary = "获取授权使用情况", description = "获取授权的使用情况统计")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "获取成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证")
    })
    public ResponseEntity<ApiResponse<List<LicenseUsageData>>> getLicenseUsage() {
        
        log.info("获取授权使用情况");
        
        List<LicenseUsageData> usage = dashboardService.getLicenseUsage();
        
        return ResponseEntity.ok(ApiResponse.success("获取授权使用情况成功", usage));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新缓存", description = "刷新仪表板数据缓存")
    @ApiResponses(value = {
        @SwaggerApiResponse(responseCode = "200", description = "刷新成功"),
        @SwaggerApiResponse(responseCode = "401", description = "未认证"),
        @SwaggerApiResponse(responseCode = "403", description = "权限不足")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> refreshCache() {
        
        log.info("刷新仪表板数据缓存");
        
        dashboardService.refreshCache();
        
        return ResponseEntity.ok(ApiResponse.success("缓存刷新成功"));
    }

}