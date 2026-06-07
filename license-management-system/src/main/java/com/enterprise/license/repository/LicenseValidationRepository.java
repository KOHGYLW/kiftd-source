package com.enterprise.license.repository;

import com.enterprise.license.entity.LicenseValidation;
import com.enterprise.license.enums.ValidationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 授权验证日志Repository接口
 * 提供授权验证日志数据访问功能
 */
@Repository
public interface LicenseValidationRepository extends JpaRepository<LicenseValidation, UUID>, JpaSpecificationExecutor<LicenseValidation> {

    /**
     * 根据授权ID查找验证记录列表
     * @param licenseId 授权ID
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByLicenseId(UUID licenseId, Pageable pageable);

    /**
     * 根据验证结果查找验证记录列表
     * @param validationResult 验证结果
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByValidationResult(ValidationResult validationResult, Pageable pageable);

    /**
     * 根据客户端IP查找验证记录列表
     * @param clientIp 客户端IP
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 根据硬件指纹查找验证记录列表
     * @param hardwareFingerprint 硬件指纹
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByHardwareFingerprint(String hardwareFingerprint, Pageable pageable);

    /**
     * 查找成功的验证记录
     * @param pageable 分页参数
     * @return 成功验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.validationResult = 'SUCCESS' AND lv.deleted = false")
    Page<LicenseValidation> findSuccessfulValidations(Pageable pageable);

    /**
     * 查找失败的验证记录
     * @param pageable 分页参数
     * @return 失败验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.validationResult != 'SUCCESS' AND lv.deleted = false")
    Page<LicenseValidation> findFailedValidations(Pageable pageable);

    /**
     * 查找可疑活动记录
     * @param pageable 分页参数
     * @return 可疑活动记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.isSuspicious = true AND lv.deleted = false")
    Page<LicenseValidation> findSuspiciousValidations(Pageable pageable);

    /**
     * 查找在线验证记录
     * @param pageable 分页参数
     * @return 在线验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.isOnlineValidation = true AND lv.deleted = false")
    Page<LicenseValidation> findOnlineValidations(Pageable pageable);

    /**
     * 查找离线验证记录
     * @param pageable 分页参数
     * @return 离线验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.isOnlineValidation = false AND lv.deleted = false")
    Page<LicenseValidation> findOfflineValidations(Pageable pageable);

    /**
     * 根据时间范围查找验证记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.validationTime BETWEEN :startTime AND :endTime AND lv.deleted = false")
    Page<LicenseValidation> findByValidationTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                       @Param("endTime") LocalDateTime endTime,
                                                       Pageable pageable);

    /**
     * 根据授权编号查找验证记录
     * @param licenseNumber 授权编号
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv JOIN lv.license l WHERE l.licenseNumber = :licenseNumber AND lv.deleted = false")
    Page<LicenseValidation> findByLicenseNumber(@Param("licenseNumber") String licenseNumber, Pageable pageable);

    /**
     * 根据用户标识查找验证记录
     * @param userIdentifier 用户标识
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByUserIdentifier(String userIdentifier, Pageable pageable);

    /**
     * 根据会话ID查找验证记录
     * @param sessionId 会话ID
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findBySessionId(String sessionId, Pageable pageable);

    /**
     * 根据验证方式查找验证记录
     * @param validationMethod 验证方式
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByValidationMethod(String validationMethod, Pageable pageable);

    /**
     * 根据功能模块查找验证记录
     * @param requestedFeature 功能模块
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    Page<LicenseValidation> findByRequestedFeature(String requestedFeature, Pageable pageable);

    /**
     * 统计各验证结果的数量
     * @return 验证结果统计
     */
    @Query("SELECT lv.validationResult, COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false GROUP BY lv.validationResult")
    List<Object[]> countByValidationResult();

    /**
     * 统计各验证方式的数量
     * @return 验证方式统计
     */
    @Query("SELECT lv.validationMethod, COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationMethod IS NOT NULL GROUP BY lv.validationMethod ORDER BY COUNT(lv) DESC")
    List<Object[]> countByValidationMethod();

    /**
     * 统计在线/离线验证数量
     * @return 在线离线统计
     */
    @Query("SELECT lv.isOnlineValidation, COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false GROUP BY lv.isOnlineValidation")
    List<Object[]> countByOnlineStatus();

    /**
     * 查找最近的验证记录
     * @param pageable 分页参数
     * @return 验证记录列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.deleted = false ORDER BY lv.validationTime DESC")
    List<LicenseValidation> findRecentValidations(Pageable pageable);

    /**
     * 查找验证用时最长的记录
     * @param pageable 分页参数
     * @return 验证记录列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationDurationMs IS NOT NULL ORDER BY lv.validationDurationMs DESC")
    List<LicenseValidation> findSlowestValidations(Pageable pageable);

    /**
     * 查找高风险验证记录
     * @param minRiskScore 最小风险评分
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.deleted = false AND lv.riskScore >= :minRiskScore")
    Page<LicenseValidation> findHighRiskValidations(@Param("minRiskScore") Integer minRiskScore, Pageable pageable);

    /**
     * 根据多个条件搜索验证记录
     * @param keyword 关键字（搜索IP、用户标识、错误信息）
     * @param validationResult 验证结果（可选）
     * @param isOnline 是否在线验证（可选）
     * @param isSuspicious 是否可疑（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageable 分页参数
     * @return 验证记录分页列表
     */
    @Query("SELECT lv FROM LicenseValidation lv WHERE lv.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     lv.clientIp LIKE %:keyword% OR " +
           "     lv.userIdentifier LIKE %:keyword% OR " +
           "     lv.failureReason LIKE %:keyword%) " +
           "AND (:validationResult IS NULL OR lv.validationResult = :validationResult) " +
           "AND (:isOnline IS NULL OR lv.isOnlineValidation = :isOnline) " +
           "AND (:isSuspicious IS NULL OR lv.isSuspicious = :isSuspicious) " +
           "AND (:startTime IS NULL OR lv.validationTime >= :startTime) " +
           "AND (:endTime IS NULL OR lv.validationTime <= :endTime)")
    Page<LicenseValidation> searchValidations(@Param("keyword") String keyword,
                                            @Param("validationResult") ValidationResult validationResult,
                                            @Param("isOnline") Boolean isOnline,
                                            @Param("isSuspicious") Boolean isSuspicious,
                                            @Param("startTime") LocalDateTime startTime,
                                            @Param("endTime") LocalDateTime endTime,
                                            Pageable pageable);

    /**
     * 统计总验证次数
     * @return 总验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false")
    long countTotalValidations();

    /**
     * 统计成功验证次数
     * @return 成功验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationResult = 'SUCCESS'")
    long countSuccessfulValidations();

    /**
     * 统计失败验证次数
     * @return 失败验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationResult != 'SUCCESS'")
    long countFailedValidations();

    /**
     * 统计可疑活动次数
     * @return 可疑活动次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.isSuspicious = true")
    long countSuspiciousValidations();

    /**
     * 统计今日验证次数
     * @param startOfDay 今日开始时间
     * @param endOfDay 今日结束时间
     * @return 今日验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationTime BETWEEN :startOfDay AND :endOfDay")
    long countValidationsToday(@Param("startOfDay") LocalDateTime startOfDay, 
                              @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 统计指定授权的验证次数
     * @param licenseId 授权ID
     * @return 验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.license.id = :licenseId AND lv.deleted = false")
    long countValidationsByLicense(@Param("licenseId") UUID licenseId);

    /**
     * 统计指定客户端IP的验证次数
     * @param clientIp 客户端IP
     * @return 验证次数
     */
    @Query("SELECT COUNT(lv) FROM LicenseValidation lv WHERE lv.clientIp = :clientIp AND lv.deleted = false")
    long countValidationsByClientIp(@Param("clientIp") String clientIp);

    /**
     * 获取所有验证方式列表
     * @return 验证方式列表
     */
    @Query("SELECT DISTINCT lv.validationMethod FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationMethod IS NOT NULL ORDER BY lv.validationMethod")
    List<String> findAllValidationMethods();

    /**
     * 获取所有功能模块列表
     * @return 功能模块列表
     */
    @Query("SELECT DISTINCT lv.requestedFeature FROM LicenseValidation lv WHERE lv.deleted = false AND lv.requestedFeature IS NOT NULL ORDER BY lv.requestedFeature")
    List<String> findAllRequestedFeatures();

    /**
     * 获取验证频率最高的IP地址
     * @param pageable 分页参数
     * @return IP地址和验证次数列表
     */
    @Query("SELECT lv.clientIp, COUNT(lv) as count FROM LicenseValidation lv WHERE lv.deleted = false AND lv.clientIp IS NOT NULL GROUP BY lv.clientIp ORDER BY count DESC")
    List<Object[]> findMostActiveIpAddresses(Pageable pageable);

    /**
     * 获取验证频率最高的用户
     * @param pageable 分页参数
     * @return 用户标识和验证次数列表
     */
    @Query("SELECT lv.userIdentifier, COUNT(lv) as count FROM LicenseValidation lv WHERE lv.deleted = false AND lv.userIdentifier IS NOT NULL GROUP BY lv.userIdentifier ORDER BY count DESC")
    List<Object[]> findMostActiveUsers(Pageable pageable);

    /**
     * 计算平均验证用时
     * @return 平均验证用时（毫秒）
     */
    @Query("SELECT AVG(lv.validationDurationMs) FROM LicenseValidation lv WHERE lv.deleted = false AND lv.validationDurationMs IS NOT NULL")
    Double calculateAverageValidationDuration();

    /**
     * 按小时统计验证次数（用于生成图表）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时和验证次数列表
     */
    @Query("SELECT HOUR(lv.validationTime), COUNT(lv) FROM LicenseValidation lv " +
           "WHERE lv.deleted = false AND lv.validationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(lv.validationTime) ORDER BY HOUR(lv.validationTime)")
    List<Object[]> countValidationsByHour(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期统计验证次数（用于生成图表）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日期和验证次数列表
     */
    @Query("SELECT DATE(lv.validationTime), COUNT(lv) FROM LicenseValidation lv " +
           "WHERE lv.deleted = false AND lv.validationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(lv.validationTime) ORDER BY DATE(lv.validationTime)")
    List<Object[]> countValidationsByDate(@Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);
}