package com.enterprise.license.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 密码重置确认请求DTO
 */
@Data
@Schema(description = "密码重置确认请求")
public class PasswordResetConfirmRequest {

    @NotBlank(message = "重置令牌不能为空")
    @Schema(description = "重置令牌", example = "abc123def456", required = true)
    private String token;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, message = "密码长度不能少于6位")
    @Schema(description = "新密码", example = "newpassword123", required = true)
    private String newPassword;

    @NotBlank(message = "确认密码不能为空")
    @Schema(description = "确认密码", example = "newpassword123", required = true)
    private String confirmPassword;

}