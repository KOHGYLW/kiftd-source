package com.enterprise.license.integration;

import com.enterprise.license.dto.ApiResponse;
import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.dto.auth.LoginRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 前后端API接口对接验证测试
 * 验证后端API的响应格式和前端期望的数据结构是否匹配
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("前后端API接口对接验证测试")
class FrontendBackendIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String authToken;

    @BeforeEach
    void setUp() throws Exception {
        // 获取认证令牌以便测试需要认证的接口
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        Map<String, Object> response = objectMapper.readValue(responseContent, 
                new TypeReference<Map<String, Object>>() {});
        
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        authToken = (String) data.get("accessToken");
    }

    @Nested
    @DisplayName("API响应格式验证")
    class ApiResponseFormatTests {

        @Test
        @DisplayName("验证统一API响应格式")
        void shouldReturnUnifiedApiResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/customers/statistics")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").isBoolean())
                    .andExpect(jsonPath("$.code").isNumber())
                    .andExpect(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.data").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andReturn();

            // 验证响应格式符合前端期望
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(responseContent, 
                    new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getSuccess()).isNotNull();
            assertThat(apiResponse.getCode()).isNotNull();
            assertThat(apiResponse.getMessage()).isNotNull();
            assertThat(apiResponse.getData()).isNotNull();
            assertThat(apiResponse.getTimestamp()).isNotNull();
        }

        @Test
        @DisplayName("验证错误响应格式")
        void shouldReturnUnifiedErrorResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/customers/999999")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(print())
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").exists())
                    .andExpected(jsonPath("$.message").exists())
                    .andExpect(jsonPath("$.timestamp").exists())
                    .andReturn();

            // 验证错误响应格式符合前端期望
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<Object> apiResponse = objectMapper.readValue(responseContent, 
                    new TypeReference<ApiResponse<Object>>() {});

            assertThat(apiResponse.getSuccess()).isFalse();
            assertThat(apiResponse.getCode()).isNotNull();
            assertThat(apiResponse.getMessage()).isNotEmpty();
        }
    }

    @Nested
    @DisplayName("认证API对接验证")
    class AuthApiIntegrationTests {

        @Test
        @DisplayName("登录API响应格式验证")
        void shouldReturnCorrectLoginResponseFormat() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").exists())
                    .andExpected(jsonPath("$.data.refreshToken").exists())
                    .andExpect(jsonPath("$.data.username").exists())
                    .andExpected(jsonPath("$.data.expiresIn").exists())
                    .andReturn();

            // 验证响应数据类型
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("accessToken")).isInstanceOf(String.class);
            assertThat(data.get("refreshToken")).isInstanceOf(String.class);
            assertThat(data.get("username")).isInstanceOf(String.class);
            assertThat(data.get("expiresIn")).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("登出API响应格式验证")
        void shouldReturnCorrectLogoutResponseFormat() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + authToken)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.message").value("登出成功"));
        }

        @Test
        @DisplayName("获取用户信息API响应格式验证")
        void shouldReturnCorrectUserInfoResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/auth/user")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.username").exists())
                    .andExpected(jsonPath("$.data.email").exists())
                    .andExpectedr(jsonPath("$.data.roles").isArray())
                    .andReturn();

            // 验证用户信息数据结构
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("username")).isInstanceOf(String.class);
            assertThat(data.get("email")).isInstanceOf(String.class);
            assertThat(data.get("roles")).isInstanceOf(java.util.List.class);
        }
    }

    @Nested
    @DisplayName("客户管理API对接验证")
    class CustomerApiIntegrationTests {

        @Test
        @DisplayName("创建客户API响应格式验证")
        void shouldReturnCorrectCreateCustomerResponseFormat() throws Exception {
            // Given
            CustomerDto customerDto = new CustomerDto();
            customerDto.setCustomerCode("API-TEST-001");
            customerDto.setCustomerName("API测试客户");
            customerDto.setContactPerson("测试联系人");
            customerDto.setContactPhone("13800138000");
            customerDto.setContactEmail("apitest@test.com");
            customerDto.setAddress("API测试地址");

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customerDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").exists())
                    .andExpected(jsonPath("$.data.customerCode").value("API-TEST-001"))
                    .andExpected(jsonPath("$.data.customerName").value("API测试客户"))
                    .andExpected(jsonPath("$.data.createdAt").exists())
                    .andExpected(jsonPath("$.data.updatedAt").exists())
                    .andReturn();

            // 验证客户数据结构符合前端期望
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<CustomerDto> apiResponse = objectMapper.readValue(responseContent, 
                    new TypeReference<ApiResponse<CustomerDto>>() {});

            CustomerDto responseData = apiResponse.getData();
            assertThat(responseData.getId()).isNotNull();
            assertThat(responseData.getCustomerCode()).isEqualTo("API-TEST-001");
            assertThat(responseData.getCustomerName()).isEqualTo("API测试客户");
            assertThat(responseData.getCreatedAt()).isNotNull();
            assertThat(responseData.getUpdatedAt()).isNotNull();
        }

        @Test
        @DisplayName("分页查询客户API响应格式验证")
        void shouldReturnCorrectPagedCustomersResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .param("page", "0")
                            .param("size", "10")
                            .param("keyword", "测试"))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpectedr(jsonPath("$.data.content").isArray())
                    .andExpect(jsonPath("$.data.totalElements").exists())
                    .andExpect(jsonPath("$.data.totalPages").exists())
                    .andExpected(jsonPath("$.data.size").exists())
                    .andExpect(jsonPath("$.data.number").exists())
                    .andReturn();

            // 验证分页数据结构
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("content")).isInstanceOf(java.util.List.class);
            assertThat(data.get("totalElements")).isInstanceOf(Number.class);
            assertThat(data.get("totalPages")).isInstanceOf(Number.class);
            assertThat(data.get("size")).isInstanceOf(Number.class);
            assertThat(data.get("number")).isInstanceOf(Number.class);
        }

        @Test
        @DisplayName("客户统计API响应格式验证")
        void shouldReturnCorrectCustomerStatisticsResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/customers/statistics")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.total").isNumber())
                    .andExpected(jsonPath("$.data.active").isNumber())
                    .andExpected(jsonPath("$.data.inactive").isNumber())
                    .andReturn();

            // 验证统计数据结构
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("total")).isInstanceOf(Number.class);
            assertThat(data.get("active")).isInstanceOf(Number.class);
            assertThat(data.get("inactive")).isInstanceOf(Number.class);
        }
    }

    @Nested
    @DisplayName("许可证管理API对接验证")
    class LicenseApiIntegrationTests {

        @Test
        @DisplayName("创建许可证API响应格式验证")
        void shouldReturnCorrectCreateLicenseResponseFormat() throws Exception {
            // Given - 先创建一个客户
            CustomerDto customer = new CustomerDto();
            customer.setCustomerCode("LIC-TEST-CUSTOMER");
            customer.setCustomerName("许可证测试客户");
            customer.setContactPerson("测试联系人");
            customer.setContactPhone("13800138000");
            customer.setContactEmail("lictest@test.com");
            customer.setAddress("测试地址");

            MvcResult customerResult = mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer))
                            .with(csrf()))
                    .andExpected(status().isCreated())
                    .andReturn();

            String customerResponse = customerResult.getResponse().getContentAsString();
            ApiResponse<CustomerDto> customerApiResponse = objectMapper.readValue(customerResponse, 
                    new TypeReference<ApiResponse<CustomerDto>>() {});
            Long customerId = customerApiResponse.getData().getId();

            // Given - 创建许可证
            LicenseDto licenseDto = new LicenseDto();
            licenseDto.setLicenseCode("API-LIC-001");
            licenseDto.setCustomerId(customerId);
            licenseDto.setLicenseType(LicenseType.STANDARD);
            licenseDto.setProductName("API测试产品");
            licenseDto.setProductVersion("1.0.0");
            licenseDto.setMaxUsers(100);
            licenseDto.setExpiresAt(LocalDateTime.now().plusDays(365));

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/licenses")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(licenseDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpected(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.id").exists())
                    .andExpected(jsonPath("$.data.licenseCode").value("API-LIC-001"))
                    .andExpected(jsonPath("$.data.customerId").value(customerId))
                    .andExpectedr(jsonPath("$.data.licenseType").exists())
                    .andExpected(jsonPath("$.data.status").exists())
                    .andExpected(jsonPath("$.data.issuedAt").exists())
                    .andExpected(jsonPath("$.data.expiresAt").exists())
                    .andReturn();

            // 验证许可证数据结构
            String responseContent = result.getResponse().getContentAsString();
            ApiResponse<LicenseDto> apiResponse = objectMapper.readValue(responseContent, 
                    new TypeReference<ApiResponse<LicenseDto>>() {});

            LicenseDto responseData = apiResponse.getData();
            assertThat(responseData.getId()).isNotNull();
            assertThat(responseData.getLicenseCode()).isEqualTo("API-LIC-001");
            assertThat(responseData.getCustomerId()).isEqualTo(customerId);
            assertThat(responseData.getLicenseType()).isNotNull();
            assertThat(responseData.getStatus()).isNotNull();
            assertThat(responseData.getIssuedAt()).isNotNull();
            assertThat(responseData.getExpiresAt()).isNotNull();
        }

        @Test
        @DisplayName("许可证验证API响应格式验证")
        void shouldReturnCorrectLicenseValidationResponseFormat() throws Exception {
            // Given
            String validationRequest = """
                {
                    "licenseCode": "TEST-LICENSE-CODE",
                    "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
                    "productName": "测试产品",
                    "productVersion": "1.0.0"
                }
                """;

            // When & Then
            MvcResult result = mockMvc.perform(post("/api/licenses/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validationRequest))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.valid").isBoolean())
                    .andExpected(jsonPath("$.data.licenseCode").exists())
                    .andExpectedr(jsonPath("$.data.message").exists())
                    .andReturn();

            // 验证验证响应数据结构
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("valid")).isInstanceOf(Boolean.class);
            assertThat(data.get("licenseCode")).isInstanceOf(String.class);
            assertThat(data.get("message")).isInstanceOf(String.class);
        }
    }

    @Nested
    @DisplayName("仪表板API对接验证")
    class DashboardApiIntegrationTests {

        @Test
        @DisplayName("仪表板概览API响应格式验证")
        void shouldReturnCorrectDashboardOverviewResponseFormat() throws Exception {
            // When & Then
            MvcResult result = mockMvc.perform(get("/api/dashboard/overview")
                            .header("Authorization", "Bearer " + authToken))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.customerCount").isNumber())
                    .andExpected(jsonPath("$.data.licenseCount").isNumber())
                    .andExpected(jsonPath("$.data.activeLicenseCount").isNumber())
                    .andExpected(jsonPath("$.data.expiredLicenseCount").isNumber())
                    .andExpectedr(jsonPath("$.data.expiringLicenseCount").isNumber())
                    .andReturn();

            // 验证仪表板数据结构
            String responseContent = result.getResponse().getContentAsString();
            Map<String, Object> response = objectMapper.readValue(responseContent, 
                    new TypeReference<Map<String, Object>>() {});
            
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            assertThat(data.get("customerCount")).isInstanceOf(Number.class);
            assertThat(data.get("licenseCount")).isInstanceOf(Number.class);
            assertThat(data.get("activeLicenseCount")).isInstanceOf(Number.class);
            assertThat(data.get("expiredLicenseCount")).isInstanceOf(Number.class);
            assertThat(data.get("expiringLicenseCount")).isInstanceOf(Number.class);
        }
    }

    @Nested
    @DisplayName("HTTP状态码验证")
    class HttpStatusCodeTests {

        @Test
        @DisplayName("验证各种HTTP状态码响应")
        void shouldReturnCorrectHttpStatusCodes() throws Exception {
            // 200 OK
            mockMvc.perform(get("/api/customers/statistics")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpected(status().isOk());

            // 401 Unauthorized
            mockMvc.perform(get("/api/customers/statistics"))
                    .andExpected(status().isUnauthorized());

            // 404 Not Found
            mockMvc.perform(get("/api/customers/999999")
                            .header("Authorization", "Bearer " + authToken))
                    .andExpected(status().isNotFound());

            // 400 Bad Request
            mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"invalid\": \"data\"}")
                            .with(csrf()))
                    .andExpected(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("CORS验证")
    class CorsTests {

        @Test
        @DisplayName("验证CORS响应头")
        void shouldReturnCorrectCorsHeaders() throws Exception {
            // When & Then
            mockMvc.perform(options("/api/customers")
                            .header("Origin", "http://localhost:3000")
                            .header("Access-Control-Request-Method", "GET"))
                    .andDo(print())
                    .andExpected(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
                    .andExpected(header().exists("Access-Control-Allow-Methods"))
                    .andExpected(header().exists("Access-Control-Allow-Headers"));
        }
    }
}