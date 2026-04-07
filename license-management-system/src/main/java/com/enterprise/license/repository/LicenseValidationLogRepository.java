package com.enterprise.license.repository;

import com.enterprise.license.entity.LicenseValidationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权验证日志数据访问接口
 */
@Repository
public interface LicenseValidationLogRepository extends JpaRepository<LicenseValidationLog, Long> {

    /**
     * 根据授权ID查找验证日志
     */
    Page<LicenseValidationLog> findByLicenseIdAndDeletedFalse(Long licenseId, Pageable pageable);

    /**
     * 根据客户端IP查找验证日志
     */
    Page<LicenseValidationLog> findByClientIpAndDeletedFalse(String clientIp, Pageable pageable);

    /**
     * 根据验证状态查找日志
     */
    List<LicenseValidationLog> findByValidationStatusAndDeletedFalse(LicenseValidationLog.ValidationStatus status);

    /**
     * 查找指定时间范围内的验证日志
     */
    @Query("SELECT v FROM LicenseValidationLog v WHERE v.validationTime BETWEEN :startTime AND :endTime AND v.deleted = false")
    List<LicenseValidationLog> findByValidationTimeBetween(@Param("startTime") LocalDateTime startTime, 
                                                            @Param("endTime") LocalDateTime endTime);

    /**
     * 统计各验证状态的数量
     */
    @Query("SELECT v.validationStatus, COUNT(v) FROM LicenseValidationLog v WHERE v.deleted = false GROUP BY v.validationStatus")
    List<Object[]> countByValidationStatus();

    /**
     * 统计指定授权的验证次数
     */
    @Query("SELECT COUNT(v) FROM LicenseValidationLog v WHERE v.licenseId = :licenseId AND v.deleted = false")
    long countByLicenseId(@Param("licenseId") Long licenseId);

    /**
     * 统计指定授权在时间范围内的验证次数
     */
    @Query("SELECT COUNT(v) FROM LicenseValidationLog v WHERE v.licenseId = :licenseId AND v.validationTime BETWEEN :startTime AND :endTime AND v.deleted = false")
    long countByLicenseIdAndTimeBetween(@Param("licenseId") Long licenseId, 
                                        @Param("startTime") LocalDateTime startTime, 
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查找指定授权的最近验证记录
     */
    @Query("SELECT v FROM LicenseValidationLog v WHERE v.licenseId = :licenseId AND v.deleted = false ORDER BY v.validationTime DESC")
    List<LicenseValidationLog> findRecentByLicenseId(@Param("licenseId") Long licenseId, Pageable pageable);

    /**
     * 统计每日验证次数
     */
    @Query("SELECT DATE(v.validationTime), COUNT(v) FROM LicenseValidationLog v WHERE v.validationTime BETWEEN :startTime AND :endTime AND v.deleted = false GROUP BY DATE(v.validationTime) ORDER BY DATE(v.validationTime)")
    List<Object[]> countDailyValidations(@Param("startTime") LocalDateTime startTime, 
                                         @Param("endTime") LocalDateTime endTime);

    /**
     * 查找失败的验证记录
     */
    @Query("SELECT v FROM LicenseValidationLog v WHERE v.validationStatus != 'SUCCESS' AND v.validationTime >= :since AND v.deleted = false ORDER BY v.validationTime DESC")
    List<LicenseValidationLog> findFailedValidationsSince(@Param("since") LocalDateTime since);

    /**
     * 统计IP地址的验证次数
     */
    @Query("SELECT v.clientIp, COUNT(v) FROM LicenseValidationLog v WHERE v.validationTime BETWEEN :startTime AND :endTime AND v.deleted = false GROUP BY v.clientIp ORDER BY COUNT(v) DESC")
    List<Object[]> countByClientIp(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime);

    /**
     * 删除指定时间之前的日志
     */
    @Query("DELETE FROM LicenseValidationLog v WHERE v.validationTime < :beforeTime")
    int deleteByValidationTimeBefore(@Param("beforeTime") LocalDateTime beforeTime);
}