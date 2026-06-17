package com.enterprise.license.repository;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.enums.CustomerStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * CustomerRepository 数据访问层测试
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("CustomerRepository 数据访问层测试")
class CustomerRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer1;
    private Customer testCustomer2;
    private Customer testCustomer3;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testCustomer1 = new Customer();
        testCustomer1.setCustomerCode("TEST001");
        testCustomer1.setCustomerName("测试客户1");
        testCustomer1.setContactPerson("张三");
        testCustomer1.setContactPhone("13800138001");
        testCustomer1.setContactEmail("zhangsan@test.com");
        testCustomer1.setAddress("北京市朝阳区");
        testCustomer1.setStatus(CustomerStatus.ACTIVE);
        testCustomer1.setCreatedAt(LocalDateTime.now().minusDays(30));

        testCustomer2 = new Customer();
        testCustomer2.setCustomerCode("TEST002");
        testCustomer2.setCustomerName("测试客户2");
        testCustomer2.setContactPerson("李四");
        testCustomer2.setContactPhone("13800138002");
        testCustomer2.setContactEmail("lisi@test.com");
        testCustomer2.setAddress("上海市浦东区");
        testCustomer2.setStatus(CustomerStatus.ACTIVE);
        testCustomer2.setCreatedAt(LocalDateTime.now().minusDays(15));

        testCustomer3 = new Customer();
        testCustomer3.setCustomerCode("TEST003");
        testCustomer3.setCustomerName("测试客户3");
        testCustomer3.setContactPerson("王五");
        testCustomer3.setContactPhone("13800138003");
        testCustomer3.setContactEmail("wangwu@test.com");
        testCustomer3.setAddress("广州市天河区");
        testCustomer3.setStatus(CustomerStatus.INACTIVE);
        testCustomer3.setCreatedAt(LocalDateTime.now().minusDays(7));

        // 保存测试数据
        entityManager.persistAndFlush(testCustomer1);
        entityManager.persistAndFlush(testCustomer2);
        entityManager.persistAndFlush(testCustomer3);
    }

    @Test
    @DisplayName("根据客户编码查找客户")
    void shouldFindCustomerByCustomerCode() {
        // When
        Optional<Customer> found = customerRepository.findByCustomerCode("TEST001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getCustomerName()).isEqualTo("测试客户1");
        assertThat(found.get().getContactPerson()).isEqualTo("张三");
    }

    @Test
    @DisplayName("客户编码不存在时返回空")
    void shouldReturnEmptyWhenCustomerCodeNotExists() {
        // When
        Optional<Customer> found = customerRepository.findByCustomerCode("NONEXISTENT");

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("检查客户编码是否存在")
    void shouldCheckIfCustomerCodeExists() {
        // When & Then
        assertThat(customerRepository.existsByCustomerCode("TEST001")).isTrue();
        assertThat(customerRepository.existsByCustomerCode("NONEXISTENT")).isFalse();
    }

    @Test
    @DisplayName("根据状态统计客户数量")
    void shouldCountCustomersByStatus() {
        // When
        long activeCount = customerRepository.countByStatus(CustomerStatus.ACTIVE);
        long inactiveCount = customerRepository.countByStatus(CustomerStatus.INACTIVE);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(inactiveCount).isEqualTo(1);
    }

    @Test
    @DisplayName("根据状态查找客户")
    void shouldFindCustomersByStatus() {
        // When
        List<Customer> activeCustomers = customerRepository.findByStatus(CustomerStatus.ACTIVE);
        List<Customer> inactiveCustomers = customerRepository.findByStatus(CustomerStatus.INACTIVE);

        // Then
        assertThat(activeCustomers).hasSize(2);
        assertThat(inactiveCustomers).hasSize(1);
        
        assertThat(activeCustomers)
                .extracting(Customer::getCustomerCode)
                .containsExactlyInAnyOrder("TEST001", "TEST002");
        
        assertThat(inactiveCustomers.get(0).getCustomerCode()).isEqualTo("TEST003");
    }

    @Test
    @DisplayName("根据客户名称模糊查找")
    void shouldFindCustomersByNameContaining() {
        // When
        List<Customer> customers = customerRepository.findByCustomerNameContaining("测试");

        // Then
        assertThat(customers).hasSize(3);
        assertThat(customers)
                .extracting(Customer::getCustomerName)
                .allMatch(name -> name.contains("测试"));
    }

    @Test
    @DisplayName("根据联系人姓名查找")
    void shouldFindCustomersByContactPerson() {
        // When
        List<Customer> customers = customerRepository.findByContactPerson("张三");

        // Then
        assertThat(customers).hasSize(1);
        assertThat(customers.get(0).getCustomerCode()).isEqualTo("TEST001");
    }

    @Test
    @DisplayName("根据邮箱查找客户")
    void shouldFindCustomerByEmail() {
        // When
        Optional<Customer> customer = customerRepository.findByContactEmail("zhangsan@test.com");

        // Then
        assertThat(customer).isPresent();
        assertThat(customer.get().getCustomerCode()).isEqualTo("TEST001");
    }

    @Test
    @DisplayName("分页查询活跃客户")
    void shouldFindActiveCustomersWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 2, Sort.by("createdAt").descending());

        // When
        Page<Customer> customerPage = customerRepository.findByStatus(CustomerStatus.ACTIVE, pageable);

        // Then
        assertThat(customerPage.getTotalElements()).isEqualTo(2);
        assertThat(customerPage.getTotalPages()).isEqualTo(1);
        assertThat(customerPage.getContent()).hasSize(2);
        
        // 验证排序（按创建时间降序）
        List<Customer> customers = customerPage.getContent();
        assertThat(customers.get(0).getCreatedAt()).isAfter(customers.get(1).getCreatedAt());
    }

    @Test
    @DisplayName("查找创建时间在指定范围内的客户")
    void shouldFindCustomersByCreatedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(20);
        LocalDateTime endDate = LocalDateTime.now().minusDays(5);

        // When
        List<Customer> customers = customerRepository.findByCreatedAtBetween(startDate, endDate);

        // Then
        assertThat(customers).hasSize(2); // TEST002 和 TEST003
        assertThat(customers)
                .extracting(Customer::getCustomerCode)
                .containsExactlyInAnyOrder("TEST002", "TEST003");
    }

    @Test
    @DisplayName("查找最近创建的客户")
    void shouldFindRecentlyCreatedCustomers() {
        // Given
        LocalDateTime since = LocalDateTime.now().minusDays(10);

        // When
        List<Customer> recentCustomers = customerRepository.findByCreatedAtAfter(since);

        // Then
        assertThat(recentCustomers).hasSize(1);
        assertThat(recentCustomers.get(0).getCustomerCode()).isEqualTo("TEST003");
    }

    @Test
    @DisplayName("自定义查询：根据多个条件查找客户")
    void shouldFindCustomersByMultipleConditions() {
        // Given
        String nameKeyword = "测试";
        CustomerStatus status = CustomerStatus.ACTIVE;

        // When
        List<Customer> customers = customerRepository.findByCustomerNameContainingAndStatus(
                nameKeyword, status);

        // Then
        assertThat(customers).hasSize(2);
        assertThat(customers)
                .extracting(Customer::getStatus)
                .allMatch(s -> s == CustomerStatus.ACTIVE);
        assertThat(customers)
                .extracting(Customer::getCustomerName)
                .allMatch(name -> name.contains("测试"));
    }

    @Test
    @DisplayName("保存客户时自动设置创建时间")
    void shouldSetCreatedAtWhenSavingCustomer() {
        // Given
        Customer newCustomer = new Customer();
        newCustomer.setCustomerCode("NEW001");
        newCustomer.setCustomerName("新客户");
        newCustomer.setContactPerson("赵六");
        newCustomer.setContactPhone("13800138004");
        newCustomer.setContactEmail("zhaoliu@test.com");
        newCustomer.setAddress("深圳市南山区");
        newCustomer.setStatus(CustomerStatus.ACTIVE);

        // When
        Customer saved = customerRepository.saveAndFlush(newCustomer);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("更新客户时自动更新修改时间")
    void shouldUpdateModifiedAtWhenUpdatingCustomer() throws InterruptedException {
        // Given
        Customer customer = customerRepository.findByCustomerCode("TEST001").orElseThrow();
        LocalDateTime originalUpdatedAt = customer.getUpdatedAt();
        
        // 等待一段时间确保时间戳不同
        Thread.sleep(1000);

        // When
        customer.setCustomerName("更新后的客户名");
        Customer updated = customerRepository.saveAndFlush(customer);

        // Then
        assertThat(updated.getUpdatedAt()).isAfter(originalUpdatedAt);
        assertThat(updated.getCreatedAt()).isEqualTo(customer.getCreatedAt()); // 创建时间不变
    }

    @Test
    @DisplayName("删除客户")
    void shouldDeleteCustomer() {
        // Given
        Customer customer = customerRepository.findByCustomerCode("TEST003").orElseThrow();
        Long customerId = customer.getId();

        // When
        customerRepository.delete(customer);
        customerRepository.flush();

        // Then
        Optional<Customer> deleted = customerRepository.findById(customerId);
        assertThat(deleted).isEmpty();
        
        // 验证总数减少
        long totalCount = customerRepository.count();
        assertThat(totalCount).isEqualTo(2);
    }

    @Test
    @DisplayName("批量删除客户")
    void shouldDeleteCustomersInBatch() {
        // Given
        List<Customer> customersToDelete = customerRepository.findByStatus(CustomerStatus.INACTIVE);
        assertThat(customersToDelete).hasSize(1);

        // When
        customerRepository.deleteAll(customersToDelete);
        customerRepository.flush();

        // Then
        long remainingCount = customerRepository.count();
        assertThat(remainingCount).isEqualTo(2);
        
        List<Customer> inactiveCustomers = customerRepository.findByStatus(CustomerStatus.INACTIVE);
        assertThat(inactiveCustomers).isEmpty();
    }
}