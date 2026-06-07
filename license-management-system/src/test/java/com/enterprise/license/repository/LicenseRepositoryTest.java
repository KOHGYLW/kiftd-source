package com.enterprise.license.repository;

import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
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
 * LicenseRepository 数据访问层测试
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("LicenseRepository 数据访问层测试")
class LicenseRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private LicenseRepository licenseRepository;

    @Autowired
    private CustomerRepository customerRepository;

    private Customer testCustomer1;
    private Customer testCustomer2;
    private License testLicense1;
    private License testLicense2;
    private License testLicense3;

    @BeforeEach
    void setUp() {
        // 初始化测试客户
        testCustomer1 = new Customer();
        testCustomer1.setCustomerCode("TEST001");
        testCustomer1.setCustomerName("测试客户1");
        testCustomer1.setContactPerson("张三");
        testCustomer1.setContactPhone("13800138001");
        testCustomer1.setContactEmail("zhangsan@test.com");
        testCustomer1.setAddress("北京市朝阳区");
        testCustomer1.setStatus(CustomerStatus.ACTIVE);

        testCustomer2 = new Customer();
        testCustomer2.setCustomerCode("TEST002");
        testCustomer2.setCustomerName("测试客户2");
        testCustomer2.setContactPerson("李四");
        testCustomer2.setContactPhone("13800138002");
        testCustomer2.setContactEmail("lisi@test.com");
        testCustomer2.setAddress("上海市浦东区");
        testCustomer2.setStatus(CustomerStatus.ACTIVE);

        entityManager.persistAndFlush(testCustomer1);
        entityManager.persistAndFlush(testCustomer2);

        // 初始化测试许可证
        testLicense1 = new License();
        testLicense1.setLicenseCode("LIC001");
        testLicense1.setCustomer(testCustomer1);
        testLicense1.setLicenseType(LicenseType.STANDARD);
        testLicense1.setProductName("测试产品A");
        testLicense1.setProductVersion("1.0.0");
        testLicense1.setStatus(LicenseStatus.ACTIVE);
        testLicense1.setMaxUsers(100);
        testLicense1.setCurrentUsers(25);
        testLicense1.setIssuedAt(LocalDateTime.now().minusDays(10));
        testLicense1.setExpiresAt(LocalDateTime.now().plusDays(90));

        testLicense2 = new License();
        testLicense2.setLicenseCode("LIC002");
        testLicense2.setCustomer(testCustomer2);
        testLicense2.setLicenseType(LicenseType.ENTERPRISE);
        testLicense2.setProductName("测试产品B");
        testLicense2.setProductVersion("2.0.0");
        testLicense2.setStatus(LicenseStatus.ACTIVE);
        testLicense2.setMaxUsers(500);
        testLicense2.setCurrentUsers(150);
        testLicense2.setIssuedAt(LocalDateTime.now().minusDays(5));
        testLicense2.setExpiresAt(LocalDateTime.now().plusDays(180));

        testLicense3 = new License();
        testLicense3.setLicenseCode("LIC003");
        testLicense3.setCustomer(testCustomer1);
        testLicense3.setLicenseType(LicenseType.TRIAL);
        testLicense3.setProductName("测试产品C");
        testLicense3.setProductVersion("1.5.0");
        testLicense3.setStatus(LicenseStatus.EXPIRED);
        testLicense3.setMaxUsers(10);
        testLicense3.setCurrentUsers(8);
        testLicense3.setIssuedAt(LocalDateTime.now().minusDays(45));
        testLicense3.setExpiresAt(LocalDateTime.now().minusDays(15));

        entityManager.persistAndFlush(testLicense1);
        entityManager.persistAndFlush(testLicense2);
        entityManager.persistAndFlush(testLicense3);
    }

    @Test
    @DisplayName("根据许可证编码查找许可证")
    void shouldFindLicenseByLicenseCode() {
        // When
        Optional<License> found = licenseRepository.findByLicenseCode("LIC001");

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getProductName()).isEqualTo("测试产品A");
        assertThat(found.get().getLicenseType()).isEqualTo(LicenseType.STANDARD);
    }

    @Test
    @DisplayName("检查许可证编码是否存在")
    void shouldCheckIfLicenseCodeExists() {
        // When & Then
        assertThat(licenseRepository.existsByLicenseCode("LIC001")).isTrue();
        assertThat(licenseRepository.existsByLicenseCode("NONEXISTENT")).isFalse();
    }

    @Test
    @DisplayName("根据状态统计许可证数量")
    void shouldCountLicensesByStatus() {
        // When
        long activeCount = licenseRepository.countByStatus(LicenseStatus.ACTIVE);
        long expiredCount = licenseRepository.countByStatus(LicenseStatus.EXPIRED);
        long inactiveCount = licenseRepository.countByStatus(LicenseStatus.INACTIVE);

        // Then
        assertThat(activeCount).isEqualTo(2);
        assertThat(expiredCount).isEqualTo(1);
        assertThat(inactiveCount).isEqualTo(0);
    }

    @Test
    @DisplayName("根据客户ID统计许可证数量")
    void shouldCountLicensesByCustomerId() {
        // When
        long customer1LicenseCount = licenseRepository.countByCustomerId(testCustomer1.getId());
        long customer2LicenseCount = licenseRepository.countByCustomerId(testCustomer2.getId());

        // Then
        assertThat(customer1LicenseCount).isEqualTo(2);
        assertThat(customer2LicenseCount).isEqualTo(1);
    }

    @Test
    @DisplayName("根据客户ID查找许可证")
    void shouldFindLicensesByCustomerId() {
        // When
        List<License> customer1Licenses = licenseRepository.findByCustomerId(testCustomer1.getId());
        List<License> customer2Licenses = licenseRepository.findByCustomerId(testCustomer2.getId());

        // Then
        assertThat(customer1Licenses).hasSize(2);
        assertThat(customer2Licenses).hasSize(1);
        
        assertThat(customer1Licenses)
                .extracting(License::getLicenseCode)
                .containsExactlyInAnyOrder("LIC001", "LIC003");
        
        assertThat(customer2Licenses.get(0).getLicenseCode()).isEqualTo("LIC002");
    }

    @Test
    @DisplayName("根据状态查找许可证")
    void shouldFindLicensesByStatus() {
        // When
        List<License> activeLicenses = licenseRepository.findByStatus(LicenseStatus.ACTIVE);
        List<License> expiredLicenses = licenseRepository.findByStatus(LicenseStatus.EXPIRED);

        // Then
        assertThat(activeLicenses).hasSize(2);
        assertThat(expiredLicenses).hasSize(1);
        
        assertThat(activeLicenses)
                .extracting(License::getLicenseCode)
                .containsExactlyInAnyOrder("LIC001", "LIC002");
        
        assertThat(expiredLicenses.get(0).getLicenseCode()).isEqualTo("LIC003");
    }

    @Test
    @DisplayName("根据许可证类型查找许可证")
    void shouldFindLicensesByLicenseType() {
        // When
        List<License> standardLicenses = licenseRepository.findByLicenseType(LicenseType.STANDARD);
        List<License> enterpriseLicenses = licenseRepository.findByLicenseType(LicenseType.ENTERPRISE);
        List<License> trialLicenses = licenseRepository.findByLicenseType(LicenseType.TRIAL);

        // Then
        assertThat(standardLicenses).hasSize(1);
        assertThat(enterpriseLicenses).hasSize(1);
        assertThat(trialLicenses).hasSize(1);
        
        assertThat(standardLicenses.get(0).getLicenseCode()).isEqualTo("LIC001");
        assertThat(enterpriseLicenses.get(0).getLicenseCode()).isEqualTo("LIC002");
        assertThat(trialLicenses.get(0).getLicenseCode()).isEqualTo("LIC003");
    }

    @Test
    @DisplayName("查找即将过期的许可证")
    void shouldFindExpiringLicenses() {
        // Given
        LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(120);

        // When
        List<License> expiringLicenses = licenseRepository
                .findByStatusAndExpiresAtBefore(LicenseStatus.ACTIVE, expiryThreshold);

        // Then
        assertThat(expiringLicenses).hasSize(1);
        assertThat(expiringLicenses.get(0).getLicenseCode()).isEqualTo("LIC001");
    }

    @Test
    @DisplayName("查找已过期但状态仍为ACTIVE的许可证")
    void shouldFindOverdueLicenses() {
        // Given
        LocalDateTime now = LocalDateTime.now();

        // When
        List<License> overdueLicenses = licenseRepository
                .findByStatusAndExpiresAtBefore(LicenseStatus.ACTIVE, now);

        // Then
        assertThat(overdueLicenses).isEmpty(); // 测试数据中没有已过期但状态仍为ACTIVE的许可证
    }

    @Test
    @DisplayName("根据产品名称查找许可证")
    void shouldFindLicensesByProductName() {
        // When
        List<License> productALicenses = licenseRepository.findByProductName("测试产品A");
        List<License> allTestProductLicenses = licenseRepository.findByProductNameContaining("测试产品");

        // Then
        assertThat(productALicenses).hasSize(1);
        assertThat(productALicenses.get(0).getLicenseCode()).isEqualTo("LIC001");
        
        assertThat(allTestProductLicenses).hasSize(3);
    }

    @Test
    @DisplayName("分页查询活跃许可证")
    void shouldFindActiveLicensesWithPagination() {
        // Given
        Pageable pageable = PageRequest.of(0, 1, Sort.by("issuedAt").descending());

        // When
        Page<License> licensePage = licenseRepository.findByStatus(LicenseStatus.ACTIVE, pageable);

        // Then
        assertThat(licensePage.getTotalElements()).isEqualTo(2);
        assertThat(licensePage.getTotalPages()).isEqualTo(2);
        assertThat(licensePage.getContent()).hasSize(1);
        
        // 验证排序（按发布时间降序）
        License firstLicense = licensePage.getContent().get(0);
        assertThat(firstLicense.getLicenseCode()).isEqualTo("LIC002"); // 最近发布的
    }

    @Test
    @DisplayName("查找发布时间在指定范围内的许可证")
    void shouldFindLicensesByIssuedAtBetween() {
        // Given
        LocalDateTime startDate = LocalDateTime.now().minusDays(8);
        LocalDateTime endDate = LocalDateTime.now().minusDays(3);

        // When
        List<License> licenses = licenseRepository.findByIssuedAtBetween(startDate, endDate);

        // Then
        assertThat(licenses).hasSize(1);
        assertThat(licenses.get(0).getLicenseCode()).isEqualTo("LIC002");
    }

    @Test
    @DisplayName("根据用户数量范围查找许可证")
    void shouldFindLicensesByMaxUsersBetween() {
        // Given
        int minUsers = 50;
        int maxUsers = 200;

        // When
        List<License> licenses = licenseRepository.findByMaxUsersBetween(minUsers, maxUsers);

        // Then
        assertThat(licenses).hasSize(1);
        assertThat(licenses.get(0).getLicenseCode()).isEqualTo("LIC001");
        assertThat(licenses.get(0).getMaxUsers()).isEqualTo(100);
    }

    @Test
    @DisplayName("查找使用率高的许可证")
    void shouldFindHighUsageLicenses() {
        // Given - 创建一个使用率超过80%的许可证
        License highUsageLicense = new License();
        highUsageLicense.setLicenseCode("LIC004");
        highUsageLicense.setCustomer(testCustomer1);
        highUsageLicense.setLicenseType(LicenseType.STANDARD);
        highUsageLicense.setProductName("高使用率产品");
        highUsageLicense.setProductVersion("1.0.0");
        highUsageLicense.setStatus(LicenseStatus.ACTIVE);
        highUsageLicense.setMaxUsers(100);
        highUsageLicense.setCurrentUsers(85); // 85% 使用率
        highUsageLicense.setIssuedAt(LocalDateTime.now());
        highUsageLicense.setExpiresAt(LocalDateTime.now().plusDays(30));
        
        entityManager.persistAndFlush(highUsageLicense);

        // When - 使用自定义查询查找使用率高的许可证
        List<License> allLicenses = licenseRepository.findByStatus(LicenseStatus.ACTIVE);
        List<License> highUsageLicenses = allLicenses.stream()
                .filter(license -> (double) license.getCurrentUsers() / license.getMaxUsers() > 0.8)
                .toList();

        // Then
        assertThat(highUsageLicenses).hasSize(1);
        assertThat(highUsageLicenses.get(0).getLicenseCode()).isEqualTo("LIC004");
    }

    @Test
    @DisplayName("自定义查询：根据客户和状态查找许可证")
    void shouldFindLicensesByCustomerAndStatus() {
        // When
        List<License> customer1ActiveLicenses = licenseRepository
                .findByCustomerIdAndStatus(testCustomer1.getId(), LicenseStatus.ACTIVE);
        List<License> customer1ExpiredLicenses = licenseRepository
                .findByCustomerIdAndStatus(testCustomer1.getId(), LicenseStatus.EXPIRED);

        // Then
        assertThat(customer1ActiveLicenses).hasSize(1);
        assertThat(customer1ActiveLicenses.get(0).getLicenseCode()).isEqualTo("LIC001");
        
        assertThat(customer1ExpiredLicenses).hasSize(1);
        assertThat(customer1ExpiredLicenses.get(0).getLicenseCode()).isEqualTo("LIC003");
    }

    @Test
    @DisplayName("保存许可证时自动设置创建时间")
    void shouldSetCreatedAtWhenSavingLicense() {
        // Given
        License newLicense = new License();
        newLicense.setLicenseCode("NEW001");
        newLicense.setCustomer(testCustomer1);
        newLicense.setLicenseType(LicenseType.STANDARD);
        newLicense.setProductName("新产品");
        newLicense.setProductVersion("1.0.0");
        newLicense.setStatus(LicenseStatus.ACTIVE);
        newLicense.setMaxUsers(50);
        newLicense.setCurrentUsers(0);
        newLicense.setIssuedAt(LocalDateTime.now());
        newLicense.setExpiresAt(LocalDateTime.now().plusDays(365));

        // When
        License saved = licenseRepository.saveAndFlush(newLicense);

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getCreatedAt()).isEqualTo(saved.getUpdatedAt());
    }

    @Test
    @DisplayName("删除许可证时不影响关联的客户")
    void shouldNotAffectCustomerWhenDeletingLicense() {
        // Given
        License licenseToDelete = licenseRepository.findByLicenseCode("LIC003").orElseThrow();
        Long customerId = licenseToDelete.getCustomer().getId();

        // When
        licenseRepository.delete(licenseToDelete);
        licenseRepository.flush();

        // Then
        Optional<License> deleted = licenseRepository.findByLicenseCode("LIC003");
        assertThat(deleted).isEmpty();
        
        // 验证客户仍然存在
        Optional<Customer> customer = customerRepository.findById(customerId);
        assertThat(customer).isPresent();
        
        // 验证该客户的其他许可证仍然存在
        List<License> remainingLicenses = licenseRepository.findByCustomerId(customerId);
        assertThat(remainingLicenses).hasSize(1);
        assertThat(remainingLicenses.get(0).getLicenseCode()).isEqualTo("LIC001");
    }
}