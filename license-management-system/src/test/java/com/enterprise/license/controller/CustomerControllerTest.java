package com.enterprise.license.controller;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.service.CustomerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CustomerController Web层集成测试
 */
@WebMvcTest(CustomerController.class)
@ActiveProfiles("test")
@DisplayName("CustomerController Web层集成测试")
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    private CustomerDto testCustomerDto;

    @BeforeEach
    void setUp() {
        testCustomerDto = new CustomerDto();
        testCustomerDto.setId(1L);
        testCustomerDto.setCustomerCode("TEST001");
        testCustomerDto.setCustomerName("测试客户");
        testCustomerDto.setContactPerson("张三");
        testCustomerDto.setContactPhone("13800138001");
        testCustomerDto.setContactEmail("zhangsan@test.com");
        testCustomerDto.setAddress("北京市朝阳区");
        testCustomerDto.setCreatedAt(LocalDateTime.now());
        testCustomerDto.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("创建客户API测试")
    class CreateCustomerTests {

        @Test
        @DisplayName("成功创建客户")
        @WithMockUser(roles = "ADMIN")
        void shouldCreateCustomerSuccessfully() throws Exception {
            // Given
            CustomerDto requestDto = new CustomerDto();
            requestDto.setCustomerCode("NEW001");
            requestDto.setCustomerName("新客户");
            requestDto.setContactPerson("李四");
            requestDto.setContactPhone("13800138002");
            requestDto.setContactEmail("lisi@test.com");
            requestDto.setAddress("上海市浦东区");

            when(customerService.createCustomer(any(CustomerDto.class)))
                    .thenReturn(testCustomerDto);

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.customerCode").value("TEST001"))
                    .andExpect(jsonPath("$.data.customerName").value("测试客户"))
                    .andExpect(jsonPath("$.data.contactPerson").value("张三"));

            verify(customerService).createCustomer(any(CustomerDto.class));
        }

        @Test
        @DisplayName("创建客户时缺少必要字段返回400")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn400WhenRequiredFieldsMissing() throws Exception {
            // Given
            CustomerDto invalidDto = new CustomerDto();
            invalidDto.setCustomerName("只有名称"); // 缺少客户编码

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest());

            verify(customerService, never()).createCustomer(any(CustomerDto.class));
        }

        @Test
        @DisplayName("未认证用户创建客户返回401")
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            // Given
            CustomerDto requestDto = new CustomerDto();
            requestDto.setCustomerCode("NEW001");
            requestDto.setCustomerName("新客户");

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());

            verify(customerService, never()).createCustomer(any(CustomerDto.class));
        }

        @Test
        @DisplayName("权限不足创建客户返回403")
        @WithMockUser(roles = "USER") // 普通用户没有创建权限
        void shouldReturn403WhenInsufficientPermissions() throws Exception {
            // Given
            CustomerDto requestDto = new CustomerDto();
            requestDto.setCustomerCode("NEW001");
            requestDto.setCustomerName("新客户");

            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(requestDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());

            verify(customerService, never()).createCustomer(any(CustomerDto.class));
        }
    }

    @Nested
    @DisplayName("查询客户API测试")
    class QueryCustomerTests {

        @Test
        @DisplayName("根据ID查询客户成功")
        @WithMockUser(roles = "USER")
        void shouldGetCustomerByIdSuccessfully() throws Exception {
            // Given
            Long customerId = 1L;
            when(customerService.getCustomerById(customerId))
                    .thenReturn(testCustomerDto);

            // When & Then
            mockMvc.perform(get("/api/customers/{id}", customerId))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.customerCode").value("TEST001"))
                    .andExpect(jsonPath("$.data.customerName").value("测试客户"));

            verify(customerService).getCustomerById(customerId);
        }

        @Test
        @DisplayName("查询不存在的客户返回404")
        @WithMockUser(roles = "USER")
        void shouldReturn404WhenCustomerNotFound() throws Exception {
            // Given
            Long customerId = 999L;
            when(customerService.getCustomerById(customerId))
                    .thenThrow(new ResourceNotFoundException("客户不存在"));

            // When & Then
            mockMvc.perform(get("/api/customers/{id}", customerId))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("客户不存在"));

            verify(customerService).getCustomerById(customerId);
        }

        @Test
        @DisplayName("分页查询客户成功")
        @WithMockUser(roles = "USER")
        void shouldGetCustomersWithPaginationSuccessfully() throws Exception {
            // Given
            List<CustomerDto> customers = Arrays.asList(testCustomerDto);
            PageResponse<CustomerDto> pageResponse = new PageResponse<>();
            pageResponse.setContent(customers);
            pageResponse.setTotalElements(1L);
            pageResponse.setTotalPages(1);
            pageResponse.setSize(10);
            pageResponse.setNumber(0);

            when(customerService.getCustomers(any()))
                    .thenReturn(pageResponse);

            // When & Then
            mockMvc.perform(get("/api/customers")
                            .param("page", "0")
                            .param("size", "10")
                            .param("keyword", "测试"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.content").isNotEmpty())
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.totalPages").value(1));

            verify(customerService).getCustomers(any());
        }
    }

    @Nested
    @DisplayName("更新客户API测试")
    class UpdateCustomerTests {

        @Test
        @DisplayName("成功更新客户")
        @WithMockUser(roles = "ADMIN")
        void shouldUpdateCustomerSuccessfully() throws Exception {
            // Given
            Long customerId = 1L;
            CustomerDto updateDto = new CustomerDto();
            updateDto.setCustomerName("更新后的客户名");
            updateDto.setContactPerson("更新后的联系人");

            when(customerService.updateCustomer(eq(customerId), any(CustomerDto.class)))
                    .thenReturn(testCustomerDto);

            // When & Then
            mockMvc.perform(put("/api/customers/{id}", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.customerCode").value("TEST001"));

            verify(customerService).updateCustomer(eq(customerId), any(CustomerDto.class));
        }

        @Test
        @DisplayName("更新不存在的客户返回404")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenUpdatingNonExistentCustomer() throws Exception {
            // Given
            Long customerId = 999L;
            CustomerDto updateDto = new CustomerDto();
            updateDto.setCustomerName("更新后的客户名");

            when(customerService.updateCustomer(eq(customerId), any(CustomerDto.class)))
                    .thenThrow(new ResourceNotFoundException("客户不存在"));

            // When & Then
            mockMvc.perform(put("/api/customers/{id}", customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("客户不存在"));

            verify(customerService).updateCustomer(eq(customerId), any(CustomerDto.class));
        }
    }

    @Nested
    @DisplayName("删除客户API测试")
    class DeleteCustomerTests {

        @Test
        @DisplayName("成功删除客户")
        @WithMockUser(roles = "ADMIN")
        void shouldDeleteCustomerSuccessfully() throws Exception {
            // Given
            Long customerId = 1L;
            doNothing().when(customerService).deleteCustomer(customerId);

            // When & Then
            mockMvc.perform(delete("/api/customers/{id}", customerId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("删除成功"));

            verify(customerService).deleteCustomer(customerId);
        }

        @Test
        @DisplayName("删除不存在的客户返回404")
        @WithMockUser(roles = "ADMIN")
        void shouldReturn404WhenDeletingNonExistentCustomer() throws Exception {
            // Given
            Long customerId = 999L;
            doThrow(new ResourceNotFoundException("客户不存在"))
                    .when(customerService).deleteCustomer(customerId);

            // When & Then
            mockMvc.perform(delete("/api/customers/{id}", customerId)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpected(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("客户不存在"));

            verify(customerService).deleteCustomer(customerId);
        }
    }

    @Nested
    @DisplayName("客户统计API测试")
    class CustomerStatisticsTests {

        @Test
        @DisplayName("获取客户统计信息成功")
        @WithMockUser(roles = "USER")
        void shouldGetCustomerStatisticsSuccessfully() throws Exception {
            // Given
            Map<String, Object> statistics = Map.of(
                    "total", 100L,
                    "active", 80L,
                    "inactive", 20L
            );
            when(customerService.getCustomerStatistics()).thenReturn(statistics);

            // When & Then
            mockMvc.perform(get("/api/customers/statistics"))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.total").value(100))
                    .andExpect(jsonPath("$.data.active").value(80))
                    .andExpect(jsonPath("$.data.inactive").value(20));

            verify(customerService).getCustomerStatistics();
        }
    }

    @Nested
    @DisplayName("批量操作API测试")
    class BatchOperationTests {

        @Test
        @DisplayName("批量导入客户成功")
        @WithMockUser(roles = "ADMIN")
        void shouldBatchImportCustomersSuccessfully() throws Exception {
            // Given
            List<CustomerDto> customers = Arrays.asList(testCustomerDto);
            BatchOperationResult result = new BatchOperationResult();
            result.setTotalCount(1);
            result.setSuccessCount(1);
            result.setFailureCount(0);

            when(customerService.batchImportCustomers(any())).thenReturn(result);

            // When & Then
            mockMvc.perform(post("/api/customers/batch-import")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customers))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.totalCount").value(1))
                    .andExpected(jsonPath("$.data.successCount").value(1))
                    .andExpected(jsonPath("$.data.failureCount").value(0));

            verify(customerService).batchImportCustomers(any());
        }

        @Test
        @DisplayName("批量删除客户成功")
        @WithMockUser(roles = "ADMIN")
        void shouldBatchDeleteCustomersSuccessfully() throws Exception {
            // Given
            List<Long> customerIds = Arrays.asList(1L, 2L, 3L);
            BatchOperationResult result = new BatchOperationResult();
            result.setTotalCount(3);
            result.setSuccessCount(2);
            result.setFailureCount(1);

            when(customerService.batchDeleteCustomers(customerIds)).thenReturn(result);

            // When & Then
            mockMvc.perform(delete("/api/customers/batch")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customerIds))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.totalCount").value(3))
                    .andExpected(jsonPath("$.data.successCount").value(2))
                    .andExpect(jsonPath("$.data.failureCount").value(1));

            verify(customerService).batchDeleteCustomers(customerIds);
        }
    }

    @Nested
    @DisplayName("错误处理测试")
    class ErrorHandlingTests {

        @Test
        @DisplayName("服务层异常时返回500")
        @WithMockUser(roles = "USER")
        void shouldReturn500WhenServiceThrowsException() throws Exception {
            // Given
            Long customerId = 1L;
            when(customerService.getCustomerById(customerId))
                    .thenThrow(new RuntimeException("数据库连接异常"));

            // When & Then
            mockMvc.perform(get("/api/customers/{id}", customerId))
                    .andDo(print())
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").exists());

            verify(customerService).getCustomerById(customerId);
        }

        @Test
        @DisplayName("参数格式错误时返回400")
        @WithMockUser(roles = "USER")
        void shouldReturn400WhenParameterFormatError() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers/{id}", "invalid-id"))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(customerService, never()).getCustomerById(any());
        }
    }
}