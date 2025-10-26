package com.enterprise.license.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 登录响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录响应")
public class LoginResponse {

    @Schema(description = "访问令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "令牌类型", example = "Bearer")
    private String tokenType = "Bearer";

    @Schema(description = "访问令牌过期时间")
    private LocalDateTime accessTokenExpiry;

    @Schema(description = "刷新令牌过期时间")
    private LocalDateTime refreshTokenExpiry;

    @Schema(description = "用户信息")
    private UserInfo userInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "用户信息")
    public static class UserInfo {
        @Schema(description = "用户ID", example = "1")
        private Long id;

        @Schema(description = "用户名", example = "admin")
        private String username;

        @Schema(description = "真实姓名", example = "管理员")
        private String realName;

        @Schema(description = "邮箱", example = "admin@example.com")
        private String email;

        @Schema(description = "角色", example = "ADMIN")
        private String role;

        @Schema(description = "最后登录时间")
        private LocalDateTime lastLoginTime;
    }

}