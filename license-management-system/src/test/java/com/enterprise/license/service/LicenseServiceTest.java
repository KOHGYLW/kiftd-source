package com.enterprise.license.service;

import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.dto.PageResponse;
import com.enterprise.license.dto.QueryParam;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.entity.LicenseKey;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
import com.enterprise.license.exception.ResourceNotFoundException;
import com.enterprise.license.exception.ValidationException;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import com.enterprise.license.repository.LicenseKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LicenseService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LicenseService 单元测试")
class LicenseServiceTest {

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LicenseKeyRepository licenseKeyRepository;

    @Mock
    private LicenseEncryptionService licenseEncryptionService;

    @Mock
    private KeyManagerService keyManagerService;

    @InjectMocks
    private LicenseService licenseService;

    private LicenseDto testLicenseDto;
    private License testLicense;
    private Customer testCustomer;
    private LicenseKey testLicenseKey;

    @BeforeEach
    void setUp() {
        // 初始化测试客户
        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setCustomerCode("TEST001");
        testCustomer.setCustomerName("测试客户");
        testCustomer.setStatus(CustomerStatus.ACTIVE);

        // 初始化测试许可证DTO
        testLicenseDto = new LicenseDto();
        testLicenseDto.setLicenseCode("LIC001");
        testLicenseDto.setCustomerId(1L);
        testLicenseDto.setLicenseType(LicenseType.STANDARD);
        testLicenseDto.setProductName("测试产品");
        testLicenseDto.setProductVersion("1.0.0");
        testLicenseDto.setMaxUsers(100);
        testLicenseDto.setExpiresAt(LocalDateTime.now().plusDays(365));

        // 初始化测试许可证实体
        testLicense = new License();
        testLicense.setId(1L);
        testLicense.setLicenseCode("LIC001");
        testLicense.setCustomer(testCustomer);
        testLicense.setLicenseType(LicenseType.STANDARD);
        testLicense.setProductName("测试产品");
        testLicense.setProductVersion("1.0.0");
        testLicense.setStatus(LicenseStatus.ACTIVE);
        testLicense.setMaxUsers(100);
        testLicense.setCurrentUsers(0);
        testLicense.setIssuedAt(LocalDateTime.now());
        testLicense.setExpiresAt(LocalDateTime.now().plusDays(365));

        // 初始化测试许可证密钥
        testLicenseKey = new LicenseKey();
        testLicenseKey.setId(1L);
        testLicenseKey.setLicense(testLicense);
        testLicenseKey.setKeyData("encrypted-license-key-data");
        testLicenseKey.setSignature("license-signature");
        testLicenseKey.setCreatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("创建许可证测试")
    class CreateLicenseTests {

        @Test
        @DisplayName("成功创建许可证")
        void shouldCreateLicenseSuccessfully() {
            // Given
            when(customerRepository.findById(testLicenseDto.getCustomerId()))
                    .thenReturn(Optional.of(testCustomer));
            when(licenseRepository.existsByLicenseCode(testLicenseDto.getLicenseCode()))
                    .thenReturn(false);
            when(licenseRepository.save(any(License.class)))
                    .thenReturn(testLicense);
            when(licenseEncryptionService.generateLicenseKey(any(License.class)))
                    .thenReturn("encrypted-license-key");
            when(licenseEncryptionService.signLicenseKey(anyString()))
                    .thenReturn("license-signature");

            // When
            LicenseDto result = licenseService.createLicense(testLicenseDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getLicenseCode()).isEqualTo(testLicenseDto.getLicenseCode());
            assertThat(result.getProductName()).isEqualTo(testLicenseDto.getProductName());
            assertThat(result.getStatus()).isEqualTo(LicenseStatus.ACTIVE);

            verify(customerRepository).findById(testLicenseDto.getCustomerId());
            verify(licenseRepository).existsByLicenseCode(testLicenseDto.getLicenseCode());
            verify(licenseRepository).save(any(License.class));
            verify(licenseEncryptionService).generateLicenseKey(any(License.class));
        }

        @Test
        @DisplayName("客户不存在时创建许可证失败")
        void shouldFailWhenCustomerNotExists() {
            // Given
            when(customerRepository.findById(testLicenseDto.getCustomerId()))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> licenseService.createLicense(testLicenseDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");

            verify(customerRepository).findById(testLicenseDto.getCustomerId());
            verify(licenseRepository, never()).save(any(License.class));
        }

        @Test
        @DisplayName("许可证编码重复时创建失败")
        void shouldFailWhenLicenseCodeExists() {
            // Given
            when(customerRepository.findById(testLicenseDto.getCustomerId()))
                    .thenReturn(Optional.of(testCustomer));
            when(licenseRepository.existsByLicenseCode(testLicenseDto.getLicenseCode()))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> licenseService.createLicense(testLicenseDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("许可证编码已存在");

            verify(licenseRepository).existsByLicenseCode(testLicenseDto.getLicenseCode());
            verify(licenseRepository, never()).save(any(License.class));
        }

        @Test
        @DisplayName("客户状态非活跃时创建许可证失败")
        void shouldFailWhenCustomerIsInactive() {
            // Given
            testCustomer.setStatus(CustomerStatus.INACTIVE);
            when(customerRepository.findById(testLicenseDto.getCustomerId()))
                    .thenReturn(Optional.of(testCustomer));

            // When & Then
            assertThatThrownBy(() -> licenseService.createLicense(testLicenseDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("客户状态非活跃");

            verify(customerRepository).findById(testLicenseDto.getCustomerId());
            verify(licenseRepository, never()).save(any(License.class));
        }

        @Test
        @DisplayName("试用许可证期限不能超过30天")
        void shouldLimitTrialLicenseDuration() {
            // Given
            testLicenseDto.setLicenseType(LicenseType.TRIAL);
            testLicenseDto.setExpiresAt(LocalDateTime.now().plusDays(45)); // 超过30天

            when(customerRepository.findById(testLicenseDto.getCustomerId()))
                    .thenReturn(Optional.of(testCustomer));

            // When & Then
            assertThatThrownBy(() -> licenseService.createLicense(testLicenseDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("试用许可证有效期不能超过30天");
        }
    }

    @Nested
    @DisplayName("更新许可证测试")
    class UpdateLicenseTests {

        @Test
        @DisplayName("成功更新许可证")
        void shouldUpdateLicenseSuccessfully() {
            // Given
            Long licenseId = 1L;
            LicenseDto updateDto = new LicenseDto();
            updateDto.setMaxUsers(200);
            updateDto.setExpiresAt(LocalDateTime.now().plusDays(180));

            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));
            when(licenseRepository.save(any(License.class)))
                    .thenReturn(testLicense);

            // When
            LicenseDto result = licenseService.updateLicense(licenseId, updateDto);

            // Then
            assertThat(result).isNotNull();
            verify(licenseRepository).findById(licenseId);
            verify(licenseRepository).save(any(License.class));
        }

        @Test
        @DisplayName("更新不存在的许可证时抛出异常")
        void shouldThrowExceptionWhenLicenseNotFound() {
            // Given
            Long licenseId = 999L;
            LicenseDto updateDto = new LicenseDto();

            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> licenseService.updateLicense(licenseId, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("许可证不存在");

            verify(licenseRepository).findById(licenseId);
            verify(licenseRepository, never()).save(any(License.class));
        }

        @Test
        @DisplayName("不能将过期许可证改为活跃状态")
        void shouldNotActivateExpiredLicense() {
            // Given
            Long licenseId = 1L;
            testLicense.setExpiresAt(LocalDateTime.now().minusDays(1)); // 已过期
            
            LicenseDto updateDto = new LicenseDto();
            updateDto.setStatus(LicenseStatus.ACTIVE);

            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));

            // When & Then
            assertThatThrownBy(() -> licenseService.updateLicense(licenseId, updateDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("不能激活已过期的许可证");
        }
    }

    @Nested
    @DisplayName("查询许可证测试")
    class QueryLicenseTests {

        @Test
        @DisplayName("根据ID查询许可证成功")
        void shouldFindLicenseByIdSuccessfully() {
            // Given
            Long licenseId = 1L;
            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));

            // When
            LicenseDto result = licenseService.getLicenseById(licenseId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(licenseId);
            assertThat(result.getLicenseCode()).isEqualTo(testLicense.getLicenseCode());

            verify(licenseRepository).findById(licenseId);
        }

        @Test
        @DisplayName("分页查询许可证成功")
        void shouldFindLicensesWithPaginationSuccessfully() {
            // Given
            QueryParam queryParam = new QueryParam();
            queryParam.setPage(0);
            queryParam.setSize(10);

            List<License> licenses = Arrays.asList(testLicense);
            Page<License> licensePage = new PageImpl<>(licenses, PageRequest.of(0, 10), 1);

            when(licenseRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(licensePage);

            // When
            PageResponse<LicenseDto> result = licenseService.getLicenses(queryParam);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);

            verify(licenseRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("根据客户ID查询许可证")
        void shouldFindLicensesByCustomerId() {
            // Given
            Long customerId = 1L;
            QueryParam queryParam = new QueryParam();
            queryParam.getFilters().put("customerId", customerId.toString());

            List<License> licenses = Arrays.asList(testLicense);
            Page<License> licensePage = new PageImpl<>(licenses, PageRequest.of(0, 10), 1);

            when(licenseRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(licensePage);

            // When
            PageResponse<LicenseDto> result = licenseService.getLicenses(queryParam);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCustomerId()).isEqualTo(customerId);
        }
    }

    @Nested
    @DisplayName("许可证状态管理测试")
    class LicenseStatusTests {

        @Test
        @DisplayName("激活许可证")
        void shouldActivateLicense() {
            // Given
            Long licenseId = 1L;
            testLicense.setStatus(LicenseStatus.INACTIVE);
            
            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));
            when(licenseRepository.save(any(License.class)))
                    .thenReturn(testLicense);

            // When
            licenseService.activateLicense(licenseId);

            // Then
            verify(licenseRepository).findById(licenseId);
            verify(licenseRepository).save(any(License.class));
        }

        @Test
        @DisplayName("停用许可证")
        void shouldDeactivateLicense() {
            // Given
            Long licenseId = 1L;
            
            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));
            when(licenseRepository.save(any(License.class)))
                    .thenReturn(testLicense);

            // When
            licenseService.deactivateLicense(licenseId);

            // Then
            verify(licenseRepository).findById(licenseId);
            verify(licenseRepository).save(any(License.class));
        }

        @Test
        @DisplayName("延期许可证")
        void shouldExtendLicense() {
            // Given
            Long licenseId = 1L;
            LocalDateTime newExpiryDate = LocalDateTime.now().plusDays(180);
            
            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));
            when(licenseRepository.save(any(License.class)))
                    .thenReturn(testLicense);

            // When
            licenseService.extendLicense(licenseId, newExpiryDate);

            // Then
            verify(licenseRepository).findById(licenseId);
            verify(licenseRepository).save(any(License.class));
        }

        @Test
        @DisplayName("不能将延期日期设置为过去时间")
        void shouldNotExtendLicenseToPastDate() {
            // Given
            Long licenseId = 1L;
            LocalDateTime pastDate = LocalDateTime.now().minusDays(1);
            
            when(licenseRepository.findById(licenseId))
                    .thenReturn(Optional.of(testLicense));

            // When & Then
            assertThatThrownBy(() -> licenseService.extendLicense(licenseId, pastDate))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("延期日期不能是过去时间");
        }
    }

    @Nested
    @DisplayName("许可证统计测试")
    class LicenseStatisticsTests {

        @Test
        @DisplayName("获取许可证统计信息")
        void shouldGetLicenseStatistics() {
            // Given
            when(licenseRepository.count()).thenReturn(100L);
            when(licenseRepository.countByStatus(LicenseStatus.ACTIVE)).thenReturn(80L);
            when(licenseRepository.countByStatus(LicenseStatus.INACTIVE)).thenReturn(15L);
            when(licenseRepository.countByStatus(LicenseStatus.EXPIRED)).thenReturn(5L);

            // When
            var statistics = licenseService.getLicenseStatistics();

            // Then
            assertThat(statistics).isNotNull();
            assertThat(statistics.get("total")).isEqualTo(100L);
            assertThat(statistics.get("active")).isEqualTo(80L);
            assertThat(statistics.get("inactive")).isEqualTo(15L);
            assertThat(statistics.get("expired")).isEqualTo(5L);
        }

        @Test
        @DisplayName("获取即将过期的许可证")
        void shouldGetExpiringLicenses() {
            // Given
            int days = 30;
            LocalDateTime expiryThreshold = LocalDateTime.now().plusDays(days);
            
            when(licenseRepository.findByStatusAndExpiresAtBefore(
                    LicenseStatus.ACTIVE, expiryThreshold))
                    .thenReturn(Arrays.asList(testLicense));

            // When
            List<LicenseDto> result = licenseService.getExpiringLicenses(days);

            // Then
            assertThat(result).isNotNull();
            assertThat(result).hasSize(1);
            
            verify(licenseRepository).findByStatusAndExpiresAtBefore(
                    LicenseStatus.ACTIVE, expiryThreshold);
        }
    }
}