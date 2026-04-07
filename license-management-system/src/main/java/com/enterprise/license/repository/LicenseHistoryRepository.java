package com.enterprise.license.repository;

import com.enterprise.license.entity.LicenseHistory;
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
 * 授权历史记录Repository接口
 * 提供授权历史记录数据访问功能
 */
@Repository
public interface LicenseHistoryRepository extends JpaRepository<LicenseHistory, UUID>, JpaSpecificationExecutor<LicenseHistory> {

    /**
     * 根据授权ID查找历史记录列表
     * @param licenseId 授权ID
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByLicenseId(UUID licenseId, Pageable pageable);

    /**
     * 根据操作类型查找历史记录列表
     * @param operationType 操作类型
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByOperationType(String operationType, Pageable pageable);

    /**
     * 根据操作人员查找历史记录列表
     * @param operator 操作人员
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByOperator(String operator, Pageable pageable);

    /**
     * 根据操作结果查找历史记录列表
     * @param operationResult 操作结果
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByOperationResult(String operationResult, Pageable pageable);

    /**
     * 根据操作级别查找历史记录列表
     * @param operationLevel 操作级别
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByOperationLevel(String operationLevel, Pageable pageable);

    /**
     * 查找敏感操作记录
     * @param pageable 分页参数
     * @return 敏感操作记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.isSensitive = true AND lh.deleted = false")
    Page<LicenseHistory> findSensitiveOperations(Pageable pageable);

    /**
     * 查找成功的操作记录
     * @param pageable 分页参数
     * @return 成功操作记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.operationResult = 'SUCCESS' AND lh.deleted = false")
    Page<LicenseHistory> findSuccessfulOperations(Pageable pageable);

    /**
     * 查找失败的操作记录
     * @param pageable 分页参数
     * @return 失败操作记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.operationResult = 'FAILURE' AND lh.deleted = false")
    Page<LicenseHistory> findFailedOperations(Pageable pageable);

    /**
     * 根据时间范围查找历史记录
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.operationTime BETWEEN :startTime AND :endTime AND lh.deleted = false")
    Page<LicenseHistory> findByOperationTimeBetween(@Param("startTime") LocalDateTime startTime,
                                                   @Param("endTime") LocalDateTime endTime,
                                                   Pageable pageable);

    /**
     * 根据授权编号查找历史记录
     * @param licenseNumber 授权编号
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh JOIN lh.license l WHERE l.licenseNumber = :licenseNumber AND lh.deleted = false")
    Page<LicenseHistory> findByLicenseNumber(@Param("licenseNumber") String licenseNumber, Pageable pageable);

    /**
     * 根据客户端IP查找历史记录
     * @param clientIp 客户端IP
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByClientIp(String clientIp, Pageable pageable);

    /**
     * 根据会话ID查找历史记录
     * @param sessionId 会话ID
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findBySessionId(String sessionId, Pageable pageable);

    /**
     * 根据业务流水号查找历史记录
     * @param businessTransactionId 业务流水号
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByBusinessTransactionId(String businessTransactionId, Pageable pageable);

    /**
     * 根据操作来源查找历史记录
     * @param operationSource 操作来源
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByOperationSource(String operationSource, Pageable pageable);

    /**
     * 根据相关实体查找历史记录
     * @param relatedEntityType 相关实体类型
     * @param relatedEntityId 相关实体ID
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByRelatedEntityTypeAndRelatedEntityId(String relatedEntityType, 
                                                                  String relatedEntityId, 
                                                                  Pageable pageable);

    /**
     * 统计各操作类型的数量
     * @return 操作类型统计
     */
    @Query("SELECT lh.operationType, COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false GROUP BY lh.operationType ORDER BY COUNT(lh) DESC")
    List<Object[]> countByOperationType();

    /**
     * 统计各操作人员的操作数量
     * @return 操作人员统计
     */
    @Query("SELECT lh.operator, COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false GROUP BY lh.operator ORDER BY COUNT(lh) DESC")
    List<Object[]> countByOperator();

    /**
     * 统计各操作结果的数量
     * @return 操作结果统计
     */
    @Query("SELECT lh.operationResult, COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false GROUP BY lh.operationResult")
    List<Object[]> countByOperationResult();

    /**
     * 统计各操作级别的数量
     * @return 操作级别统计
     */
    @Query("SELECT lh.operationLevel, COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false GROUP BY lh.operationLevel")
    List<Object[]> countByOperationLevel();

    /**
     * 查找最近的历史记录
     * @param pageable 分页参数
     * @return 历史记录列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.deleted = false ORDER BY lh.operationTime DESC")
    List<LicenseHistory> findRecentHistory(Pageable pageable);

    /**
     * 查找执行时间最长的操作记录
     * @param pageable 分页参数
     * @return 历史记录列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.deleted = false AND lh.executionTimeMs IS NOT NULL ORDER BY lh.executionTimeMs DESC")
    List<LicenseHistory> findSlowestOperations(Pageable pageable);

    /**
     * 查找特定操作人员的最近操作
     * @param operator 操作人员
     * @param pageable 分页参数
     * @return 历史记录列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.operator = :operator AND lh.deleted = false ORDER BY lh.operationTime DESC")
    List<LicenseHistory> findRecentOperationsByOperator(@Param("operator") String operator, 
                                                        Pageable pageable);

    /**
     * 根据多个条件搜索历史记录
     * @param keyword 关键字（搜索操作描述、操作人员、错误信息）
     * @param operationType 操作类型（可选）
     * @param operationResult 操作结果（可选）
     * @param isSensitive 是否敏感操作（可选）
     * @param startTime 开始时间（可选）
     * @param endTime 结束时间（可选）
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     lh.operationDescription LIKE %:keyword% OR " +
           "     lh.operator LIKE %:keyword% OR " +
           "     lh.errorMessage LIKE %:keyword%) " +
           "AND (:operationType IS NULL OR :operationType = '' OR lh.operationType = :operationType) " +
           "AND (:operationResult IS NULL OR :operationResult = '' OR lh.operationResult = :operationResult) " +
           "AND (:isSensitive IS NULL OR lh.isSensitive = :isSensitive) " +
           "AND (:startTime IS NULL OR lh.operationTime >= :startTime) " +
           "AND (:endTime IS NULL OR lh.operationTime <= :endTime)")
    Page<LicenseHistory> searchHistory(@Param("keyword") String keyword,
                                      @Param("operationType") String operationType,
                                      @Param("operationResult") String operationResult,
                                      @Param("isSensitive") Boolean isSensitive,
                                      @Param("startTime") LocalDateTime startTime,
                                      @Param("endTime") LocalDateTime endTime,
                                      Pageable pageable);

    /**
     * 统计总历史记录数
     * @return 总历史记录数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false")
    long countTotalHistory();

    /**
     * 统计成功操作数
     * @return 成功操作数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operationResult = 'SUCCESS'")
    long countSuccessfulOperations();

    /**
     * 统计失败操作数
     * @return 失败操作数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operationResult = 'FAILURE'")
    long countFailedOperations();

    /**
     * 统计敏感操作数
     * @return 敏感操作数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false AND lh.isSensitive = true")
    long countSensitiveOperations();

    /**
     * 统计今日操作数
     * @param startOfDay 今日开始时间
     * @param endOfDay 今日结束时间
     * @return 今日操作数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operationTime BETWEEN :startOfDay AND :endOfDay")
    long countOperationsToday(@Param("startOfDay") LocalDateTime startOfDay, 
                             @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * 统计指定授权的历史记录数
     * @param licenseId 授权ID
     * @return 历史记录数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.license.id = :licenseId AND lh.deleted = false")
    long countHistoryByLicense(@Param("licenseId") UUID licenseId);

    /**
     * 统计指定操作人员的操作数
     * @param operator 操作人员
     * @return 操作数
     */
    @Query("SELECT COUNT(lh) FROM LicenseHistory lh WHERE lh.operator = :operator AND lh.deleted = false")
    long countOperationsByOperator(@Param("operator") String operator);

    /**
     * 获取所有操作类型列表
     * @return 操作类型列表
     */
    @Query("SELECT DISTINCT lh.operationType FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operationType IS NOT NULL ORDER BY lh.operationType")
    List<String> findAllOperationTypes();

    /**
     * 获取所有操作人员列表
     * @return 操作人员列表
     */
    @Query("SELECT DISTINCT lh.operator FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operator IS NOT NULL ORDER BY lh.operator")
    List<String> findAllOperators();

    /**
     * 获取所有操作来源列表
     * @return 操作来源列表
     */
    @Query("SELECT DISTINCT lh.operationSource FROM LicenseHistory lh WHERE lh.deleted = false AND lh.operationSource IS NOT NULL ORDER BY lh.operationSource")
    List<String> findAllOperationSources();

    /**
     * 获取操作最频繁的IP地址
     * @param pageable 分页参数
     * @return IP地址和操作次数列表
     */
    @Query("SELECT lh.clientIp, COUNT(lh) as count FROM LicenseHistory lh WHERE lh.deleted = false AND lh.clientIp IS NOT NULL GROUP BY lh.clientIp ORDER BY count DESC")
    List<Object[]> findMostActiveIpAddresses(Pageable pageable);

    /**
     * 计算平均执行时间
     * @return 平均执行时间（毫秒）
     */
    @Query("SELECT AVG(lh.executionTimeMs) FROM LicenseHistory lh WHERE lh.deleted = false AND lh.executionTimeMs IS NOT NULL")
    Double calculateAverageExecutionTime();

    /**
     * 按小时统计操作次数（用于生成图表）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 小时和操作次数列表
     */
    @Query("SELECT HOUR(lh.operationTime), COUNT(lh) FROM LicenseHistory lh " +
           "WHERE lh.deleted = false AND lh.operationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY HOUR(lh.operationTime) ORDER BY HOUR(lh.operationTime)")
    List<Object[]> countOperationsByHour(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 按日期统计操作次数（用于生成图表）
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 日期和操作次数列表
     */
    @Query("SELECT DATE(lh.operationTime), COUNT(lh) FROM LicenseHistory lh " +
           "WHERE lh.deleted = false AND lh.operationTime BETWEEN :startTime AND :endTime " +
           "GROUP BY DATE(lh.operationTime) ORDER BY DATE(lh.operationTime)")
    List<Object[]> countOperationsByDate(@Param("startTime") LocalDateTime startTime,
                                        @Param("endTime") LocalDateTime endTime);

    /**
     * 查找变更记录（有新旧值的记录）
     * @param pageable 分页参数
     * @return 变更记录分页列表
     */
    @Query("SELECT lh FROM LicenseHistory lh WHERE lh.deleted = false AND lh.oldValue IS NOT NULL AND lh.newValue IS NOT NULL")
    Page<LicenseHistory> findChangeRecords(Pageable pageable);

    /**
     * 根据变更字段查找历史记录
     * @param changedFields 变更字段
     * @param pageable 分页参数
     * @return 历史记录分页列表
     */
    Page<LicenseHistory> findByChangedFields(String changedFields, Pageable pageable);
}