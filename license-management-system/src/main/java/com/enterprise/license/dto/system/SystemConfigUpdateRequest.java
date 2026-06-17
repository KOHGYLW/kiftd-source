package com.enterprise.license.dto.system;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

/**
 * 系统配置更新请求DTO
 */
@Data
@Schema(description = "系统配置更新请求")
public class SystemConfigUpdateRequest {

    @Schema(description = "系统名称", example = "授权管理系统")
    private String systemName;

    @Schema(description = "JWT过期时间（小时）", example = "24")
    private Integer jwtExpirationHours;

    @Schema(description = "最大登录失败次数", example = "5")
    private Integer maxLoginFailures;

    @Schema(description = "密码最小长度", example = "8")
    private Integer minPasswordLength;

    @Schema(description = "密码复杂度要求", example = "true")
    private Boolean passwordComplexityRequired;

    @Schema(description = "会话超时时间（分钟）", example = "30")
    private Integer sessionTimeoutMinutes;

    @Schema(description = "允许的文件上传大小（MB）", example = "10")
    private Integer maxFileUploadSizeMB;

    @Schema(description = "启用邮件通知", example = "true")
    private Boolean emailNotificationEnabled;

    @Schema(description = "SMTP服务器配置")
    private Map<String, String> smtpConfig;

    @Schema(description = "系统备份配置")
    private Map<String, Object> backupConfig;

    @Schema(description = "其他配置项")
    private Map<String, Object> additionalConfig;

}