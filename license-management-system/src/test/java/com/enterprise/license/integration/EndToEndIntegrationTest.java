package com.enterprise.license.integration;

import com.enterprise.license.config.TestContainersConfig;
import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.dto.CustomerDto;
import com.enterprise.license.dto.LicenseDto;
import com.enterprise.license.dto.auth.LoginRequest;
import com.enterprise.license.dto.auth.LoginResponse;
import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.enterprise.license.enums.CustomerStatus;
import com.enterprise.license.enums.LicenseStatus;
import com.enterprise.license.enums.LicenseType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 端到端集成测试
 * 测试完整的业务流程：用户登录 -> 客户管理 -> 许可证管理 -> 许可证验证
 */
@SpringBootTest
@AutoConfigureWebMvc
@Import(TestContainersConfig.class)
@ActiveProfiles("integration-test")
@DisplayName("端到端集成测试")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class EndToEndIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TestDataInitializer testDataInitializer;

    private static String authToken;
    private static Long customerId;
    private static Long licenseId;
    private static String licenseCode;

    @BeforeEach
    void setUp() {
        // 每个测试前的基础数据准备在各个测试方法中处理
    }

    @Test
    @Order(1)
    @DisplayName("1. 用户登录获取访问令牌")
    void shouldLoginAndGetAccessToken() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("admin");
        loginRequest.setPassword("admin123");

        // When & Then
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.username").value("admin"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        LoginResponse loginResponse = objectMapper.readValue(response, LoginResponse.class);
        authToken = loginResponse.getData().getAccessToken();
        
        Assertions.assertNotNull(authToken, "登录后应该获得访问令牌");
    }

    @Test
    @Order(2)
    @DisplayName("2. 创建客户")
    @Transactional
    void shouldCreateCustomer() throws Exception {
        // Given
        CustomerDto customerDto = new CustomerDto();
        customerDto.setCustomerCode("E2E001");
        customerDto.setCustomerName("端到端测试客户");
        customerDto.setContactPerson("测试联系人");
        customerDto.setContactPhone("13800138000");
        customerDto.setContactEmail("e2e@test.com");
        customerDto.setAddress("测试地址");
        customerDto.setStatus(CustomerStatus.ACTIVE);

        // When & Then
        String response = mockMvc.perform(post("/api/customers")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerDto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.customerCode").value("E2E001"))
                .andExpect(jsonPath("$.data.customerName").value("端到端测试客户"))
                .andExpected(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        CustomerDto createdCustomer = objectMapper.readValue(response, CustomerDto.class);
        customerId = createdCustomer.getData().getId();
        
        Assertions.assertNotNull(customerId, "客户创建后应该有ID");
    }

    @Test
    @Order(3)
    @DisplayName("3. 查询客户信息")
    void shouldGetCustomerById() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + authToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(customerId))
                .andExpect(jsonPath("$.data.customerCode").value("E2E001"))
                .andExpect(jsonPath("$.data.customerName").value("端到端测试客户"));
    }

    @Test
    @Order(4)
    @DisplayName("4. 为客户创建许可证")
    @Transactional
    void shouldCreateLicenseForCustomer() throws Exception {
        // Given
        LicenseDto licenseDto = new LicenseDto();
        licenseDto.setLicenseCode("E2E-LIC-001");
        licenseDto.setCustomerId(customerId);
        licenseDto.setLicenseType(LicenseType.STANDARD);
        licenseDto.setProductName("端到端测试产品");
        licenseDto.setProductVersion("1.0.0");
        licenseDto.setMaxUsers(100);
        licenseDto.setExpiresAt(LocalDateTime.now().plusDays(365));

        // When & Then
        String response = mockMvc.perform(post("/api/licenses")
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(licenseDto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.licenseCode").value("E2E-LIC-001"))
                .andExpect(jsonPath("$.data.productName").value("端到端测试产品"))
                .andExpect(jsonPath("$.data.status").value(LicenseStatus.ACTIVE.toString()))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        LicenseDto createdLicense = objectMapper.readValue(response, LicenseDto.class);
        licenseId = createdLicense.getData().getId();
        licenseCode = createdLicense.getData().getLicenseCode();
        
        Assertions.assertNotNull(licenseId, "许可证创建后应该有ID");
        Assertions.assertNotNull(licenseCode, "许可证创建后应该有编码");
    }

    @Test
    @Order(5)
    @DisplayName("5. 查询客户的许可证列表")
    void shouldGetLicensesForCustomer() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/licenses")
                        .header("Authorization", "Bearer " + authToken)
                        .param("customerId", customerId.toString()))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].licenseCode").value("E2E-LIC-001"))
                .andExpect(jsonPath("$.data.content[0].customerId").value(customerId));
    }

    @Test
    @Order(6)  
    @DisplayName("6. 生成许可证密钥")
    void shouldGenerateLicenseKey() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/licenses/{id}/generate-key", licenseId)
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.licenseKey").exists())
                .andExpect(jsonPath("$.data.signature").exists())
                .andExpect(jsonPath("$.data.publicKey").exists());
    }

    @Test
    @Order(7)
    @DisplayName("7. 验证许可证（在线验证）")
    void shouldValidateLicenseOnline() throws Exception {
        // Given
        String validationRequest = """
            {
                "licenseCode": "%s",
                "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
                "productName": "端到端测试产品",
                "productVersion": "1.0.0"
            }
            """.formatted(licenseCode);

        // When & Then
        mockMvc.perform(post("/api/licenses/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validationRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.licenseCode").value(licenseCode))
                .andExpected(jsonPath("$.data.remainingDays").exists())
                .andExpect(jsonPath("$.data.maxUsers").value(100));
    }

    @Test
    @Order(8)
    @DisplayName("8. 更新许可证状态")
    @Transactional
    void shouldUpdateLicenseStatus() throws Exception {
        // Given
        LicenseDto updateDto = new LicenseDto();
        updateDto.setMaxUsers(200);
        updateDto.setExpiresAt(LocalDateTime.now().plusDays(730)); // 延期到2年

        // When & Then
        mockMvc.perform(put("/api/licenses/{id}", licenseId)
                        .header("Authorization", "Bearer " + authToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto))
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.maxUsers").value(200))
                .andExpect(jsonPath("$.data.id").value(licenseId));
    }

    @Test
    @Order(9)
    @DisplayName("9. 停用许可证")
    @Transactional
    void shouldDeactivateLicense() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/licenses/{id}/deactivate", licenseId)
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("许可证已停用"));
    }

    @Test
    @Order(10)
    @DisplayName("10. 验证停用的许可证失败")
    void shouldFailValidateDeactivatedLicense() throws Exception {
        // Given
        String validationRequest = """
            {
                "licenseCode": "%s",
                "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
                "productName": "端到端测试产品",
                "productVersion": "1.0.0"
            }
            """.formatted(licenseCode);

        // When & Then
        mockMvc.perform(post("/api/licenses/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validationRequest))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.valid").value(false))
                .andExpected(jsonPath("$.data.reason").value("许可证已停用"));
    }

    @Test
    @Order(11)
    @DisplayName("11. 重新激活许可证")
    @Transactional
    void shouldReactivateLicense() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/licenses/{id}/activate", licenseId)
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("许可证已激活"));
    }

    @Test
    @Order(12)
    @DisplayName("12. 获取仪表板统计数据")
    void shouldGetDashboardStatistics() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/dashboard/overview")
                        .header("Authorization", "Bearer " + authToken))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.data.customerCount").exists())
                .andExpect(jsonPath("$.data.licenseCount").exists())
                .andExpected(jsonPath("$.data.activeLicenseCount").exists())
                .andExpected(jsonPath("$.data.expiringLicenseCount").exists());
    }

    @Test
    @Order(13)
    @DisplayName("13. 导出客户数据")
    void shouldExportCustomerData() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/customers/export")
                        .header("Authorization", "Bearer " + authToken)
                        .param("format", "excel"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @Order(14)
    @DisplayName("14. 导出许可证数据")
    void shouldExportLicenseData() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/licenses/export")
                        .header("Authorization", "Bearer " + authToken)
                        .param("format", "excel")
                        .param("customerId", customerId.toString()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpected(header().string("Content-Type", 
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().exists("Content-Disposition"));
    }

    @Test
    @Order(15)
    @DisplayName("15. 清理测试数据 - 删除许可证")
    @Transactional
    void shouldDeleteLicense() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/licenses/{id}", licenseId)
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @Order(16)
    @DisplayName("16. 清理测试数据 - 删除客户")
    @Transactional
    void shouldDeleteCustomer() throws Exception {
        // When & Then
        mockMvc.perform(delete("/api/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("删除成功"));
    }

    @Test
    @Order(17)
    @DisplayName("17. 用户登出")
    void shouldLogoutSuccessfully() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + authToken)
                        .with(csrf()))
                .andDo(print())
                .andExpected(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpected(jsonPath("$.message").value("登出成功"));
    }

    @Nested
    @DisplayName("异常场景端到端测试")
    class ExceptionScenariosTest {

        @Test
        @DisplayName("创建重复客户编码时失败")
        void shouldFailCreateDuplicateCustomerCode() throws Exception {
            // Given - 首先创建一个客户
            CustomerDto customer1 = new CustomerDto();
            customer1.setCustomerCode("DUP001");
            customer1.setCustomerName("重复测试客户1");
            customer1.setContactPerson("联系人1");
            customer1.setContactPhone("13800138001");
            customer1.setContactEmail("dup1@test.com");

            mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer1))
                            .with(csrf()))
                    .andExpect(status().isCreated());

            // When & Then - 尝试创建相同编码的客户
            CustomerDto customer2 = new CustomerDto();
            customer2.setCustomerCode("DUP001"); // 相同编码
            customer2.setCustomerName("重复测试客户2");
            customer2.setContactPerson("联系人2");
            customer2.setContactPhone("13800138002");
            customer2.setContactEmail("dup2@test.com");

            mockMvc.perform(post("/api/customers")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(customer2))
                            .with(csrf()))
                    .andDo(print())
                    .andExpected(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpected(jsonPath("$.message").value("客户编码已存在"));
        }

        @Test
        @DisplayName("为不存在的客户创建许可证时失败")
        void shouldFailCreateLicenseForNonExistentCustomer() throws Exception {
            // Given
            LicenseDto licenseDto = new LicenseDto();
            licenseDto.setLicenseCode("FAIL-LIC-001");
            licenseDto.setCustomerId(99999L); // 不存在的客户ID
            licenseDto.setLicenseType(LicenseType.STANDARD);
            licenseDto.setProductName("失败测试产品");
            licenseDto.setProductVersion("1.0.0");
            licenseDto.setMaxUsers(100);
            licenseDto.setExpiresAt(LocalDateTime.now().plusDays(365));

            // When & Then
            mockMvc.perform(post("/api/licenses")
                            .header("Authorization", "Bearer " + authToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(licenseDto))
                            .with(csrf()))
                    .andDo(print())
                    .andExpected(status().isNotFound())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpected(jsonPath("$.message").value("客户不存在"));
        }

        @Test
        @DisplayName("验证不存在的许可证时失败")
        void shouldFailValidateNonExistentLicense() throws Exception {
            // Given
            String validationRequest = """
                {
                    "licenseCode": "NONEXISTENT-LIC",
                    "hardwareFingerprint": "MAC:AA:BB:CC:DD:EE:FF|CPU:Intel|DISK:12345",
                    "productName": "不存在的产品",
                    "productVersion": "1.0.0"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/licenses/validate")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validationRequest))
                    .andDo(print())
                    .andExpected(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpected(jsonPath("$.data.valid").value(false))
                    .andExpected(jsonPath("$.data.reason").value("许可证不存在"));
        }
    }
}