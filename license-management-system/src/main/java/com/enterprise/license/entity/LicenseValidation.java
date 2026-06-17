package com.enterprise.license.entity;

import com.enterprise.license.enums.ValidationResult;
import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

/**
 * 授权验证日志实体类
 * 记录每次授权验证的详细信息，用于审计和分析
 */
@Entity
@Table(name = "license_validations", indexes = {
    @Index(name = "idx_license_validation_license_id", columnList = "license_id"),
    @Index(name = "idx_license_validation_result", columnList = "validation_result"),
    @Index(name = "idx_license_validation_time", columnList = "validation_time"),
    @Index(name = "idx_license_validation_client_ip", columnList = "client_ip"),
    @Index(name = "idx_license_validation_created_time", columnList = "created_time")
})
@Data
@EqualsAndHashCode(callSuper = true, exclude = {"license"})
@ToString(callSuper = true, exclude = {"license"})
@Comment("授权验证日志表")
public class LicenseValidation extends BaseEntity {

    /**
     * 所属授权
     */
    @NotNull(message = "所属授权不能为空")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", nullable = false, foreignKey = @ForeignKey(name = "fk_license_validation_license"))
    @JsonBackReference
    @Comment("所属授权")
    private License license;

    /**
     * 验证时间
     */
    @NotNull(message = "验证时间不能为空")
    @Column(name = "validation_time", nullable = false)
    @Comment("验证时间")
    private LocalDateTime validationTime;

    /**
     * 验证结果
     */
    @NotNull(message = "验证结果不能为空")
    @Enumerated(EnumType.STRING)
    @Column(name = "validation_result", length = 50, nullable = false)
    @Comment("验证结果")
    private ValidationResult validationResult;

    /**
     * 客户端IP地址
     */
    @Size(max = 45, message = "客户端IP地址长度不能超过45个字符")
    @Pattern(regexp = "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$|^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$", message = "IP地址格式不正确")
    @Column(name = "client_ip", length = 45)
    @Comment("客户端IP地址")
    private String clientIp;

    /**
     * 客户端MAC地址
     */
    @Size(max = 17, message = "MAC地址长度不能超过17个字符")
    @Pattern(regexp = "^([0-9A-Fa-f]{2}[:-]){5}([0-9A-Fa-f]{2})$", message = "MAC地址格式不正确")
    @Column(name = "client_mac", length = 17)
    @Comment("客户端MAC地址")
    private String clientMac;

    /**
     * 客户端硬件指纹
     */
    @Size(max = 500, message = "硬件指纹长度不能超过500个字符")
    @Column(name = "hardware_fingerprint", length = 500)
    @Comment("客户端硬件指纹")
    private String hardwareFingerprint;

    /**
     * 客户端操作系统信息
     */
    @Size(max = 200, message = "操作系统信息长度不能超过200个字符")
    @Column(name = "operating_system", length = 200)
    @Comment("客户端操作系统信息")
    private String operatingSystem;

    /**
     * 客户端应用程序版本
     */
    @Size(max = 50, message = "应用程序版本长度不能超过50个字符")
    @Column(name = "application_version", length = 50)
    @Comment("客户端应用程序版本")
    private String applicationVersion;

    /**
     * 请求的功能模块
     */
    @Size(max = 200, message = "功能模块长度不能超过200个字符")
    @Column(name = "requested_feature", length = 200)
    @Comment("请求的功能模块")
    private String requestedFeature;

    /**
     * 验证方式
     */
    @Size(max = 50, message = "验证方式长度不能超过50个字符")
    @Column(name = "validation_method", length = 50)
    @Comment("验证方式")
    private String validationMethod;

    /**
     * 验证用时（毫秒）
     */
    @Min(value = 0, message = "验证用时不能为负数")
    @Column(name = "validation_duration_ms")
    @Comment("验证用时（毫秒）")
    private Long validationDurationMs;

    /**
     * 是否在线验证
     */
    @Column(name = "is_online_validation", columnDefinition = "BOOLEAN DEFAULT TRUE")
    @Comment("是否在线验证")
    private Boolean isOnlineValidation = true;

    /**
     * 验证服务器地址
     */
    @Size(max = 100, message = "验证服务器地址长度不能超过100个字符")
    @Column(name = "validation_server", length = 100)
    @Comment("验证服务器地址")
    private String validationServer;

    /**
     * 会话ID
     */
    @Size(max = 64, message = "会话ID长度不能超过64个字符")
    @Column(name = "session_id", length = 64)
    @Comment("会话ID")
    private String sessionId;

    /**
     * 用户标识
     */
    @Size(max = 100, message = "用户标识长度不能超过100个字符")
    @Column(name = "user_identifier", length = 100)
    @Comment("用户标识")
    private String userIdentifier;

    /**
     * 验证前授权状态
     */
    @Size(max = 20, message = "验证前授权状态长度不能超过20个字符")
    @Column(name = "license_status_before", length = 20)
    @Comment("验证前授权状态")
    private String licenseStatusBefore;

    /**
     * 验证后授权状态
     */
    @Size(max = 20, message = "验证后授权状态长度不能超过20个字符")
    @Column(name = "license_status_after", length = 20)
    @Comment("验证后授权状态")
    private String licenseStatusAfter;

    /**
     * 失败原因
     */
    @Size(max = 1000, message = "失败原因长度不能超过1000个字符")
    @Column(name = "failure_reason", length = 1000)
    @Comment("失败原因")
    private String failureReason;

    /**
     * 错误代码
     */
    @Size(max = 20, message = "错误代码长度不能超过20个字符")
    @Column(name = "error_code", length = 20)
    @Comment("错误代码")
    private String errorCode;

    /**
     * 验证请求数据（JSON格式）
     */
    @Size(max = 2000, message = "验证请求数据长度不能超过2000个字符")
    @Column(name = "request_data", length = 2000)
    @Comment("验证请求数据")
    private String requestData;

    /**
     * 验证响应数据（JSON格式）
     */
    @Size(max = 2000, message = "验证响应数据长度不能超过2000个字符")
    @Column(name = "response_data", length = 2000)
    @Comment("验证响应数据")
    private String responseData;

    /**
     * 地理位置信息
     */
    @Size(max = 200, message = "地理位置信息长度不能超过200个字符")
    @Column(name = "geo_location", length = 200)
    @Comment("地理位置信息")
    private String geoLocation;

    /**
     * 风险评分（0-100）
     */
    @Min(value = 0, message = "风险评分不能小于0")
    @Max(value = 100, message = "风险评分不能大于100")  
    @Column(name = "risk_score")
    @Comment("风险评分")
    private Integer riskScore;

    /**
     * 是否可疑活动
     */
    @Column(name = "is_suspicious", columnDefinition = "BOOLEAN DEFAULT FALSE")
    @Comment("是否可疑活动")
    private Boolean isSuspicious = false;

    /**
     * 备注信息
     */
    @Size(max = 1000, message = "备注信息长度不能超过1000个字符")
    @Column(name = "remarks", length = 1000)
    @Comment("备注信息")
    private String remarks;

    /**
     * 获取验证结果描述
     * @return 验证结果描述
     */
    public String getValidationResultDescription() {
        return validationResult != null ? validationResult.getDescription() : "";
    }

    /**
     * 判断验证是否成功
     * @return true如果验证成功
     */
    public boolean isValidationSuccessful() {
        return validationResult != null && validationResult.isSuccess();
    }

    /**
     * 判断是否为在线验证
     * @return true如果是在线验证
     */
    public boolean isOnline() {
        return Boolean.TRUE.equals(isOnlineValidation);
    }

    /**
     * 判断是否为可疑活动
     * @return true如果是可疑活动
     */
    public boolean isSuspiciousActivity() {
        return Boolean.TRUE.equals(isSuspicious);
    }

    /**
     * 设置验证结果为成功
     */
    public void setValidationSuccess() {
        this.validationResult = ValidationResult.SUCCESS;
        this.failureReason = null;
        this.errorCode = null;
    }

    /**
     * 设置验证结果为失败
     * @param result 失败的验证结果
     * @param reason 失败原因
     * @param errorCode 错误代码
     */
    public void setValidationFailure(ValidationResult result, String reason, String errorCode) {
        this.validationResult = result;
        this.failureReason = reason;
        this.errorCode = errorCode;
    }

    /**
     * 标记为可疑活动
     * @param reason 可疑原因
     */
    public void markAsSuspicious(String reason) {
        this.isSuspicious = true;
        if (this.remarks == null) {
            this.remarks = "可疑活动: " + reason;
        } else {
            this.remarks += "; 可疑活动: " + reason;
        }
    }

    /**
     * 计算验证用时（秒）
     * @return 验证用时（秒）
     */
    public double getValidationDurationSeconds() {
        return validationDurationMs != null ? validationDurationMs / 1000.0 : 0.0;
    }
}