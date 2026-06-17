package com.enterprise.license.service;

import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.QueryParam;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 客户管理服务
 */
@Slf4j
@Service
@Validated
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class CustomerService extends BaseService {

    private final CustomerRepository customerRepository;
    private final LicenseRepository licenseRepository;

    /**
     * 创建客户
     */
    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDto createCustomer(CustomerDto customerDto) {
        log.info("创建客户: {}", customerDto.getCustomerName());

        // 验证客户编码唯一性
        if (StringUtils.isNotBlank(customerDto.getCustomerCode()) &&
                customerRepository.existsByCustomerCodeAndDeletedFalse(customerDto.getCustomerCode())) {
            throw new IllegalArgumentException("客户编码已存在: " + customerDto.getCustomerCode());
        }

        // 验证邮箱唯一性
        if (StringUtils.isNotBlank(customerDto.getEmail()) &&
                customerRepository.existsByEmailAndDeletedFalse(customerDto.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + customerDto.getEmail());
        }

        // 生成客户编码
        if (StringUtils.isBlank(customerDto.getCustomerCode())) {
            customerDto.setCustomerCode(generateCustomerCode());
        }

        Customer customer = convertToEntity(customerDto);
        customer.setCreatedTime(getCurrentTime());
        customer.setCreatedBy("system"); // TODO: 从安全上下文获取当前用户

        Customer savedCustomer = customerRepository.save(customer);
        logOperation("创建客户", "客户ID: " + savedCustomer.getId());

        return convertToDto(savedCustomer);
    }

    /**
     * 更新客户信息
     */
    @CacheEvict(value = "customers", allEntries = true)
    public CustomerDto updateCustomer(Long id, CustomerDto customerDto) {
        log.info("更新客户: {}", id);

        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + id));

        if (existingCustomer.getDeleted()) {
            throw new IllegalArgumentException("客户已被删除: " + id);
        }

        // 验证客户编码唯一性（排除当前客户）
        if (StringUtils.isNotBlank(customerDto.getCustomerCode()) &&
                !customerDto.getCustomerCode().equals(existingCustomer.getCustomerCode()) &&
                customerRepository.existsByCustomerCodeAndDeletedFalse(customerDto.getCustomerCode())) {
            throw new IllegalArgumentException("客户编码已存在: " + customerDto.getCustomerCode());
        }

        // 验证邮箱唯一性（排除当前客户）
        if (StringUtils.isNotBlank(customerDto.getEmail()) &&
                !customerDto.getEmail().equals(existingCustomer.getEmail()) &&
                customerRepository.existsByEmailAndDeletedFalse(customerDto.getEmail())) {
            throw new IllegalArgumentException("邮箱已存在: " + customerDto.getEmail());
        }

        // 更新字段
        updateCustomerFields(existingCustomer, customerDto);
        existingCustomer.setUpdatedTime(getCurrentTime());
        existingCustomer.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        Customer updatedCustomer = customerRepository.save(existingCustomer);
        logOperation("更新客户", "客户ID: " + updatedCustomer.getId());

        return convertToDto(updatedCustomer);
    }

    /**
     * 根据ID查找客户
     */
    @Cacheable(value = "customers", key = "#id")
    public CustomerDto findById(Long id) {
        log.debug("查找客户: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + id));

        if (customer.getDeleted()) {
            throw new IllegalArgumentException("客户已被删除: " + id);
        }

        CustomerDto customerDto = convertToDto(customer);
        // 加载授权统计信息
        loadLicenseStatistics(customerDto);

        return customerDto;
    }

    /**
     * 根据客户编码查找客户
     */
    @Cacheable(value = "customers", key = "#customerCode")
    public CustomerDto findByCustomerCode(String customerCode) {
        log.debug("根据客户编码查找客户: {}", customerCode);

        Customer customer = customerRepository.findByCustomerCodeAndDeletedFalse(customerCode)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + customerCode));

        CustomerDto customerDto = convertToDto(customer);
        loadLicenseStatistics(customerDto);

        return customerDto;
    }

    /**
     * 分页查询客户
     */
    public Page<CustomerDto> findCustomers(QueryParam.CustomerQueryParam queryParam) {
        log.debug("分页查询客户: {}", queryParam);

        Specification<Customer> spec = buildCustomerSpecification(queryParam);
        Pageable pageable = buildPageable(queryParam);

        Page<Customer> customerPage = customerRepository.findAll(spec, pageable);

        return customerPage.map(customer -> {
            CustomerDto customerDto = convertToDto(customer);
            loadLicenseStatistics(customerDto);
            return customerDto;
        });
    }

    /**
     * 删除客户（软删除）
     */
    @CacheEvict(value = "customers", allEntries = true)
    public void deleteCustomer(Long id) {
        log.info("删除客户: {}", id);

        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + id));

        if (customer.getDeleted()) {
            throw new IllegalArgumentException("客户已被删除: " + id);
        }

        // 检查是否有有效授权
        long activeLicenseCount = licenseRepository.countActiveByCustomerId(id, getCurrentTime());
        if (activeLicenseCount > 0) {
            throw new IllegalArgumentException("客户存在有效授权，无法删除");
        }

        customer.setDeleted(true);
        customer.setUpdatedTime(getCurrentTime());
        customer.setUpdatedBy("system"); // TODO: 从安全上下文获取当前用户

        customerRepository.save(customer);
        logOperation("删除客户", "客户ID: " + id);
    }

    /**
     * 批量更新客户状态
     */
    @CacheEvict(value = "customers", allEntries = true)
    public int batchUpdateStatus(List<Long> ids, Customer.CustomerStatus status) {
        log.info("批量更新客户状态: {} -> {}", ids, status);

        if (ids == null || ids.isEmpty()) {
            throw new IllegalArgumentException("客户ID列表不能为空");
        }

        int updatedCount = customerRepository.batchUpdateStatus(
                ids, status, getCurrentTime(), "system"); // TODO: 从安全上下文获取当前用户

        logOperation("批量更新客户状态", String.format("更新数量: %d, 状态: %s", updatedCount, status));

        return updatedCount;
    }

    /**
     * 获取客户状态统计
     */
    @Cacheable(value = "customer_statistics", key = "'status_count'")
    public Map<String, Long> getStatusStatistics() {
        log.debug("获取客户状态统计");

        List<Object[]> results = customerRepository.countByStatus();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((Customer.CustomerStatus) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取客户类型统计
     */
    @Cacheable(value = "customer_statistics", key = "'type_count'")
    public Map<String, Long> getTypeStatistics() {
        log.debug("获取客户类型统计");

        List<Object[]> results = customerRepository.countByType();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> ((Customer.CustomerType) result[0]).name(),
                        result -> (Long) result[1]
                ));
    }

    /**
     * 获取活跃客户列表
     */
    public List<CustomerDto> getActiveCustomersSince(LocalDateTime since) {
        log.debug("获取活跃客户: {}", since);

        List<Customer> customers = customerRepository.findActiveCustomersSince(since);
        return customers.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 更新客户最后登录时间
     */
    @CacheEvict(value = "customers", key = "#customerId")
    public void updateLastLoginTime(Long customerId, LocalDateTime loginTime) {
        log.debug("更新客户最后登录时间: {} -> {}", customerId, loginTime);

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("客户不存在: " + customerId));

        customer.setLastLoginTime(loginTime);
        customer.setUpdatedTime(getCurrentTime());

        customerRepository.save(customer);
    }

    /**
     * 构建客户查询条件
     */
    private Specification<Customer> buildCustomerSpecification(QueryParam.CustomerQueryParam queryParam) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 基础条件：未删除
            predicates.add(criteriaBuilder.equal(root.get("deleted"), false));

            // 关键词搜索
            if (StringUtils.isNotBlank(queryParam.getKeyword())) {
                String keyword = "%" + queryParam.getKeyword().toLowerCase() + "%";
                Predicate namePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerName")), keyword);
                Predicate codePredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerCode")), keyword);
                Predicate contactPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("contactPerson")), keyword);
                Predicate emailPredicate = criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")), keyword);

                predicates.add(criteriaBuilder.or(namePredicate, codePredicate, contactPredicate, emailPredicate));
            }

            // 具体字段搜索
            if (StringUtils.isNotBlank(queryParam.getCustomerCode())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerCode")),
                        "%" + queryParam.getCustomerCode().toLowerCase() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getCustomerName())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("customerName")),
                        "%" + queryParam.getCustomerName().toLowerCase() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getContactPerson())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("contactPerson")),
                        "%" + queryParam.getContactPerson().toLowerCase() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getEmail())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + queryParam.getEmail().toLowerCase() + "%"));
            }

            if (StringUtils.isNotBlank(queryParam.getPhone())) {
                predicates.add(criteriaBuilder.like(root.get("phone"),
                        "%" + queryParam.getPhone() + "%"));
            }

            if (queryParam.getStatus() != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), queryParam.getStatus()));
            }

            if (queryParam.getType() != null) {
                predicates.add(criteriaBuilder.equal(root.get("type"), queryParam.getType()));
            }

            if (StringUtils.isNotBlank(queryParam.getAddress())) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("address")),
                        "%" + queryParam.getAddress().toLowerCase() + "%"));
            }

            // 时间范围
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
     * 生成客户编码
     */
    private String generateCustomerCode() {
        String prefix = "CUST";
        String timestamp = String.valueOf(System.currentTimeMillis());
        return prefix + timestamp.substring(timestamp.length() - 8);
    }

    /**
     * 更新客户字段
     */
    private void updateCustomerFields(Customer customer, CustomerDto customerDto) {
        if (StringUtils.isNotBlank(customerDto.getCustomerCode())) {
            customer.setCustomerCode(customerDto.getCustomerCode());
        }
        if (StringUtils.isNotBlank(customerDto.getCustomerName())) {
            customer.setCustomerName(customerDto.getCustomerName());
        }
        if (StringUtils.isNotBlank(customerDto.getContactPerson())) {
            customer.setContactPerson(customerDto.getContactPerson());
        }
        if (StringUtils.isNotBlank(customerDto.getEmail())) {
            customer.setEmail(customerDto.getEmail());
        }
        if (StringUtils.isNotBlank(customerDto.getPhone())) {
            customer.setPhone(customerDto.getPhone());
        }
        if (StringUtils.isNotBlank(customerDto.getAddress())) {
            customer.setAddress(customerDto.getAddress());
        }
        if (customerDto.getStatus() != null) {
            customer.setStatus(customerDto.getStatus());
        }
        if (customerDto.getType() != null) {
            customer.setType(customerDto.getType());
        }
        if (StringUtils.isNotBlank(customerDto.getRemark())) {
            customer.setRemark(customerDto.getRemark());
        }
    }

    /**
     * 加载授权统计信息
     */
    private void loadLicenseStatistics(CustomerDto customerDto) {
        Long customerId = customerDto.getId();
        if (customerId != null) {
            customerDto.setLicenseCount(licenseRepository.countByCustomerId(customerId));
            customerDto.setActiveLicenseCount(
                    licenseRepository.countActiveByCustomerId(customerId, getCurrentTime()));
        }
    }

    /**
     * 实体转DTO
     */
    private CustomerDto convertToDto(Customer customer) {
        CustomerDto dto = new CustomerDto();
        dto.setId(customer.getId());
        dto.setCustomerCode(customer.getCustomerCode());
        dto.setCustomerName(customer.getCustomerName());
        dto.setContactPerson(customer.getContactPerson());
        dto.setEmail(customer.getEmail());
        dto.setPhone(customer.getPhone());
        dto.setAddress(customer.getAddress());
        dto.setStatus(customer.getStatus());
        dto.setType(customer.getType());
        dto.setRemark(customer.getRemark());
        dto.setLastLoginTime(customer.getLastLoginTime());
        dto.setCreatedTime(customer.getCreatedTime());
        dto.setUpdatedTime(customer.getUpdatedTime());
        dto.setCreatedBy(customer.getCreatedBy());
        dto.setUpdatedBy(customer.getUpdatedBy());
        return dto;
    }

    /**
     * DTO转实体
     */
    private Customer convertToEntity(CustomerDto dto) {
        Customer customer = new Customer();
        customer.setCustomerCode(dto.getCustomerCode());
        customer.setCustomerName(dto.getCustomerName());
        customer.setContactPerson(dto.getContactPerson());
        customer.setEmail(dto.getEmail());
        customer.setPhone(dto.getPhone());
        customer.setAddress(dto.getAddress());
        customer.setStatus(dto.getStatus() != null ? dto.getStatus() : Customer.CustomerStatus.ACTIVE);
        customer.setType(dto.getType() != null ? dto.getType() : Customer.CustomerType.ENTERPRISE);
        customer.setRemark(dto.getRemark());
        return customer;
    }
}