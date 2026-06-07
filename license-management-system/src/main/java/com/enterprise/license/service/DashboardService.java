package com.enterprise.license.service;

import com.enterprise.license.dto.StatisticsResult;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.entity.LicenseValidationLog;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import com.enterprise.license.repository.LicenseValidationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 仪表板数据服务
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
public class DashboardService extends BaseService {

    private final CustomerRepository customerRepository;
    private final LicenseRepository licenseRepository;
    private final LicenseValidationLogRepository validationLogRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取仪表板数据
     */
    @Cacheable(value = "dashboard_data", key = "'overview'")
    public StatisticsResult.DashboardData getDashboardData() {
        log.info("获取仪表板数据");

        StatisticsResult.DashboardData dashboardData = new StatisticsResult.DashboardData();
        LocalDateTime now = getCurrentTime();

        // 基础统计数据
        dashboardData.setTotalCustomers(getTotalCustomers());
        dashboardData.setActiveCustomers(getActiveCustomers());
        dashboardData.setTotalLicenses(getTotalLicenses());
        dashboardData.setActiveLicenses(getActiveLicenses(now));
        dashboardData.setExpiringLicenses(getExpiringLicenses(now, 30));
        dashboardData.setExpiredLicenses(getExpiredLicenses(now));

        // 验证统计
        dashboardData.setTodayValidations(getTodayValidations(now));
        dashboardData.setMonthValidations(getMonthValidations(now));

        // 收入统计
        dashboardData.setTotalRevenue(getTotalRevenue());
        dashboardData.setMonthRevenue(getMonthRevenue(now));

        // 分布统计
        dashboardData.setCustomerStatusDistribution(getCustomerStatusDistribution());
        dashboardData.setLicenseStatusDistribution(getLicenseStatusDistribution());
        dashboardData.setLicenseTypeDistribution(getLicenseTypeDistribution());

        // 趋势数据
        dashboardData.setValidationTrend(getValidationTrend(now, 7));
        dashboardData.setRevenueTrend(getRevenueTrend(now, 30));

        logOperation("获取仪表板数据", "数据获取完成");
        return dashboardData;
    }

    /**
     * 获取客户活跃度分析
     */
    @Cacheable(value = "customer_activity", key = "'analysis'")
    public List<StatisticsResult.CustomerActivity> getCustomerActivityAnalysis() {
        log.info("获取客户活跃度分析");

        List<Customer> customers = customerRepository.findByStatusAndDeletedFalse(Customer.CustomerStatus.ACTIVE);
        List<StatisticsResult.CustomerActivity> activities = new ArrayList<>();

        for (Customer customer : customers) {
            StatisticsResult.CustomerActivity activity = new StatisticsResult.CustomerActivity();
            activity.setCustomerId(customer.getId());
            activity.setCustomerName(customer.getCustomerName());

            // 统计授权数量
            long licenseCount = licenseRepository.countByCustomerId(customer.getId());
            activity.setLicenseCount(licenseCount);

            // 统计验证次数（最近30天）
            LocalDateTime thirtyDaysAgo = getCurrentTime().minusDays(30);
            long validationCount = validationLogRepository.countByLicenseIdAndTimeBetween(
                    customer.getId(), thirtyDaysAgo, getCurrentTime());
            activity.setValidationCount(validationCount);

            // 最后验证时间
            List<LicenseValidationLog> recentLogs = validationLogRepository.findRecentByLicenseId(
                    customer.getId(), PageRequest.of(0, 1));
            if (!recentLogs.isEmpty()) {
                activity.setLastValidationTime(recentLogs.get(0).getValidationTime());
            }

            // 计算活跃度评分（基于授权数量、验证频率等）
            double score = calculateActivityScore(licenseCount, validationCount, activity.getLastValidationTime());
            activity.setActivityScore(score);

            activities.add(activity);
        }

        // 按活跃度评分排序
        activities.sort((a, b) -> Double.compare(b.getActivityScore(), a.getActivityScore()));

        return activities.stream().limit(50).collect(Collectors.toList());
    }

    /**
     * 获取产品使用统计
     */
    @Cacheable(value = "product_usage", key = "'statistics'")
    public List<StatisticsResult.ProductUsage> getProductUsageStatistics() {
        log.info("获取产品使用统计");

        // 按产品名称分组统计
        List<License> allLicenses = licenseRepository.findByDeletedFalse();
        Map<String, List<License>> productGroups = allLicenses.stream()
                .collect(Collectors.groupingBy(License::getProductName));

        List<StatisticsResult.ProductUsage> usageList = new ArrayList<>();
        LocalDateTime now = getCurrentTime();

        for (Map.Entry<String, List<License>> entry : productGroups.entrySet()) {
            String productName = entry.getKey();
            List<License> licenses = entry.getValue();

            StatisticsResult.ProductUsage usage = new StatisticsResult.ProductUsage();
            usage.setProductName(productName);
            usage.setLicenseCount((long) licenses.size());

            // 统计活跃授权数
            long activeLicenses = licenses.stream()
                    .filter(license -> license.getStatus() == License.LicenseStatus.ACTIVE &&
                                     license.getExpireTime().isAfter(now))
                    .count();
            usage.setActiveLicenseCount(activeLicenses);

            // 统计验证次数
            long totalValidations = licenses.stream()
                    .mapToLong(license -> license.getValidationCount() != null ? license.getValidationCount() : 0)
                    .sum();
            usage.setValidationCount(totalValidations);

            // 统计收入
            BigDecimal totalRevenue = licenses.stream()
                    .filter(license -> license.getPrice() != null)
                    .map(License::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            usage.setRevenue(totalRevenue);

            usageList.add(usage);
        }

        // 按授权数量排序
        usageList.sort((a, b) -> Long.compare(b.getLicenseCount(), a.getLicenseCount()));
        return usageList;
    }

    /**
     * 获取地域分布统计
     */
    @Cacheable(value = "region_distribution", key = "'statistics'")
    public List<StatisticsResult.RegionDistribution> getRegionDistribution() {
        log.info("获取地域分布统计");

        List<Customer> customers = customerRepository.findByDeletedFalse();
        Map<String, List<Customer>> regionGroups = customers.stream()
                .filter(customer -> customer.getAddress() != null && !customer.getAddress().isEmpty())
                .collect(Collectors.groupingBy(this::extractRegion));

        List<StatisticsResult.RegionDistribution> distributions = new ArrayList<>();

        for (Map.Entry<String, List<Customer>> entry : regionGroups.entrySet()) {
            String region = entry.getKey();
            List<Customer> regionCustomers = entry.getValue();

            StatisticsResult.RegionDistribution distribution = new StatisticsResult.RegionDistribution();
            distribution.setRegion(region);
            distribution.setCustomerCount((long) regionCustomers.size());

            // 统计该地区的授权数量
            long totalLicenses = regionCustomers.stream()
                    .mapToLong(customer -> licenseRepository.countByCustomerId(customer.getId()))
                    .sum();
            distribution.setLicenseCount(totalLicenses);

            // 统计验证次数（最近30天）
            LocalDateTime thirtyDaysAgo = getCurrentTime().minusDays(30);
            long totalValidations = regionCustomers.stream()
                    .flatMap(customer -> licenseRepository.findByCustomerIdAndDeletedFalse(customer.getId()).stream())
                    .mapToLong(license -> validationLogRepository.countByLicenseIdAndTimeBetween(
                            license.getId(), thirtyDaysAgo, getCurrentTime()))
                    .sum();
            distribution.setValidationCount(totalValidations);

            distributions.add(distribution);
        }

        // 按客户数量排序
        distributions.sort((a, b) -> Long.compare(b.getCustomerCount(), a.getCustomerCount()));
        return distributions;
    }

    /**
     * 获取实时监控数据
     */
    public Map<String, Object> getRealTimeMonitoringData() {
        log.info("获取实时监控数据");

        Map<String, Object> monitoringData = new HashMap<>();
        LocalDateTime now = getCurrentTime();

        // 在线授权数量
        long onlineLicenses = getActiveLicenses(now);
        monitoringData.put("onlineLicenses", onlineLicenses);

        // 最近1小时验证次数
        LocalDateTime oneHourAgo = now.minusHours(1);
        List<LicenseValidationLog> recentValidations = validationLogRepository.findByValidationTimeBetween(
                oneHourAgo, now);
        monitoringData.put("recentValidations", recentValidations.size());

        // 验证成功率
        long successCount = recentValidations.stream()
                .filter(log -> log.getValidationStatus() == LicenseValidationLog.ValidationStatus.SUCCESS)
                .count();
        double successRate = recentValidations.isEmpty() ? 0.0 : 
                           (double) successCount / recentValidations.size() * 100;
        monitoringData.put("validationSuccessRate", Math.round(successRate * 100) / 100.0);

        // 即将过期的授权（7天内）
        long soonExpiring = getExpiringLicenses(now, 7);
        monitoringData.put("soonExpiringLicenses", soonExpiring);

        // 异常验证记录
        List<LicenseValidationLog> failedValidations = validationLogRepository.findFailedValidationsSince(oneHourAgo);
        monitoringData.put("failedValidations", failedValidations.size());

        // 系统状态
        monitoringData.put("systemStatus", "HEALTHY");
        monitoringData.put("lastUpdateTime", now);

        return monitoringData;
    }

    // ============ 私有辅助方法 ============

    /**
     * 获取总客户数
     */
    private Long getTotalCustomers() {
        return customerRepository.count() - customerRepository.countByDeletedTrue();
    }

    /**
     * 获取活跃客户数
     */
    private Long getActiveCustomers() {
        return (long) customerRepository.findByStatusAndDeletedFalse(Customer.CustomerStatus.ACTIVE).size();
    }

    /**
     * 获取总授权数
     */
    private Long getTotalLicenses() {
        return licenseRepository.count() - licenseRepository.countByDeletedTrue();
    }

    /**
     * 获取有效授权数
     */
    private Long getActiveLicenses(LocalDateTime now) {
        List<License> licenses = licenseRepository.findByStatusAndDeletedFalse(License.LicenseStatus.ACTIVE);
        return licenses.stream()
                .filter(license -> license.getExpireTime().isAfter(now))
                .count();
    }

    /**
     * 获取即将过期的授权数
     */
    private Long getExpiringLicenses(LocalDateTime now, int days) {
        LocalDateTime futureTime = now.plusDays(days);
        return (long) licenseRepository.findExpiringLicenses(now, futureTime).size();
    }

    /**
     * 获取已过期的授权数
     */
    private Long getExpiredLicenses(LocalDateTime now) {
        return (long) licenseRepository.findExpiredActiveLicenses(now).size();
    }

    /**
     * 获取今日验证次数
     */
    private Long getTodayValidations(LocalDateTime now) {
        LocalDateTime startOfDay = now.truncatedTo(ChronoUnit.DAYS);
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        return (long) validationLogRepository.findByValidationTimeBetween(startOfDay, endOfDay).size();
    }

    /**
     * 获取本月验证次数
     */
    private Long getMonthValidations(LocalDateTime now) {
        LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        return (long) validationLogRepository.findByValidationTimeBetween(startOfMonth, now).size();
    }

    /**
     * 获取总收入
     */
    private BigDecimal getTotalRevenue() {
        List<License> licenses = licenseRepository.findByDeletedFalse();
        return licenses.stream()
                .filter(license -> license.getPrice() != null)
                .map(License::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取本月收入
     */
    private BigDecimal getMonthRevenue(LocalDateTime now) {
        LocalDateTime startOfMonth = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        List<License> monthLicenses = licenseRepository.findByCreatedTimeBetween(startOfMonth, now);
        return monthLicenses.stream()
                .filter(license -> license.getPrice() != null)
                .map(License::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取客户状态分布
     */
    private Map<String, Long> getCustomerStatusDistribution() {
        List<Object[]> results = customerRepository.countByStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((Customer.CustomerStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取授权状态分布
     */
    private Map<String, Long> getLicenseStatusDistribution() {
        List<Object[]> results = licenseRepository.countByStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((License.LicenseStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取授权类型分布
     */
    private Map<String, Long> getLicenseTypeDistribution() {
        List<Object[]> results = licenseRepository.countByType();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((License.LicenseType) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取验证趋势
     */
    private List<StatisticsResult.DailyValidation> getValidationTrend(LocalDateTime now, int days) {
        List<StatisticsResult.DailyValidation> trend = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            LocalDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<LicenseValidationLog> dayLogs = validationLogRepository.findByValidationTimeBetween(startOfDay, endOfDay);
            
            long totalCount = dayLogs.size();
            long successCount = dayLogs.stream()
                    .filter(log -> log.getValidationStatus() == LicenseValidationLog.ValidationStatus.SUCCESS)
                    .count();
            long failedCount = totalCount - successCount;
            
            StatisticsResult.DailyValidation dailyValidation = new StatisticsResult.DailyValidation(
                    date.format(DATE_FORMATTER), totalCount, successCount, failedCount);
            trend.add(dailyValidation);
        }
        
        return trend;
    }

    /**
     * 获取收入趋势
     */
    private List<StatisticsResult.DailyRevenue> getRevenueTrend(LocalDateTime now, int days) {
        List<StatisticsResult.DailyRevenue> trend = new ArrayList<>();
        
        for (int i = days - 1; i >= 0; i--) {
            LocalDateTime date = now.minusDays(i);
            LocalDateTime startOfDay = date.truncatedTo(ChronoUnit.DAYS);
            LocalDateTime endOfDay = startOfDay.plusDays(1);
            
            List<License> dayLicenses = licenseRepository.findByCreatedTimeBetween(startOfDay, endOfDay);
            
            BigDecimal dayRevenue = dayLicenses.stream()
                    .filter(license -> license.getPrice() != null)
                    .map(License::getPrice)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            
            StatisticsResult.DailyRevenue dailyRevenue = new StatisticsResult.DailyRevenue(
                    date.format(DATE_FORMATTER), dayRevenue, (long) dayLicenses.size());
            trend.add(dailyRevenue);
        }
        
        return trend;
    }

    /**
     * 计算活跃度评分
     */
    private double calculateActivityScore(long licenseCount, long validationCount, LocalDateTime lastValidationTime) {
        double score = 0.0;
        
        // 授权数量权重 (30%)
        score += Math.min(licenseCount * 10, 100) * 0.3;
        
        // 验证频率权重 (40%)
        score += Math.min(validationCount * 2, 100) * 0.4;
        
        // 最近活跃度权重 (30%)
        if (lastValidationTime != null) {
            long daysSinceLastValidation = ChronoUnit.DAYS.between(lastValidationTime, getCurrentTime());
            double recentScore = Math.max(0, 100 - daysSinceLastValidation * 5);
            score += recentScore * 0.3;
        }
        
        return Math.min(score, 100.0);
    }

    /**
     * 提取地区信息
     */
    private String extractRegion(Customer customer) {
        String address = customer.getAddress();
        if (address == null || address.isEmpty()) {
            return "未知地区";
        }
        
        // 简单的地区提取逻辑，实际项目中可能需要更复杂的解析
        if (address.contains("北京")) return "北京市";
        if (address.contains("上海")) return "上海市";
        if (address.contains("广州") || address.contains("深圳")) return "广东省";
        if (address.contains("杭州")) return "浙江省";
        if (address.contains("南京")) return "江苏省";
        if (address.contains("成都")) return "四川省";
        if (address.contains("武汉")) return "湖北省";
        if (address.contains("西安")) return "陕西省";
        
        return "其他地区";
    }
}