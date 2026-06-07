package com.enterprise.license.security;

import com.enterprise.license.config.TestDataInitializer;
import com.enterprise.license.dto.auth.LoginRequest;
import com.enterprise.license.dto.auth.LoginResponse;
import com.enterprise.license.entity.User;
import com.enterprise.license.service.LicenseUserDetailsService;
import com.enterprise.license.util.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureWebMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 安全功能集成测试
 */
@SpringBootTest
@AutoConfigureWebMvc
@ActiveProfiles("test")
@DisplayName("安全功能集成测试")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private LicenseUserDetailsService userDetailsService;

    @MockBean
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private TestDataInitializer testDataInitializer;

    private User testUser;
    private String validToken;

    @BeforeEach
    void setUp() {
        testUser = testDataInitializer.createTestUser();
        validToken = "valid-jwt-token";
    }

    @Nested
    @DisplayName("认证测试")
    class AuthenticationTests {

        @Test
        @DisplayName("用户登录成功")
        void shouldLoginSuccessfully() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                    testUser, null, testUser.getAuthorities());

            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(mockAuth);
            when(jwtTokenUtil.generateToken(any(UserDetails.class)))
                    .thenReturn(validToken);
            when(jwtTokenUtil.generateRefreshToken(any(UserDetails.class)))
                    .thenReturn("refresh-token");

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(validToken))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.username").value("testuser"));

            verify(authenticationManager).authenticate(any(Authentication.class));
            verify(jwtTokenUtil).generateToken(any(UserDetails.class));
        }

        @Test
        @DisplayName("用户名或密码错误时登录失败")
        void shouldFailLoginWithWrongCredentials() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenThrow(new BadCredentialsException("用户名或密码错误"));

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("用户名或密码错误"));

            verify(authenticationManager).authenticate(any(Authentication.class));
            verify(jwtTokenUtil, never()).generateToken(any(UserDetails.class));
        }

        @Test
        @DisplayName("缺少必要字段时登录失败")
        void shouldFailLoginWithMissingFields() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            // 缺少密码

            // When & Then
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));

            verify(authenticationManager, never()).authenticate(any(Authentication.class));
        }
    }

    @Nested
    @DisplayName("JWT令牌测试")
    class JwtTokenTests {

        @Test
        @DisplayName("有效令牌访问受保护资源成功")
        @WithMockUser(username = "testuser", roles = "USER")
        void shouldAccessProtectedResourceWithValidToken() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("无效令牌访问受保护资源失败")
        void shouldFailAccessProtectedResourceWithInvalidToken() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer invalid-token"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("令牌过期时访问受保护资源失败")
        void shouldFailAccessProtectedResourceWithExpiredToken() throws Exception {
            // Given
            String expiredToken = "expired-jwt-token";
            when(jwtTokenUtil.isTokenExpired(expiredToken)).thenReturn(true);

            // When & Then
            mockMvc.perform(get("/api/customers")
                            .header("Authorization", "Bearer " + expiredToken))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("刷新令牌成功")
        void shouldRefreshTokenSuccessfully() throws Exception {
            // Given
            String refreshToken = "valid-refresh-token";
            String newAccessToken = "new-access-token";

            when(jwtTokenUtil.validateToken(refreshToken, testUser)).thenReturn(true);
            when(jwtTokenUtil.getUsernameFromToken(refreshToken)).thenReturn("testuser");
            when(userDetailsService.loadUserByUsername("testuser")).thenReturn(testUser);
            when(jwtTokenUtil.generateToken(testUser)).thenReturn(newAccessToken);

            // When & Then
            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"refreshToken\":\"" + refreshToken + "\"}")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value(newAccessToken));

            verify(jwtTokenUtil).validateToken(refreshToken, testUser);
            verify(jwtTokenUtil).generateToken(testUser);
        }
    }

    @Nested
    @DisplayName("权限控制测试")
    class AuthorizationTests {

        @Test
        @DisplayName("管理员可以访问管理员专用API")
        @WithMockUser(username = "admin", roles = "ADMIN")
        void shouldAllowAdminAccessToAdminAPI() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerCode\":\"TEST\",\"customerName\":\"测试\"}")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("普通用户无法访问管理员专用API")
        @WithMockUser(username = "user", roles = "USER")
        void shouldDenyUserAccessToAdminAPI() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerCode\":\"TEST\",\"customerName\":\"测试\"}")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("普通用户可以访问只读API")
        @WithMockUser(username = "user", roles = "USER")
        void shouldAllowUserAccessToReadOnlyAPI() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers/1"))
                    .andDo(print())
                    .andExpected(status().isOk());
        }

        @Test
        @DisplayName("未认证用户无法访问受保护API")
        void shouldDenyAnonymousAccessToProtectedAPI() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers"))
                    .andDo(print())
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("CSRF保护测试")
    class CsrfProtectionTests {

        @Test
        @DisplayName("POST请求需要CSRF令牌")
        @WithMockUser(roles = "ADMIN")
        void shouldRequireCsrfTokenForPost() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerCode\":\"TEST\",\"customerName\":\"测试\"}"))
                    .andDo(print())
                    .andExpected(status().isForbidden()); // CSRF令牌缺失
        }

        @Test
        @DisplayName("带有CSRF令牌的POST请求成功")
        @WithMockUser(roles = "ADMIN")
        void shouldSucceedPostWithCsrfToken() throws Exception {
            // When & Then
            mockMvc.perform(post("/api/customers")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"customerCode\":\"TEST\",\"customerName\":\"测试\"}")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isCreated());
        }

        @Test
        @DisplayName("GET请求不需要CSRF令牌")
        @WithMockUser(roles = "USER")
        void shouldNotRequireCsrfTokenForGet() throws Exception {
            // When & Then
            mockMvc.perform(get("/api/customers"))
                    .andDo(print())
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("会话管理测试")
    class SessionManagementTests {

        @Test
        @DisplayName("用户登出后令牌失效")
        @WithMockUser(username = "testuser")
        void shouldInvalidateTokenAfterLogout() throws Exception {
            // Given
            String token = "user-token";

            // When
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer " + token)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpected(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("登出成功"));

            // 验证令牌被添加到黑名单
            verify(jwtTokenUtil).invalidateToken(token);
        }

        @Test
        @DisplayName("同一用户多次登录时旧令牌失效")
        void shouldInvalidateOldTokensOnNewLogin() throws Exception {
            // Given
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsername("testuser");
            loginRequest.setPassword("password123");

            Authentication mockAuth = new UsernamePasswordAuthenticationToken(
                    testUser, null, testUser.getAuthorities());

            when(authenticationManager.authenticate(any(Authentication.class)))
                    .thenReturn(mockAuth);
            when(jwtTokenUtil.generateToken(any(UserDetails.class)))
                    .thenReturn("new-token");
            when(jwtTokenUtil.generateRefreshToken(any(UserDetails.class)))
                    .thenReturn("new-refresh-token");

            // When
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest))
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk());

            // 验证旧令牌被清理
            verify(jwtTokenUtil).invalidateUserTokens("testuser");
        }
    }

    @Nested
    @DisplayName("密码安全测试")
    class PasswordSecurityTests {

        @Test
        @DisplayName("修改密码成功")
        @WithMockUser(username = "testuser")
        void shouldChangePasswordSuccessfully() throws Exception {
            // Given
            String changePasswordRequest = """
                {
                    "oldPassword": "password123",
                    "newPassword": "newPassword456",
                    "confirmPassword": "newPassword456"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(changePasswordRequest)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.message").value("密码修改成功"));
        }

        @Test
        @DisplayName("原密码错误时修改密码失败")
        @WithMockUser(username = "testuser")
        void shouldFailChangePasswordWithWrongOldPassword() throws Exception {
            // Given
            String changePasswordRequest = """
                {
                    "oldPassword": "wrongPassword",
                    "newPassword": "newPassword456",
                    "confirmPassword": "newPassword456"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(changePasswordRequest)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExpected(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("原密码错误"));
        }

        @Test
        @DisplayName("新密码不符合强度要求时修改失败")
        @WithMockUser(username = "testuser")
        void shouldFailChangePasswordWithWeakPassword() throws Exception {
            // Given
            String changePasswordRequest = """
                {
                    "oldPassword": "password123",
                    "newPassword": "123",
                    "confirmPassword": "123"
                }
                """;

            // When & Then
            mockMvc.perform(post("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(changePasswordRequest)
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isBadRequest())
                    .andExtected(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.message").value("密码强度不足"));
        }
    }

    @Nested
    @DisplayName("API限流测试")
    class RateLimitingTests {

        @Test
        @DisplayName("频繁请求时触发限流")
        void shouldTriggerRateLimitingOnFrequentRequests() throws Exception {
            // Given - 模拟频繁请求
            for (int i = 0; i < 100; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"username\":\"test\",\"password\":\"test\"}")
                                .with(csrf()))
                        .andDo(print());
            }

            // When & Then - 第101次请求应该被限流
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"username\":\"test\",\"password\":\"test\"}")
                            .with(csrf()))
                    .andDo(print())
                    .andExpect(status().isTooManyRequests())
                    .andExpected(jsonPath("$.message").value("请求过于频繁，请稍后再试"));
        }
    }
}