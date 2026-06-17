package com.enterprise.license.config;

import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.entity.User;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 测试数据初始化器
 * 为测试提供预设的测试数据
 */
@TestComponent
@RequiredArgsConstructor
public class TestDataInitializer {

    private final CustomerRepository customerRepository;
    private final LicenseRepository licenseRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 初始化测试用户
     */
    public User createTestUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setEmail("test@enterprise.com");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        return user;
    }

    /**
     * 初始化测试客户
     */
    @Transactional
    public List<Customer> initializeTestCustomers() {
        Customer customer1 = new Customer();
        customer1.setCustomerCode("TEST001");
        customer1.setCustomerName("测试客户1");
        customer1.setContactPerson("张三");
        customer1.setContactPhone("13800138001");
        customer1.setContactEmail("zhangsan@test.com");
        customer1.setAddress("北京市朝阳区");
        customer1.setStatus(CustomerStatus.ACTIVE);
        customer1.setCreatedAt(LocalDateTime.now().minusDays(30));

        Customer customer2 = new Customer();
        customer2.setCustomerCode("TEST002");
        customer2.setCustomerName("测试客户2");
        customer2.setContactPerson("李四");
        customer2.setContactPhone("13800138002");
        customer2.setContactEmail("lisi@test.com");
        customer2.setAddress("上海市浦东区");
        customer2.setStatus(CustomerStatus.ACTIVE);
        customer2.setCreatedAt(LocalDateTime.now().minusDays(15));

        Customer customer3 = new Customer();
        customer3.setCustomerCode("TEST003");
        customer3.setCustomerName("测试客户3");
        customer3.setContactPerson("王五");
        customer3.setContactPhone("13800138003");
        customer3.setContactEmail("wangwu@test.com");
        customer3.setAddress("广州市天河区");
        customer3.setStatus(CustomerStatus.INACTIVE);
        customer3.setCreatedAt(LocalDateTime.now().minusDays(7));

        return customerRepository.saveAll(Arrays.asList(customer1, customer2, customer3));
    }

    /**
     * 初始化测试许可证
     */
    @Transactional
    public List<License> initializeTestLicenses(List<Customer> customers) {
        if (customers.size() < 2) {
            throw new IllegalArgumentException("至少需要2个客户来创建测试许可证");
        }

        License license1 = new License();
        license1.setLicenseCode("LIC001");
        license1.setCustomer(customers.get(0));
        license1.setLicenseType(LicenseType.STANDARD);
        license1.setProductName("测试产品A");
        license1.setProductVersion("1.0.0");
        license1.setStatus(LicenseStatus.ACTIVE);
        license1.setIssuedAt(LocalDateTime.now().minusDays(10));
        license1.setExpiresAt(LocalDateTime.now().plusDays(90));
        license1.setMaxUsers(100);
        license1.setCurrentUsers(25);

        License license2 = new License();
        license2.setLicenseCode("LIC002");
        license2.setCustomer(customers.get(1));
        license2.setLicenseType(LicenseType.ENTERPRISE);
        license2.setProductName("测试产品B");
        license2.setProductVersion("2.0.0");
        license2.setStatus(LicenseStatus.ACTIVE);
        license2.setIssuedAt(LocalDateTime.now().minusDays(5));
        license2.setExpiresAt(LocalDateTime.now().plusDays(180));
        license2.setMaxUsers(500);
        license2.setCurrentUsers(150);

        License license3 = new License();
        license3.setLicenseCode("LIC003");
        license3.setCustomer(customers.get(0));
        license3.setLicenseType(LicenseType.TRIAL);
        license3.setProductName("测试产品C");
        license3.setProductVersion("1.5.0");
        license3.setStatus(LicenseStatus.EXPIRED);
        license3.setIssuedAt(LocalDateTime.now().minusDays(45));
        license3.setExpiresAt(LocalDateTime.now().minusDays(15));
        license3.setMaxUsers(10);
        license3.setCurrentUsers(8);

        return licenseRepository.saveAll(Arrays.asList(license1, license2, license3));
    }

    /**
     * 清理测试数据
     */
    @Transactional
    public void cleanupTestData() {
        licenseRepository.deleteAll();
        customerRepository.deleteAll();
    }
}