package com.enterprise.license.controller;

import com.enterprise.license.dto.ApiResponse;
import com.enterprise.license.security.LicenseUserDetailsService;
import com.enterprise.license.util.JwtTokenUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "认证管理", description = "用户认证和授权相关接口")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private LicenseUserDetailsService userDetailsService;

    /**
     * 登录请求DTO
     */
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;

        @NotBlank(message = "密码不能为空")
        private String password;

        private String deviceId;

        // Getters and Setters
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getDeviceId() { return deviceId; }
        public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
    }

    /**
     * 刷新令牌请求DTO
     */
    public static class RefreshTokenRequest {
        @NotBlank(message = "刷新令牌不能为空")
        private String refreshToken;

        // Getters and Setters
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户名密码登录，返回JWT令牌")
    public ResponseEntity<ApiResponse<Map<String, Object>>> login(
            @Valid @RequestBody LoginRequest loginRequest,
            HttpServletRequest request) {

        try {
            logger.info("用户登录尝试: {}", loginRequest.getUsername());

            // 认证用户
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getUsername(),
                            loginRequest.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            
            // 生成设备ID（如果未提供）
            String deviceId = loginRequest.getDeviceId();
            if (deviceId == null || deviceId.trim().isEmpty()) {
                deviceId = generateDeviceId(request);
            }

            // 生成JWT令牌
            JwtTokenUtil.TokenInfo tokenInfo = jwtTokenUtil.generateTokenInfo(userDetails, deviceId);

            // 更新最后登录时间
            userDetailsService.updateLastLoginTime(userDetails.getUsername());

            // 构建响应数据
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", tokenInfo.getAccessToken());
            responseData.put("refreshToken", tokenInfo.getRefreshToken());
            responseData.put("tokenType", tokenInfo.getTokenType());
            responseData.put("expiresIn", tokenInfo.getExpiresIn());
            responseData.put("username", userDetails.getUsername());
            responseData.put("authorities", userDetails.getAuthorities());
            responseData.put("deviceId", deviceId);

            logger.info("用户登录成功: {}, 设备: {}", loginRequest.getUsername(), deviceId);

            return ResponseEntity.ok(ApiResponse.success(responseData, "登录成功"));

        } catch (Exception e) {
            logger.error("用户登录失败: {}", loginRequest.getUsername(), e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("登录失败: " + e.getMessage()));
        }
    }

    /**
     * 刷新访问令牌
     */
    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refreshToken(
            @Valid @RequestBody RefreshTokenRequest refreshRequest) {

        try {
            logger.info("令牌刷新请求");

            JwtTokenUtil.TokenInfo tokenInfo = jwtTokenUtil.refreshToken(refreshRequest.getRefreshToken());

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("accessToken", tokenInfo.getAccessToken());
            responseData.put("refreshToken", tokenInfo.getRefreshToken());
            responseData.put("tokenType", tokenInfo.getTokenType());
            responseData.put("expiresIn", tokenInfo.getExpiresIn());

            logger.info("令牌刷新成功");
            return ResponseEntity.ok(ApiResponse.success(responseData, "令牌刷新成功"));

        } catch (Exception e) {
            logger.error("令牌刷新失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("令牌刷新失败: " + e.getMessage()));
        }
    }

    /**
     * 用户登出
     */
    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "撤销当前用户令牌")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                jwtTokenUtil.revokeToken(token);
                logger.info("用户登出成功");
                return ResponseEntity.ok(ApiResponse.success(null, "登出成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的令牌"));
            }

        } catch (Exception e) {
            logger.error("用户登出失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("登出失败: " + e.getMessage()));
        }
    }

    /**
     * 撤销所有令牌
     */
    @PostMapping("/logout-all")
    @Operation(summary = "全部登出", description = "撤销用户所有设备的令牌")
    public ResponseEntity<ApiResponse<Void>> logoutAll(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null) {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                jwtTokenUtil.revokeAllUserTokens(username);
                
                logger.info("用户全部登出成功: {}", username);
                return ResponseEntity.ok(ApiResponse.success(null, "全部登出成功"));
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的令牌"));
            }

        } catch (Exception e) {
            logger.error("用户全部登出失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("全部登出失败: " + e.getMessage()));
        }
    }

    /**
     * 验证令牌
     */
    @PostMapping("/validate")
    @Operation(summary = "验证令牌", description = "验证JWT令牌是否有效")
    public ResponseEntity<ApiResponse<Map<String, Object>>> validateToken(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtTokenUtil.validateToken(token)) {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                String deviceId = jwtTokenUtil.getDeviceIdFromToken(token);
                
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("valid", true);
                responseData.put("username", username);
                responseData.put("deviceId", deviceId);
                
                return ResponseEntity.ok(ApiResponse.success(responseData, "令牌有效"));
            } else {
                Map<String, Object> responseData = new HashMap<>();
                responseData.put("valid", false);
                
                return ResponseEntity.ok(ApiResponse.success(responseData, "令牌无效"));
            }

        } catch (Exception e) {
            logger.error("令牌验证失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("令牌验证失败: " + e.getMessage()));
        }
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/user-info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的信息")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserInfo(HttpServletRequest request) {
        try {
            String token = extractTokenFromRequest(request);
            
            if (token != null && jwtTokenUtil.validateToken(token)) {
                String username = jwtTokenUtil.getUsernameFromToken(token);
                LicenseUserDetailsService.LicenseUser user = userDetailsService.getUser(username);
                
                if (user != null) {
                    Map<String, Object> responseData = new HashMap<>();
                    responseData.put("username", user.getUsername());
                    responseData.put("email", user.getEmail());
                    responseData.put("roles", user.getRoles());
                    responseData.put("enabled", user.isEnabled());
                    responseData.put("createdAt", user.getCreatedAt());
                    responseData.put("lastLoginAt", user.getLastLoginAt());
                    
                    return ResponseEntity.ok(ApiResponse.success(responseData, "获取用户信息成功"));
                } else {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("用户不存在"));
                }
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("无效的令牌"));
            }

        } catch (Exception e) {
            logger.error("获取用户信息失败", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("获取用户信息失败: " + e.getMessage()));
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * 生成设备ID
     */
    private String generateDeviceId(HttpServletRequest request) {
        StringBuilder deviceInfo = new StringBuilder();
        deviceInfo.append(request.getRemoteAddr());
        deviceInfo.append(request.getHeader("User-Agent"));
        
        return Integer.toHexString(deviceInfo.toString().hashCode()).toUpperCase();
    }
}