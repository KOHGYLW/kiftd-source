package com.enterprise.license.repository;

import com.enterprise.license.entity.LicenseKey;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 授权密钥Repository接口
 * 提供授权密钥数据访问功能
 */
@Repository
public interface LicenseKeyRepository extends JpaRepository<LicenseKey, UUID>, JpaSpecificationExecutor<LicenseKey> {

    /**
     * 根据授权ID查找密钥列表
     * @param licenseId 授权ID
     * @param pageable 分页参数
     * @return 密钥分页列表
     */
    Page<LicenseKey> findByLicenseId(UUID licenseId, Pageable pageable);

    /**
     * 根据授权ID查找密钥列表
     * @param licenseId 授权ID
     * @return 密钥列表
     */
    List<LicenseKey> findByLicenseId(UUID licenseId);

    /**
     * 根据密钥哈希值查找密钥
     * @param keyHash 密钥哈希值
     * @return 密钥信息
     */
    Optional<LicenseKey> findByKeyHash(String keyHash);

    /**
     * 根据授权ID查找活跃的密钥列表
     * @param licenseId 授权ID
     * @return 活跃密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.license.id = :licenseId AND lk.isActive = true AND lk.deleted = false")
    List<LicenseKey> findActiveLicenseKeysByLicenseId(@Param("licenseId") UUID licenseId);

    /**
     * 根据授权ID查找主密钥
     * @param licenseId 授权ID
     * @return 主密钥
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.license.id = :licenseId AND lk.isPrimary = true AND lk.deleted = false")
    Optional<LicenseKey> findPrimaryKeyByLicenseId(@Param("licenseId") UUID licenseId);

    /**
     * 查找活跃的密钥列表
     * @param pageable 分页参数
     * @return 活跃密钥分页列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.isActive = true AND lk.deleted = false")
    Page<LicenseKey> findActiveKeys(Pageable pageable);

    /**
     * 查找已撤销的密钥列表
     * @param pageable 分页参数
     * @return 已撤销密钥分页列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.revokedTime IS NOT NULL AND lk.deleted = false")
    Page<LicenseKey> findRevokedKeys(Pageable pageable);

    /**
     * 根据硬件指纹查找密钥
     * @param hardwareFingerprint 硬件指纹
     * @return 密钥列表
     */
    List<LicenseKey> findByHardwareFingerprint(String hardwareFingerprint);

    /**
     * 根据客户端标识查找密钥
     * @param clientIdentifier 客户端标识
     * @param pageable 分页参数
     * @return 密钥分页列表
     */
    Page<LicenseKey> findByClientIdentifier(String clientIdentifier, Pageable pageable);

    /**
     * 根据密钥版本查找密钥
     * @param keyVersion 密钥版本
     * @param pageable 分页参数
     * @return 密钥分页列表
     */
    Page<LicenseKey> findByKeyVersion(String keyVersion, Pageable pageable);

    /**
     * 检查密钥哈希值是否存在
     * @param keyHash 密钥哈希值
     * @return true如果存在
     */
    boolean existsByKeyHash(String keyHash);

    /**
     * 检查密钥哈希值是否存在（排除指定密钥）
     * @param keyHash 密钥哈希值
     * @param keyId 要排除的密钥ID
     * @return true如果存在
     */
    @Query("SELECT COUNT(lk) > 0 FROM LicenseKey lk WHERE lk.keyHash = :keyHash AND lk.id != :keyId")
    boolean existsByKeyHashAndIdNot(@Param("keyHash") String keyHash, @Param("keyId") UUID keyId);

    /**
     * 统计各版本的密钥数量
     * @return 版本统计结果
     */
    @Query("SELECT lk.keyVersion, COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false GROUP BY lk.keyVersion ORDER BY lk.keyVersion")
    List<Object[]> countByKeyVersion();

    /**
     * 统计活跃和非活跃密钥数量
     * @return 统计结果
     */
    @Query("SELECT lk.isActive, COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false GROUP BY lk.isActive")
    List<Object[]> countByActiveStatus();

    /**
     * 查找最近创建的密钥
     * @param pageable 分页参数
     * @return 密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false ORDER BY lk.generatedTime DESC")
    List<LicenseKey> findRecentKeys(Pageable pageable);

    /**
     * 查找最近使用的密钥
     * @param pageable 分页参数
     * @return 密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false AND lk.lastUsedTime IS NOT NULL ORDER BY lk.lastUsedTime DESC")
    List<LicenseKey> findRecentlyUsedKeys(Pageable pageable);

    /**
     * 查找使用频率高的密钥
     * @param pageable 分页参数
     * @return 密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false ORDER BY lk.usageCount DESC")
    List<LicenseKey> findMostUsedKeys(Pageable pageable);

    /**
     * 查找验证失败次数高的密钥
     * @param pageable 分页参数
     * @return 密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false ORDER BY lk.validationFailureCount DESC")
    List<LicenseKey> findKeysWithMostValidationFailures(Pageable pageable);

    /**
     * 根据授权编号查找密钥列表
     * @param licenseNumber 授权编号
     * @param pageable 分页参数
     * @return 密钥分页列表
     */
    @Query("SELECT lk FROM LicenseKey lk JOIN lk.license l WHERE l.licenseNumber = :licenseNumber AND lk.deleted = false")
    Page<LicenseKey> findByLicenseNumber(@Param("licenseNumber") String licenseNumber, Pageable pageable);

    /**
     * 根据多个条件搜索密钥
     * @param keyword 关键字（搜索客户端标识、硬件指纹）
     * @param isActive 是否活跃（可选）
     * @param keyVersion 密钥版本（可选）
     * @param pageable 分页参数
     * @return 密钥分页列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     lk.clientIdentifier LIKE %:keyword% OR " +
           "     lk.hardwareFingerprint LIKE %:keyword%) " +
           "AND (:isActive IS NULL OR lk.isActive = :isActive) " +
           "AND (:keyVersion IS NULL OR :keyVersion = '' OR lk.keyVersion = :keyVersion)")
    Page<LicenseKey> searchKeys(@Param("keyword") String keyword,
                               @Param("isActive") Boolean isActive,
                               @Param("keyVersion") String keyVersion,
                               Pageable pageable);

    /**
     * 统计总密钥数
     * @return 总密钥数
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false")
    long countTotalKeys();

    /**
     * 统计活跃密钥数
     * @return 活跃密钥数
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false AND lk.isActive = true")
    long countActiveKeys();

    /**
     * 统计已撤销密钥数
     * @return 已撤销密钥数
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false AND lk.revokedTime IS NOT NULL")
    long countRevokedKeys();

    /**
     * 统计主密钥数
     * @return 主密钥数
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.deleted = false AND lk.isPrimary = true")
    long countPrimaryKeys();

    /**
     * 获取所有密钥版本列表
     * @return 密钥版本列表
     */
    @Query("SELECT DISTINCT lk.keyVersion FROM LicenseKey lk WHERE lk.deleted = false AND lk.keyVersion IS NOT NULL ORDER BY lk.keyVersion")
    List<String> findAllKeyVersions();

    /**
     * 获取所有客户端标识列表
     * @return 客户端标识列表
     */
    @Query("SELECT DISTINCT lk.clientIdentifier FROM LicenseKey lk WHERE lk.deleted = false AND lk.clientIdentifier IS NOT NULL ORDER BY lk.clientIdentifier")
    List<String> findAllClientIdentifiers();

    /**
     * 批量撤销密钥
     * @param keyIds 密钥ID列表
     * @param revokedTime 撤销时间
     * @param revocationReason 撤销原因
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.isActive = false, lk.revokedTime = :revokedTime, " +
           "lk.revocationReason = :revocationReason, lk.updatedBy = :updatedBy, lk.updatedTime = :updatedTime " +
           "WHERE lk.id IN :keyIds")
    int batchRevokeKeys(@Param("keyIds") List<UUID> keyIds,
                       @Param("revokedTime") LocalDateTime revokedTime,
                       @Param("revocationReason") String revocationReason,
                       @Param("updatedBy") String updatedBy,
                       @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 批量激活密钥
     * @param keyIds 密钥ID列表
     * @param activatedTime 激活时间
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.isActive = true, lk.activatedTime = :activatedTime, " +
           "lk.revokedTime = null, lk.revocationReason = null, lk.updatedBy = :updatedBy, lk.updatedTime = :updatedTime " +
           "WHERE lk.id IN :keyIds")
    int batchActivateKeys(@Param("keyIds") List<UUID> keyIds,
                         @Param("activatedTime") LocalDateTime activatedTime,
                         @Param("updatedBy") String updatedBy,
                         @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 增加密钥使用次数并更新最后使用时间
     * @param keyId 密钥ID
     * @param lastUsedTime 最后使用时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.usageCount = lk.usageCount + 1, lk.lastUsedTime = :lastUsedTime WHERE lk.id = :keyId")
    int incrementUsageCount(@Param("keyId") UUID keyId, @Param("lastUsedTime") LocalDateTime lastUsedTime);

    /**
     * 增加密钥验证失败次数
     * @param keyId 密钥ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.validationFailureCount = lk.validationFailureCount + 1 WHERE lk.id = :keyId")
    int incrementValidationFailureCount(@Param("keyId") UUID keyId);

    /**
     * 重置密钥验证失败次数
     * @param keyId 密钥ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.validationFailureCount = 0 WHERE lk.id = :keyId")
    int resetValidationFailureCount(@Param("keyId") UUID keyId);

    /**
     * 清除授权的主密钥标记
     * @param licenseId 授权ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.isPrimary = false WHERE lk.license.id = :licenseId")
    int clearPrimaryKeyFlag(@Param("licenseId") UUID licenseId);

    /**
     * 设置主密钥
     * @param keyId 密钥ID
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE LicenseKey lk SET lk.isPrimary = true WHERE lk.id = :keyId")
    int setPrimaryKey(@Param("keyId") UUID keyId);

    /**
     * 查找验证失败次数超过限制的密钥
     * @return 密钥列表
     */
    @Query("SELECT lk FROM LicenseKey lk WHERE lk.deleted = false AND lk.maxValidationFailures > 0 " +
           "AND lk.validationFailureCount >= lk.maxValidationFailures AND lk.isActive = true")
    List<LicenseKey> findKeysExceedingValidationFailureLimit();

    /**
     * 查找指定授权的密钥数量
     * @param licenseId 授权ID
     * @return 密钥数量
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.license.id = :licenseId AND lk.deleted = false")
    long countKeysByLicense(@Param("licenseId") UUID licenseId);

    /**
     * 查找指定授权的活跃密钥数量
     * @param licenseId 授权ID
     * @return 活跃密钥数量
     */
    @Query("SELECT COUNT(lk) FROM LicenseKey lk WHERE lk.license.id = :licenseId AND lk.isActive = true AND lk.deleted = false")
    long countActiveKeysByLicense(@Param("licenseId") UUID licenseId);
}