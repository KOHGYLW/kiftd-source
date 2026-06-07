package com.enterprise.license.service;

import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.dto.QueryParam;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 授权管理服务
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class LicenseService extends BaseService {

    private final LicenseRepository licenseRepository;
    private final CustomerRepository customerRepository;

    /**
     * 创建授权
     */
    @CacheEvict(value = "licenses", allEntries = true)
    public LicenseDto createLicense(LicenseDto licenseDto) {
        log.info("创建授权: {}", licenseDto.getProductName());

        // 验证客户存在
        Customer customer = customerRepository.findById(licenseDto.getCustomerId())
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + licenseDto.getCustomerId()));

        if (customer.getDeleted()) {
            throw new IllegalArgumentException("客户已被删除: " + licenseDto.getCustomerId());
        }

        // 验证授权编码唯一性
        if (StringUtils.isNotBlank(licenseDto.getLicenseCode()) &&
                licenseRepository.existsByLicenseCodeAndDeletedFalse(licenseDto.getLicenseCode())) {
            throw new IllegalArgumentException("授权编码已存在: " + licenseDto.getLicenseCode());
        }

        // 生成授权编码
        if (StringUtils.isBlank(licenseDto.getLicenseCode())) {
            licenseDto.setLicenseCode(generateLicenseCode());
        }

        // 验证时间有效性
        validateLicenseTime(licenseDto.getStartTime(), licenseDto.getExpireTime());

        License license = convertToEntity(licenseDto);
        license.setCreatedTime(getCurrentTime());
        license.setCreatedBy("system"); // TODO: 从安全上下文获取当前用户

        License savedLicense = licenseRepository.save(license);
        logOperation("创建授权", "授权ID: " + savedLicense.getId());

        return convertToDto(savedLicense);
    }

    /**
     * 更新授权信息
     */
    @CacheEvict(value = "licenses", allEntries = true)
    public LicenseDto updateLicense(Long id, LicenseDto licenseDto) {
        log.info("更新授权: {}", id);

        License existingLicense = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (existingLicense.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        // 验证授权编码唯一性（排除当前授权）
        if (StringUtils.isNotBlank(licenseDto.getLicenseCode()) &&
                !licenseDto.getLicenseCode().equals(existingLicense.getLicenseCode()) &&
                licenseRepository.existsByLicenseCodeAndDeletedFalse(licenseDto.getLicenseCode())) {
            throw new IllegalArgumentException("授权编码已存在: " + licenseDto.getLicenseCode());
        }

        // 验证时间有效性
        if (licenseDto.getStartTime() != null && licenseDto.getExpireTime() != null) {
            validateLicenseTime(licenseDto.getStartTime(), licenseDto.getExpireTime());
        }

        // 更新字段
        updateLicenseFields(existingLicense, licenseDto);
        existingLicense.setUpdatedTime(getCurrentTime());
        existingLicense.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(existingLicense);
        logOperation("更新授权", "授权ID: " + updatedLicense.getId());

        return convertToDto(updatedLicense);
    }

    /**
     * 根据ID查找授权
     */
    @Cacheable(value = "licenses", key = "#id")
    public LicenseDto findById(Long id) {
        log.debug("查找授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        return convertToDto(license);
    }

    /**
     * 根据授权编码查找授权
     */
    @Cacheable(value = "licenses", key = "#licenseCode")
    public LicenseDto findByLicenseCode(String licenseCode) {
        log.debug("根据授权编码查找授权: {}", licenseCode);

        License license = licenseRepository.findByLicenseCodeAndDeletedFalse(licenseCode)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + licenseCode));

        return convertToDto(license);
    }

    /**
     * 分页查询授权
     */
    public Page<LicenseDto> findLicenses(QueryParam.LicenseQueryParam queryParam) {
        log.debug("分页查询授权: {}", queryParam);

        Specification<License> spec = buildLicenseSpecification(queryParam);
        Pageable pageable = buildPageable(queryParam);

        Page<License> licensePage = licenseRepository.findAll(spec, pageable);

        return licensePage.map(this::convertToDto);
    }

    /**
     * 根据客户ID查找授权
     */
    public List<LicenseDto> findByCustomerId(Long customerId) {
        log.debug("根据客户ID查找授权: {}", customerId);

        List<License> licenses = licenseRepository.findByCustomerIdAndDeletedFalse(customerId);
        return licenses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 激活授权
     */
    @CacheEvict(value = "licenses", key = "#id")
    public LicenseDto activateLicense(Long id) {
        log.info("激活授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        if (license.getExpireTime().isBefore(getCurrentTime())) {
            throw new IllegalArgumentException("授权已过期，无法激活");
        }

        license.setStatus(License.LicenseStatus.ACTIVE);
        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(license);
        logOperation("激活授权", "授权ID: " + id);

        return convertToDto(updatedLicense);
    }

    /**
     * 暂停授权
     */
    @CacheEvict(value = "licenses", key = "#id")
    public LicenseDto suspendLicense(Long id) {
        log.info("暂停授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        license.setStatus(License.LicenseStatus.SUSPENDED);
        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(license);
        logOperation("暂停授权", "授权ID: " + id);

        return convertToDto(updatedLicense);
    }

    /**
     * 撤销授权
     */
    @CacheEvict(value = "licenses", key = "#id")
    public LicenseDto revokeLicense(Long id) {
        log.info("撤销授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        license.setStatus(License.LicenseStatus.REVOKED);
        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(license);
        logOperation("撤销授权", "授权ID: " + id);

        return convertToDto(updatedLicense);
    }

    /**
     * 续期授权
     */
    @CacheEvict(value = "licenses", key = "#id")
    public LicenseDto renewLicense(Long id, LocalDateTime newExpireTime) {
        log.info("续期授权: {} -> {}", id, newExpireTime);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        if (newExpireTime.isBefore(getCurrentTime())) {
            throw new IllegalArgumentException("新的到期时间不能早于当前时间");
        }

        if (newExpireTime.isBefore(license.getExpireTime())) {
            throw new IllegalArgumentException("新的到期时间不能早于原到期时间");
        }

        license.setExpireTime(newExpireTime);
        license.setStatus(License.LicenseStatus.ACTIVE);
        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(license);
        logOperation("续期授权", String.format("授权ID: %d, 新到期时间: %s", id, newExpireTime));

        return convertToDto(updatedLicense);
    }

    /**
     * 升级授权
     */
    @CacheEvict(value = "licenses", key = "#id")
    public LicenseDto upgradeLicense(Long id, License.LicenseType newType, Integer newMaxUsers, String newFeatures) {
        log.info("升级授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        if (license.getStatus() != License.LicenseStatus.ACTIVE) {
            throw new IllegalArgumentException("只能升级激活状态的授权");
        }

        if (newType != null) {
            license.setLicenseType(newType);
        }
        if (newMaxUsers != null && newMaxUsers > 0) {
            license.setMaxUsers(newMaxUsers);
        }
        if (StringUtils.isNotBlank(newFeatures)) {
            license.setFeatures(newFeatures);
        }

        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        License updatedLicense = licenseRepository.save(license);
        logOperation("升级授权", "授权ID: " + id);

        return convertToDto(updatedLicense);
    }

    /**
     * 删除授权（软删除）
     */
    @CacheEvict(value = "licenses", allEntries = true)
    public void deleteLicense(Long id) {
        log.info("删除授权: {}", id);

        License license = licenseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("授权不存在: " + id));

        if (license.getDeleted()) {
            throw new IllegalArgumentException("授权已被删除: " + id);
        }

        license.setDeleted(true);
        license.setStatus(License.LicenseStatus.REVOKED);
        license.setUpdatedTime(getCurrentTime());
        license.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        licenseRepository.save(license);
        logOperation("删除授权", "授权ID: " + id);
    }

    /**
     * 批量更新授权状态
     */
    @CacheEvict(value = "licenses", allEntries = true)
    public int batchUpdateStatus(List<Long> ids, License.LicenseStatus status) {
        log.info("批量更新授权状态: {} -> {}", ids, status);

        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("授权ID列表不能为空");
        }

        int updatedCount = licenseRepository.batchUpdateStatus(
                ids, status, getCurrentTime(), "system"); // TODO: 从安全上下文获取当前用户

        logOperation("批量更新授权状态", String.format("更新数量: %d, 状态: %s", updatedCount, status));

        return updatedCount;
    }

    /**
     * 获取授权状态统计
     */
    @Cacheable(value = "license_statistics", key = "'status_count'")
    public Map<String, Long> getStatusStatistics() {
        log.debug("获取授权状态统计");

        List<Object[]> results = licenseRepository.countByStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((License.LicenseStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取授权类型统计
     */
    @Cacheable(value = "license_statistics", key = "'type_count'")
    public Map<String, Long> getTypeStatistics() {
        log.debug("获取授权类型统计");

        List<Object[]> results = licenseRepository.countByType();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((License.LicenseType) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取即将过期的授权
     */
    public List<LicenseDto> getExpiringLicenses(int days) {
        log.debug("获取即将过期的授权: {} 天内", days);

        LocalDateTime now = getCurrentTime();
        LocalDateTime expireTime = now.plusDays(days);

        List<License> licenses = licenseRepository.findExpiringLicenses(now, expireTime);
        return licenses.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 定时任务：更新过期授权状态
     */
    @Scheduled(cron = "0 0 1 * * ?") // 每天凌晨1点执行
    @CacheEvict(value = "licenses", allEntries = true)
    public void updateExpiredLicenses() {
        log.info("开始更新过期授权状态");

        LocalDateTime now = getCurrentTime();
        List<License> expiredLicenses = licenseRepository.findExpiredActiveLicenses(now);

        int updatedCount = 0;
        for (License license : expiredLicenses) {
            license.setStatus(License.LicenseStatus.EXPIRED);
            license.setUpdatedTime(now);
            license.setUpdatedBy("system");
            licenseRepository.save(license);
            updatedCount++;
        }

        log.info("更新过期授权状态完成，更新数量: {}", updatedCount);
        logOperation("定时更新过期授权", "更新数量: " + updatedCount);
    }

    /**
     * 构建授权查询条件
     */
    private Specification<License> buildLicenseSpecification(QueryParam.LicenseQueryParam queryParam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 基础条件：未删除
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            // 关键词搜索
            if (StringUtils.isNotBlank(queryParam.getKeyword())) {
                String keyword = "%" + queryParam.getKeyword().toLowerCase() + "%";
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("licenseCode")), keyword);
                Predicate productPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("productName")), keyword);

                // 联接客户表进行搜索
                Join<License, Customer> customerJoin = root.join("customer", JoinType.LEFT);
                Predicate customerNamePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(customerJoin.get("customerName")), keyword);

                predicates.add(criteriaBuilder.or(codePredicate, productPredicate, customerNamePredicate));
            }

            // 具体字段搜索
            if (StringUtils.isNotBlank(queryParam.getLicenseCode())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("licenseCode")),
                        "%" + queryParam.getLicenseCode().toLowerCase() + "%"));
            }

            if (queryParam.getCustomerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("customerId"), queryParam.getCustomerId()));
            }

            if (StringUtils.isNotBlank(queryParam.getProductName())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("productName")),
                        "%" + queryParam.getProductName().toLowerCase() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getProductVersion())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("productVersion")),
                        "%" + queryParam.getProductVersion().toLowerCase() + "%"));
            }

            if (queryParam.getLicenseType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("licenseType"), queryParam.getLicenseType()));
            }

            if (queryParam.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryParam.getStatus()));
            }

            // 到期时间范围
            if (queryParam.getExpireStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("expireTime"), queryParam.getExpireStartTime()));
            }

            if (queryParam.getExpireEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("expireTime"), queryParam.getExpireEndTime()));
            }

            // 即将过期条件
            if (queryParam.getIsExpiring() != null && queryParam.getIsExpiring()) {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime futureTime = now.plusDays(
                        queryParam.getRemainingDaysLessThan() != null ? queryParam.getRemainingDaysLessThan() : 30);
                predicates.add(criteriaBuilder.between(root.get("expireTime"), now, futureTime));
                predicates.add(criteriaBuilder.equal(root.get("status"), License.LicenseStatus.ACTIVE));
            }

            // 创建时间范围
            if (queryParam.getStartTime() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("createdTime"), queryParam.getStartTime()));
            }

            if (queryParam.getEndTime() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("createdTime"), queryParam.getEndTime()));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 构建分页对象
     */
    private Pageable buildPageable(QueryParam queryParam) {
        Sort.Direction direction = "desc".equalsIgnoreCase(queryParam.getSortDir()) ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        Sort sort = Sort.by(direction, queryParam.getSortBy());

        return PageRequest.of(queryParam.getPage() - 1, queryParam.getSize(), sort);
    }

    /**
     * 生成授权编码
     */
    private String generateLicenseCode() {
        String prefix = "LIC";
        String timestamp = String.valueOf(System.currentTimeMillis());
        String uuid = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return prefix + timestamp.substring(timestamp.length() - 6) + uuid;
    }

    /**
     * 验证授权时间
     */
    private void validateLicenseTime(LocalDateTime startTime, LocalDateTime expireTime) {
        if (startTime.isAfter(expireTime)) {
            throw new IllegalArgumentException("开始时间不能晚于到期时间");
        }
        if (expireTime.isBefore(getCurrentTime())) {
            throw new IllegalArgumentException("到期时间不能早于当前时间");
        }
    }

    /**
     * 更新授权字段
     */
    private void updateLicenseFields(License license, LicenseDto licenseDto) {
        if (StringUtils.isNotBlank(licenseDto.getLicenseCode())) {
            license.setLicenseCode(licenseDto.getLicenseCode());
        }
        if (licenseDto.getCustomerId() != null) {
            license.setCustomerId(licenseDto.getCustomerId());
        }
        if (StringUtils.isNotBlank(licenseDto.getProductName())) {
            license.setProductName(licenseDto.getProductName());
        }
        if (StringUtils.isNotBlank(licenseDto.getProductVersion())) {
            license.setProductVersion(licenseDto.getProductVersion());
        }
        if (licenseDto.getLicenseType() != null) {
            license.setLicenseType(licenseDto.getLicenseType());
        }
        if (licenseDto.getStatus() != null) {
            license.setStatus(licenseDto.getStatus());
        }
        if (licenseDto.getStartTime() != null) {
            license.setStartTime(licenseDto.getStartTime());
        }
        if (licenseDto.getExpireTime() != null) {
            license.setExpireTime(licenseDto.getExpireTime());
        }
        if (licenseDto.getMaxUsers() != null) {
            license.setMaxUsers(licenseDto.getMaxUsers());
        }
        if (StringUtils.isNotBlank(licenseDto.getFeatures())) {
            license.setFeatures(licenseDto.getFeatures());
        }
        if (StringUtils.isNotBlank(licenseDto.getMachineFingerprint())) {
            license.setMachineFingerprint(licenseDto.getMachineFingerprint());
        }
        if (licenseDto.getPrice() != null) {
            license.setPrice(licenseDto.getPrice());
        }
        if (StringUtils.isNotBlank(licenseDto.getRemark())) {
            license.setRemark(licenseDto.getRemark());
        }
    }

    /**
     * 实体转DTO
     */
    private LicenseDto convertToDto(License license) {
        LicenseDto dto = new LicenseDto();
        dto.setId(license.getId());
        dto.setLicenseCode(license.getLicenseCode());
        dto.setCustomerId(license.getCustomerId());
        dto.setProductName(license.getProductName());
        dto.setProductVersion(license.getProductVersion());
        dto.setLicenseType(license.getLicenseType());
        dto.setStatus(license.getStatus());
        dto.setStartTime(license.getStartTime());
        dto.setExpireTime(license.getExpireTime());
        dto.setMaxUsers(license.getMaxUsers());
        dto.setFeatures(license.getFeatures());
        dto.setMachineFingerprint(license.getMachineFingerprint());
        dto.setPrice(license.getPrice());
        dto.setRemark(license.getRemark());
        dto.setLastValidationTime(license.getLastValidationTime());
        dto.setValidationCount(license.getValidationCount());
        dto.setCreatedTime(license.getCreatedTime());
        dto.setUpdatedTime(license.getUpdatedTime());
        dto.setCreatedBy(license.getCreatedBy());
        dto.setUpdatedBy(license.getUpdatedBy());

        // 计算是否即将过期和剩余天数
        LocalDateTime now = getCurrentTime();
        if (license.getExpireTime() != null) {
            long remainingDays = ChronoUnit.DAYS.between(now, license.getExpireTime());
            dto.setRemainingDays(remainingDays);
            dto.setIsExpiring(remainingDays <= 30 && remainingDays > 0);
        }

        return dto;
    }

    /**
     * DTO转实体
     */
    private License convertToEntity(LicenseDto dto) {
        License license = new License();
        license.setLicenseCode(dto.getLicenseCode());
        license.setCustomerId(dto.getCustomerId());
        license.setProductName(dto.getProductName());
        license.setProductVersion(dto.getProductVersion());
        license.setLicenseType(dto.getLicenseType() != null ? dto.getLicenseType() : License.LicenseType.COMMERCIAL);
        license.setStatus(dto.getStatus() != null ? dto.getStatus() : License.LicenseStatus.ACTIVE);
        license.setStartTime(dto.getStartTime());
        license.setExpireTime(dto.getExpireTime());
        license.setMaxUsers(dto.getMaxUsers());
        license.setFeatures(dto.getFeatures());
        license.setMachineFingerprint(dto.getMachineFingerprint());
        license.setPrice(dto.getPrice());
        license.setRemark(dto.getRemark());
        return license;
    }
}