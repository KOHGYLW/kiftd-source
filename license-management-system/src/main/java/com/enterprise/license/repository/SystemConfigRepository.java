package com.enterprise.license.repository;

import com.enterprise.license.entity.SystemConfig;
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
 * 系统配置Repository接口
 * 提供系统配置数据访问功能
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, UUID>, JpaSpecificationExecutor<SystemConfig> {

    /**
     * 根据配置键查找配置
     * @param configKey 配置键
     * @return 配置信息
     */
    Optional<SystemConfig> findByConfigKey(String configKey);

    /**
     * 根据配置键查找有效配置
     * @param configKey 配置键
     * @return 配置信息
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.isActive = true AND sc.deleted = false")
    Optional<SystemConfig> findActiveConfigByKey(@Param("configKey") String configKey);

    /**
     * 根据配置类别查找配置列表
     * @param configCategory 配置类别
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByConfigCategory(String configCategory, Pageable pageable);

    /**
     * 根据配置类别查找有效配置列表
     * @param configCategory 配置类别
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configCategory = :configCategory AND sc.isActive = true AND sc.deleted = false")
    Page<SystemConfig> findActiveConfigsByCategory(@Param("configCategory") String configCategory, Pageable pageable);

    /**
     * 根据配置组查找配置列表
     * @param configGroup 配置组
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByConfigGroup(String configGroup, Pageable pageable);

    /**
     * 根据数据类型查找配置列表
     * @param dataType 数据类型
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByDataType(String dataType, Pageable pageable);

    /**
     * 根据环境查找配置列表
     * @param environment 环境
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByEnvironment(String environment, Pageable pageable);

    /**
     * 根据应用名称查找配置列表
     * @param applicationName 应用名称
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByApplicationName(String applicationName, Pageable pageable);

    /**
     * 查找所有有效配置
     * @param pageable 分页参数
     * @return 有效配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isActive = true AND sc.deleted = false")
    Page<SystemConfig> findActiveConfigs(Pageable pageable);

    /**
     * 查找所有系统内置配置
     * @param pageable 分页参数
     * @return 系统配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isSystem = true AND sc.deleted = false")
    Page<SystemConfig> findSystemConfigs(Pageable pageable);

    /**
     * 查找所有只读配置
     * @param pageable 分页参数
     * @return 只读配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isReadonly = true AND sc.deleted = false")
    Page<SystemConfig> findReadonlyConfigs(Pageable pageable);

    /**
     * 查找所有敏感配置
     * @param pageable 分页参数
     * @return 敏感配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isSensitive = true AND sc.deleted = false")
    Page<SystemConfig> findSensitiveConfigs(Pageable pageable);

    /**
     * 查找当前有效的配置（在有效期内）
     * @param currentTime 当前时间
     * @param pageable 分页参数
     * @return 有效配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isActive = true AND sc.deleted = false " +
           "AND (sc.effectiveTime IS NULL OR sc.effectiveTime <= :currentTime) " +
           "AND (sc.expiryTime IS NULL OR sc.expiryTime > :currentTime)")
    Page<SystemConfig> findValidConfigs(@Param("currentTime") LocalDateTime currentTime, Pageable pageable);

    /**
     * 查找即将过期的配置
     * @param currentTime 当前时间
     * @param expiryThreshold 过期阈值时间
     * @return 即将过期的配置列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isActive = true AND sc.deleted = false " +
           "AND sc.expiryTime IS NOT NULL AND sc.expiryTime BETWEEN :currentTime AND :expiryThreshold")
    List<SystemConfig> findConfigsExpiringSoon(@Param("currentTime") LocalDateTime currentTime,
                                              @Param("expiryThreshold") LocalDateTime expiryThreshold);

    /**
     * 查找已过期的配置
     * @param currentTime 当前时间
     * @return 已过期的配置列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.isActive = true AND sc.deleted = false " +
           "AND sc.expiryTime IS NOT NULL AND sc.expiryTime < :currentTime")
    List<SystemConfig> findExpiredConfigs(@Param("currentTime") LocalDateTime currentTime);

    /**
     * 根据配置名称模糊查询
     * @param configName 配置名称关键字
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    Page<SystemConfig> findByConfigNameContainingIgnoreCase(String configName, Pageable pageable);

    /**
     * 根据标签查找配置
     * @param tag 标签
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.deleted = false AND sc.tags LIKE %:tag%")
    Page<SystemConfig> findByTag(@Param("tag") String tag, Pageable pageable);

    /**
     * 检查配置键是否存在
     * @param configKey 配置键
     * @return true如果存在
     */
    boolean existsByConfigKey(String configKey);

    /**
     * 检查配置键是否存在（排除指定配置）
     * @param configKey 配置键
     * @param configId 要排除的配置ID
     * @return true如果存在
     */
    @Query("SELECT COUNT(sc) > 0 FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.id != :configId")
    boolean existsByConfigKeyAndIdNot(@Param("configKey") String configKey, @Param("configId") UUID configId);

    /**
     * 统计各类别的配置数量
     * @return 类别统计结果
     */
    @Query("SELECT sc.configCategory, COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false GROUP BY sc.configCategory ORDER BY COUNT(sc) DESC")
    List<Object[]> countByCategory();

    /**
     * 统计各数据类型的配置数量
     * @return 数据类型统计结果
     */
    @Query("SELECT sc.dataType, COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false GROUP BY sc.dataType")
    List<Object[]> countByDataType();

    /**
     * 统计各环境的配置数量
     * @return 环境统计结果
     */
    @Query("SELECT sc.environment, COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false GROUP BY sc.environment")
    List<Object[]> countByEnvironment();

    /**
     * 统计活跃和非活跃配置数量
     * @return 统计结果
     */
    @Query("SELECT sc.isActive, COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false GROUP BY sc.isActive")
    List<Object[]> countByActiveStatus();

    /**
     * 查找最近创建的配置
     * @param pageable 分页参数
     * @return 配置列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.deleted = false ORDER BY sc.createdTime DESC")
    List<SystemConfig> findRecentConfigs(Pageable pageable);

    /**
     * 查找最近修改的配置
     * @param pageable 分页参数
     * @return 配置列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.deleted = false AND sc.lastModifiedTime IS NOT NULL ORDER BY sc.lastModifiedTime DESC")
    List<SystemConfig> findRecentlyModifiedConfigs(Pageable pageable);

    /**
     * 根据多个条件搜索配置
     * @param keyword 关键字（搜索配置键、名称、描述）
     * @param configCategory 配置类别（可选）
     * @param dataType 数据类型（可选）
     * @param isActive 是否活跃（可选）
     * @param isSystem 是否系统配置（可选）
     * @param environment 环境（可选）
     * @param pageable 分页参数
     * @return 配置分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.deleted = false " +
           "AND (:keyword IS NULL OR :keyword = '' OR " +
           "     sc.configKey LIKE %:keyword% OR " +
           "     sc.configName LIKE %:keyword% OR " +
           "     sc.configDescription LIKE %:keyword%) " +
           "AND (:configCategory IS NULL OR :configCategory = '' OR sc.configCategory = :configCategory) " +
           "AND (:dataType IS NULL OR :dataType = '' OR sc.dataType = :dataType) " +
           "AND (:isActive IS NULL OR sc.isActive = :isActive) " +
           "AND (:isSystem IS NULL OR sc.isSystem = :isSystem) " +
           "AND (:environment IS NULL OR :environment = '' OR sc.environment = :environment)")
    Page<SystemConfig> searchConfigs(@Param("keyword") String keyword,
                                    @Param("configCategory") String configCategory,
                                    @Param("dataType") String dataType,
                                    @Param("isActive") Boolean isActive,
                                    @Param("isSystem") Boolean isSystem,
                                    @Param("environment") String environment,
                                    Pageable pageable);

    /**
     * 统计总配置数
     * @return 总配置数
     */
    @Query("SELECT COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false")
    long countTotalConfigs();

    /**
     * 统计活跃配置数
     * @return 活跃配置数
     */
    @Query("SELECT COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false AND sc.isActive = true")
    long countActiveConfigs();

    /**
     * 统计系统配置数
     * @return 系统配置数
     */
    @Query("SELECT COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false AND sc.isSystem = true")
    long countSystemConfigs();

    /**
     * 统计敏感配置数
     * @return 敏感配置数
     */
    @Query("SELECT COUNT(sc) FROM SystemConfig sc WHERE sc.deleted = false AND sc.isSensitive = true")
    long countSensitiveConfigs();

    /**
     * 获取所有配置类别列表
     * @return 配置类别列表
     */
    @Query("SELECT DISTINCT sc.configCategory FROM SystemConfig sc WHERE sc.deleted = false AND sc.configCategory IS NOT NULL ORDER BY sc.configCategory")
    List<String> findAllCategories();

    /**
     * 获取所有配置组列表
     * @return 配置组列表
     */
    @Query("SELECT DISTINCT sc.configGroup FROM SystemConfig sc WHERE sc.deleted = false AND sc.configGroup IS NOT NULL ORDER BY sc.configGroup")
    List<String> findAllConfigGroups();

    /**
     * 获取所有数据类型列表
     * @return 数据类型列表
     */
    @Query("SELECT DISTINCT sc.dataType FROM SystemConfig sc WHERE sc.deleted = false AND sc.dataType IS NOT NULL ORDER BY sc.dataType")
    List<String> findAllDataTypes();

    /**
     * 获取所有环境列表
     * @return 环境列表
     */
    @Query("SELECT DISTINCT sc.environment FROM SystemConfig sc WHERE sc.deleted = false AND sc.environment IS NOT NULL ORDER BY sc.environment")
    List<String> findAllEnvironments();

    /**
     * 获取所有应用名称列表
     * @return 应用名称列表
     */
    @Query("SELECT DISTINCT sc.applicationName FROM SystemConfig sc WHERE sc.deleted = false AND sc.applicationName IS NOT NULL ORDER BY sc.applicationName")
    List<String> findAllApplicationNames();

    /**
     * 批量更新配置状态
     * @param configIds 配置ID列表
     * @param isActive 新状态
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE SystemConfig sc SET sc.isActive = :isActive, sc.lastModifiedBy = :updatedBy, sc.lastModifiedTime = :updatedTime " +
           "WHERE sc.id IN :configIds")
    int batchUpdateActiveStatus(@Param("configIds") List<UUID> configIds,
                               @Param("isActive") Boolean isActive,
                               @Param("updatedBy") String updatedBy,
                               @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 批量更新配置值
     * @param configKey 配置键
     * @param configValue 新配置值
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE SystemConfig sc SET sc.configValue = :configValue, sc.lastModifiedBy = :updatedBy, " +
           "sc.lastModifiedTime = :updatedTime, sc.version = sc.version + 1 WHERE sc.configKey = :configKey")
    int updateConfigValue(@Param("configKey") String configKey,
                         @Param("configValue") String configValue,
                         @Param("updatedBy") String updatedBy,
                         @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 清理过期配置（设置为非活跃状态）
     * @param currentTime 当前时间
     * @param updatedBy 更新人
     * @param updatedTime 更新时间
     * @return 更新的记录数
     */
    @Modifying
    @Query("UPDATE SystemConfig sc SET sc.isActive = false, sc.lastModifiedBy = :updatedBy, sc.lastModifiedTime = :updatedTime " +
           "WHERE sc.expiryTime < :currentTime AND sc.isActive = true AND sc.deleted = false")
    int deactivateExpiredConfigs(@Param("currentTime") LocalDateTime currentTime,
                                @Param("updatedBy") String updatedBy,
                                @Param("updatedTime") LocalDateTime updatedTime);

    /**
     * 根据优先级排序查找配置
     * @param configCategory 配置类别
     * @return 排序后的配置列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configCategory = :configCategory AND sc.isActive = true AND sc.deleted = false ORDER BY sc.priority ASC, sc.sortOrder ASC")
    List<SystemConfig> findConfigsByCategoryOrderByPriority(@Param("configCategory") String configCategory);

    /**
     * 获取配置的所有版本历史
     * @param configKey 配置键
     * @param pageable 分页参数
     * @return 配置历史分页列表
     */
    @Query("SELECT sc FROM SystemConfig sc WHERE sc.configKey = :configKey AND sc.deleted = false ORDER BY sc.version DESC")
    Page<SystemConfig> findConfigVersionHistory(@Param("configKey") String configKey, Pageable pageable);

    /**
     * 查找配置冲突（相同键但不同值）
     * @return 冲突配置列表
     */
    @Query("SELECT sc1 FROM SystemConfig sc1 WHERE sc1.deleted = false AND sc1.isActive = true " +
           "AND EXISTS (SELECT sc2 FROM SystemConfig sc2 WHERE sc2.deleted = false AND sc2.isActive = true " +
           "AND sc2.configKey = sc1.configKey AND sc2.configValue != sc1.configValue AND sc2.id != sc1.id)")
    List<SystemConfig> findConflictingConfigs();
}