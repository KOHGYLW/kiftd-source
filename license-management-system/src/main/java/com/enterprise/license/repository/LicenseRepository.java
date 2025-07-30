package com.enterprise.license.repository;

import com.enterprise.license.entity.License;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 授权Repository接口
 * 提供授权数据访问功能
 */
@Repository
public interface LicenseRepository extends JpaRepository<License, UUID>, JpaSpecificationExecutor<License> {

    /**
     * 根据授权编号查找授权
     * @param licenseNumber 授权编号
     * @return 授权信息
     */
    Optional<License> findByLicenseNumber(String licenseNumber);

    /**
     * 根据客户ID查找授权列表
     * @param customerId 客户ID
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByCustomerId(UUID customerId, Pageable pageable);

    /**
     * 根据客户ID和状态查找授权列表
     * @param customerId 客户ID
     * @param status 授权状态
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByCustomerIdAndStatus(UUID customerId, LicenseStatus status, Pageable pageable);

    /**
     * 根据授权状态查找授权列表
     * @param status 授权状态
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByStatus(LicenseStatus status, Pageable pageable);

    /**
     * 根据授权类型查找授权列表
     * @param licenseType 授权类型
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByLicenseType(LicenseType licenseType, Pageable pageable);

    /**
     * 根据产品名称查找授权列表
     * @param productName 产品名称
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByProductName(String productName, Pageable pageable);

    /**
     * 根据产品名称和版本查找授权列表
     * @param productName 产品名称
     * @param productVersion 产品版本
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    Page<License> findByProductNameAndProductVersion(String productName, String productVersion, Pageable pageable);

    /**
     * 查找活跃授权列表
     * @param pageable 分页参数
     * @return 活跃授权分页列表
     */
    @Query("SELECT l FROM License l WHERE l.status = 'ACTIVE' AND l.deleted = false")
    Page<License> findActiveLicenses(Pageable pageable);

    /**
     * 查找即将到期的授权
     * @param fromDate 开始日期
     * @param toDate 结束日期
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.expireDate BETWEEN :fromDate AND :toDate AND l.status = 'ACTIVE' AND l.deleted = false")
    List<License> findLicensesExpiringSoon(@Param("fromDate") LocalDate fromDate, 
                                          @Param("toDate") LocalDate toDate);

    /**
     * 查找已过期的授权
     * @param currentDate 当前日期
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.expireDate < :currentDate AND l.status = 'ACTIVE' AND l.deleted = false")
    List<License> findExpiredLicenses(@Param("currentDate") LocalDate currentDate);

    /**
     * 查找永久授权列表
     * @param pageable 分页参数
     * @return 永久授权分页列表
     */
    @Query("SELECT l FROM License l WHERE l.licenseType = 'PERPETUAL' AND l.deleted = false")
    Page<License> findPerpetualLicenses(Pageable pageable);

    /**
     * 查找试用版授权列表
     * @param pageable 分页参数
     * @return 试用版授权分页列表
     */
    @Query("SELECT l FROM License l WHERE l.licenseType = 'TRIAL' AND l.deleted = false")
    Page<License> findTrialLicenses(Pageable pageable);

    /**
     * 根据硬件指纹查找授权
     * @param hardwareFingerprint 硬件指纹
     * @return 授权列表
     */
    List<License> findByHardwareFingerprint(String hardwareFingerprint);

    /**
     * 检查授权编号是否存在
     * @param licenseNumber 授权编号
     * @return true如果存在
     */
    boolean existsByLicenseNumber(String licenseNumber);

    /**
     * 检查授权编号是否存在（排除指定授权）
     * @param licenseNumber 授权编号
     * @param licenseId 要排除的授权ID
     * @return true如果存在
     */
    @Query("SELECT COUNT(l) > 0 FROM License l WHERE l.licenseNumber = :licenseNumber AND l.id != :licenseId")
    boolean existsByLicenseNumberAndIdNot(@Param("licenseNumber") String licenseNumber, 
                                         @Param("licenseId") UUID licenseId);

    /**
     * 统计各状态的授权数量
     * @return 状态统计结果
     */
    @Query("SELECT l.status, COUNT(l) FROM License l WHERE l.deleted = false GROUP BY l.status")
    List<Object[]> countByStatus();

    /**
     * 统计各类型的授权数量
     * @return 类型统计结果
     */
    @Query("SELECT l.licenseType, COUNT(l) FROM License l WHERE l.deleted = false GROUP BY l.licenseType")
    List<Object[]> countByLicenseType();

    /**
     * 统计各产品的授权数量
     * @return 产品统计结果
     */
    @Query("SELECT l.productName, COUNT(l) FROM License l WHERE l.deleted = false GROUP BY l.productName ORDER BY COUNT(l) DESC")
    List<Object[]> countByProduct();

    /**
     * 查找最近创建的授权
     * @param pageable 分页参数
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.deleted = false ORDER BY l.createdTime DESC")
    List<License> findRecentLicenses(Pageable pageable);

    /**
     * 查找最近验证的授权
     * @param pageable 分页参数
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.deleted = false AND l.lastValidationTime IS NOT NULL ORDER BY l.lastValidationTime DESC")
    List<License> findRecentlyValidatedLicenses(Pageable pageable);

    /**
     * 查找使用频率高的授权
     * @param pageable 分页参数
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.deleted = false ORDER BY l.validationCount DESC, l.currentUsageCount DESC")
    List<License> findMostUsedLicenses(Pageable pageable);

    /**
     * 根据客户编码查找授权列表
     * @param customerCode 客户编码
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    @Query("SELECT l FROM License l JOIN l.customer c WHERE c.customerCode = :customerCode AND l.deleted = false")
    Page<License> findByCustomerCode(@Param("customerCode") String customerCode, Pageable pageable);

    /**
     * 根据多个条件搜索授权
     * @param keyword 关键字（搜索授权编号、产品名称、客户名称）
     * @param status 授权状态（可选）
     * @param licenseType 授权类型（可选）
     * @param productName 产品名称（可选）
     * @param pageable 分页参数
     * @return 授权分页列表
     */
    @Query("SELECT l FROM License l JOIN l.customer c WHERE l.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     l.licenseNumber LIKE %:keyword% OR " +
           "     l.productName LIKE %:keyword% OR " +
           "     c.customerName LIKE %:keyword%) " +
           "AND (:status IS NULL OR l.status = :status) " +
           "AND (:licenseType IS NULL OR l.licenseType = :licenseType) " +
           "AND (:productName IS NULL OR :productName = '' OR l.productName = :productName)")
    Page<License> searchLicenses(@Param("keyword") String keyword,
                                @Param("status") LicenseStatus status,
                                @Param("licenseType") LicenseType licenseType,
                                @Param("productName") String productName,
                                Pageable pageable);

    /**
     * 统计总授权数
     * @return 总授权数
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.deleted = false")
    long countTotalLicenses();

    /**
     * 统计活跃授权数
     * @return 活跃授权数
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.deleted = false AND l.status = 'ACTIVE'")
    long countActiveLicenses();

    /**
     * 统计即将到期的授权数（30天内）
     * @param date30DaysLater 30天后的日期
     * @return 即将到期的授权数
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.deleted = false AND l.status = 'ACTIVE' AND l.expireDate BETWEEN CURRENT_DATE AND :date30DaysLater")
    long countLicensesExpiringSoon(@Param("date30DaysLater") LocalDate date30DaysLater);

    /**
     * 统计已过期的授权数
     * @return 已过期的授权数
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.deleted = false AND l.expireDate < CURRENT_DATE AND l.licenseType != 'PERPETUAL'")
    long countExpiredLicenses();

    /**
     * 统计本月新增授权数
     * @param startOfMonth 月初日期
     * @param endOfMonth 月末日期
     * @return 新增授权数
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.deleted = false AND l.createdTime BETWEEN :startOfMonth AND :endOfMonth")
    long countNewLicensesThisMonth(@Param("startOfMonth") LocalDate startOfMonth, 
                                  @Param("endOfMonth") LocalDate endOfMonth);

    /**
     * 获取所有产品名称列表
     * @return 产品名称列表
     */
    @Query("SELECT DISTINCT l.productName FROM License l WHERE l.deleted = false AND l.productName IS NOT NULL ORDER BY l.productName")
    List<String> findAllProductNames();

    /**
     * 根据产品名称获取所有版本列表
     * @param productName 产品名称
     * @return 版本列表
     */
    @Query("SELECT DISTINCT l.productVersion FROM License l WHERE l.deleted = false AND l.productName = :productName AND l.productVersion IS NOT NULL ORDER BY l.productVersion")
    List<String> findAllVersionsByProduct(@Param("productName") String productName);

    /**
     * 批量更新授权状态
     * @param ids 授权ID列表
     * @param status 新状态
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE License l SET l.status = :status, l.updatedBy = :updatedBy, l.updatedTime = :updatedTime WHERE l.id IN :ids")
    int batchUpdateStatus(@Param("ids") List<UUID> ids, 
                         @Param("status") LicenseStatus status,
                         @Param("updatedBy") String updatedBy,
                         @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 批量更新过期授权状态为已过期
     * @param currentDate 当前日期
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE License l SET l.status = 'EXPIRED', l.updatedBy = :updatedBy, l.updatedTime = :updatedTime " +
           "WHERE l.expireDate < :currentDate AND l.status = 'ACTIVE' AND l.licenseType != 'PERPETUAL' AND l.deleted = false")
    int batchUpdateExpiredLicenses(@Param("currentDate") LocalDate currentDate,
                                  @Param("updatedBy") String updatedBy,
                                  @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 增加授权使用次数
     * @param licenseId 授权ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE License l SET l.currentUsageCount = l.currentUsageCount + 1 WHERE l.id = :licenseId")
    int incrementUsageCount(@Param("licenseId") UUID licenseId);

    /**
     * 增加授权验证次数并更新最后验证时间
     * @param licenseId 授权ID
     * @param lastValidationTime 最后验证时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE License l SET l.validationCount = l.validationCount + 1, l.lastValidationTime = :lastValidationTime WHERE l.id = :licenseId")
    int incrementValidationCount(@Param("licenseId") UUID licenseId, 
                               @Param("lastValidationTime") LocalDateTime lastValidationTime);

    /**
     * 查找指定客户的有效授权数量
     * @param customerId 客户ID
     * @return 有效授权数量
     */
    @Query("SELECT COUNT(l) FROM License l WHERE l.customer.id = :customerId AND l.status = 'ACTIVE' AND l.deleted = false")
    long countActiveLicensesByCustomer(@Param("customerId") UUID customerId);

    /**
     * 查找使用次数超过限制的授权
     * @return 授权列表
     */
    @Query("SELECT l FROM License l WHERE l.deleted = false AND l.maxUsageCount > 0 AND l.currentUsageCount >= l.maxUsageCount")
    List<License> findLicensesExceedingUsageLimit();
}