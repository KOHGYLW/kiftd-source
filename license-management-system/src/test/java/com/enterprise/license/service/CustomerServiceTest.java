package com.enterprise.license.service;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.PageResponse;
import com.enterprise.license.dto.QueryParam;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.exception.ResourceNotFoundException;
import com.enterprise.license.exception.ValidationException;
import com.enterprise.license.repository.CustomerRepository;
import com.enterprise.license.repository.LicenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
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
 * CustomerService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerService 单元测试")
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private LicenseRepository licenseRepository;

    @Mock
    private CacheManager cacheManager;

    @InjectMocks
    private CustomerService customerService;

    private CustomerDto testCustomerDto;
    private Customer testCustomer;

    @BeforeEach
    void setUp() {
        // 初始化测试数据
        testCustomerDto = new CustomerDto();
        testCustomerDto.setCustomerCode("TEST001");
        testCustomerDto.setCustomerName("测试客户");
        testCustomerDto.setContactPerson("张三");
        testCustomerDto.setContactPhone("13800138001");
        testCustomerDto.setContactEmail("zhangsan@test.com");
        testCustomerDto.setAddress("北京市朝阳区");
        testCustomerDto.setStatus(CustomerStatus.ACTIVE);

        testCustomer = new Customer();
        testCustomer.setId(1L);
        testCustomer.setCustomerCode("TEST001");
        testCustomer.setCustomerName("测试客户");
        testCustomer.setContactPerson("张三");
        testCustomer.setContactPhone("13800138001");
        testCustomer.setContactEmail("zhangsan@test.com");
        testCustomer.setAddress("北京市朝阳区");
        testCustomer.setStatus(CustomerStatus.ACTIVE);
        testCustomer.setCreatedAt(LocalDateTime.now());
        testCustomer.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("创建客户测试")
    class CreateCustomerTests {

        @Test
        @DisplayName("成功创建客户")
        void shouldCreateCustomerSuccessfully() {
            // Given
            when(customerRepository.existsByCustomerCode(testCustomerDto.getCustomerCode()))
                    .thenReturn(false);
            when(customerRepository.save(any(Customer.class)))
                    .thenReturn(testCustomer);

            // When
            CustomerDto result = customerService.createCustomer(testCustomerDto);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getCustomerCode()).isEqualTo(testCustomerDto.getCustomerCode());
            assertThat(result.getCustomerName()).isEqualTo(testCustomerDto.getCustomerName());
            assertThat(result.getContactPerson()).isEqualTo(testCustomerDto.getContactPerson());

            verify(customerRepository).existsByCustomerCode(testCustomerDto.getCustomerCode());
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("客户编码重复时抛出异常")
        void shouldThrowExceptionWhenCustomerCodeExists() {
            // Given
            when(customerRepository.existsByCustomerCode(testCustomerDto.getCustomerCode()))
                    .thenReturn(true);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(testCustomerDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("客户编码已存在");

            verify(customerRepository).existsByCustomerCode(testCustomerDto.getCustomerCode());
            verify(customerRepository, never()).save(any(Customer.class));
        }

        @Test
        @DisplayName("客户编码为空时抛出异常")
        void shouldThrowExceptionWhenCustomerCodeIsEmpty() {
            // Given
            testCustomerDto.setCustomerCode("");

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(testCustomerDto))
                    .isInstanceOf(ValidationException.class);
        }

        @Test
        @DisplayName("客户名称为空时抛出异常")
        void shouldThrowExceptionWhenCustomerNameIsEmpty() {
            // Given
            testCustomerDto.setCustomerName("");

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(testCustomerDto))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("更新客户测试")
    class UpdateCustomerTests {

        @Test
        @DisplayName("成功更新客户")
        void shouldUpdateCustomerSuccessfully() {
            // Given
            Long customerId = 1L;
            CustomerDto updateDto = new CustomerDto();
            updateDto.setCustomerName("更新后的客户名");
            updateDto.setContactPerson("李四");
            updateDto.setStatus(CustomerStatus.INACTIVE);

            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.of(testCustomer));
            when(customerRepository.save(any(Customer.class)))
                    .thenReturn(testCustomer);

            // When
            CustomerDto result = customerService.updateCustomer(customerId, updateDto);

            // Then
            assertThat(result).isNotNull();
            verify(customerRepository).findById(customerId);
            verify(customerRepository).save(any(Customer.class));
        }

        @Test
        @DisplayName("更新不存在的客户时抛出异常")
        void shouldThrowExceptionWhenCustomerNotFound() {
            // Given
            Long customerId = 999L;
            CustomerDto updateDto = new CustomerDto();
            updateDto.setCustomerName("更新后的客户名");

            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customerService.updateCustomer(customerId, updateDto))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");

            verify(customerRepository).findById(customerId);
            verify(customerRepository, never()).save(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("查询客户测试")
    class QueryCustomerTests {

        @Test
        @DisplayName("根据ID查询客户成功")
        void shouldFindCustomerByIdSuccessfully() {
            // Given
            Long customerId = 1L;
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.of(testCustomer));

            // When
            CustomerDto result = customerService.getCustomerById(customerId);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(customerId);
            assertThat(result.getCustomerCode()).isEqualTo(testCustomer.getCustomerCode());

            verify(customerRepository).findById(customerId);
        }

        @Test
        @DisplayName("查询不存在的客户时抛出异常")
        void shouldThrowExceptionWhenCustomerIdNotFound() {
            // Given
            Long customerId = 999L;
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customerService.getCustomerById(customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");

            verify(customerRepository).findById(customerId);
        }

        @Test
        @DisplayName("分页查询客户成功")
        void shouldFindCustomersWithPaginationSuccessfully() {
            // Given
            QueryParam queryParam = new QueryParam();
            queryParam.setPage(0);
            queryParam.setSize(10);
            queryParam.setKeyword("测试");

            List<Customer> customers = Arrays.asList(testCustomer);
            Page<Customer> customerPage = new PageImpl<>(customers, PageRequest.of(0, 10), 1);

            when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(customerPage);

            // When
            PageResponse<CustomerDto> result = customerService.getCustomers(queryParam);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);

            verify(customerRepository).findAll(any(Specification.class), any(Pageable.class));
        }

        @Test
        @DisplayName("根据状态筛选客户")
        void shouldFilterCustomersByStatus() {
            // Given
            QueryParam queryParam = new QueryParam();
            queryParam.setPage(0);
            queryParam.setSize(10);
            queryParam.getFilters().put("status", CustomerStatus.ACTIVE.toString());

            List<Customer> customers = Arrays.asList(testCustomer);
            Page<Customer> customerPage = new PageImpl<>(customers, PageRequest.of(0, 10), 1);

            when(customerRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(customerPage);

            // When
            PageResponse<CustomerDto> result = customerService.getCustomers(queryParam);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getStatus()).isEqualTo(CustomerStatus.ACTIVE);

            verify(customerRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("删除客户测试")
    class DeleteCustomerTests {

        @Test
        @DisplayName("成功删除客户")
        void shouldDeleteCustomerSuccessfully() {
            // Given
            Long customerId = 1L;
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.of(testCustomer));
            when(licenseRepository.countByCustomerId(customerId))
                    .thenReturn(0L);

            // When
            assertThatNoException().isThrownBy(() -> 
                customerService.deleteCustomer(customerId));

            // Then
            verify(customerRepository).findById(customerId);
            verify(licenseRepository).countByCustomerId(customerId);
            verify(customerRepository).delete(testCustomer);
        }

        @Test
        @DisplayName("删除有关联许可证的客户时抛出异常")
        void shouldThrowExceptionWhenCustomerHasLicenses() {
            // Given
            Long customerId = 1L;
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.of(testCustomer));
            when(licenseRepository.countByCustomerId(customerId))
                    .thenReturn(2L);

            // When & Then
            assertThatThrownBy(() -> customerService.deleteCustomer(customerId))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("存在关联的许可证");

            verify(customerRepository).findById(customerId);
            verify(licenseRepository).countByCustomerId(customerId);
            verify(customerRepository, never()).delete(any(Customer.class));
        }

        @Test
        @DisplayName("删除不存在的客户时抛出异常")
        void shouldThrowExceptionWhenDeletingNonExistentCustomer() {
            // Given
            Long customerId = 999L;
            when(customerRepository.findById(customerId))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> customerService.deleteCustomer(customerId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("客户不存在");

            verify(customerRepository).findById(customerId);
            verify(licenseRepository, never()).countByCustomerId(any());
            verify(customerRepository, never()).delete(any(Customer.class));
        }
    }

    @Nested
    @DisplayName("业务逻辑测试")
    class BusinessLogicTests {

        @Test
        @DisplayName("获取客户统计信息")
        void shouldGetCustomerStatistics() {
            // Given
            when(customerRepository.count()).thenReturn(100L);
            when(customerRepository.countByStatus(CustomerStatus.ACTIVE)).thenReturn(80L);
            when(customerRepository.countByStatus(CustomerStatus.INACTIVE)).thenReturn(20L);

            // When
            var statistics = customerService.getCustomerStatistics();

            // Then
            assertThat(statistics).isNotNull();
            assertThat(statistics.get("total")).isEqualTo(100L);
            assertThat(statistics.get("active")).isEqualTo(80L);
            assertThat(statistics.get("inactive")).isEqualTo(20L);

            verify(customerRepository).count();
            verify(customerRepository).countByStatus(CustomerStatus.ACTIVE);
            verify(customerRepository).countByStatus(CustomerStatus.INACTIVE);
        }

        @Test
        @DisplayName("验证联系人电话格式")
        void shouldValidateContactPhoneFormat() {
            // Given
            testCustomerDto.setContactPhone("invalid-phone");
            when(customerRepository.existsByCustomerCode(testCustomerDto.getCustomerCode()))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(testCustomerDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("电话号码格式不正确");
        }

        @Test
        @DisplayName("验证联系人邮箱格式")
        void shouldValidateContactEmailFormat() {
            // Given
            testCustomerDto.setContactEmail("invalid-email");
            when(customerRepository.existsByCustomerCode(testCustomerDto.getCustomerCode()))
                    .thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> customerService.createCustomer(testCustomerDto))
                    .isInstanceOf(ValidationException.class)
                    .hasMessageContaining("邮箱格式不正确");
        }
    }
}