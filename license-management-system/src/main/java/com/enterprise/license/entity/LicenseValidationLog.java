package com.enterprise.license.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 授权验证日志实体类
 */
@Data
@Entity
@Table(name = "license_validation_log", indexes = {
    @Index(name = "idx_validation_license_id", columnList = "license_id"),
    @Index(name = "idx_validation_status", columnList = "validation_status"),
    @Index(name = "idx_validation_time", columnList = "validation_time"),
    @Index(name = "idx_validation_ip", columnList = "client_ip")
})
@EqualsAndHashCode(callSuper = true)
@Schema(description = "授权验证日志")
public class LicenseValidationLog extends BaseEntity {

    @NotNull(message = "授权ID不能为空")
    @Column(name = "license_id", nullable = false)
    @Schema(description = "授权ID", example = "1")
    private Long licenseId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "license_id", insertable = false, updatable = false)
    @Schema(description = "授权信息")
    private License license;

    @NotBlank(message = "客户端IP不能为空")
    @Size(max = 45, message = "客户端IP长度不能超过45位")
    @Column(name = "client_ip", length = 45, nullable = false)
    @Schema(description = "客户端IP", example = "192.168.1.100")
    private String clientIp;

    @Size(max = 256, message = "机器指纹长度不能超过256位")
    @Column(name = "machine_fingerprint", length = 256)
    @Schema(description = "机器指纹")
    private String machineFingerprint;

    @NotNull(message = "验证时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "validation_time", nullable = false)
    @Schema(description = "验证时间")
    private LocalDateTime validationTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_status", length = 16, nullable = false)
    @Schema(description = "验证状态", example = "SUCCESS")
    private ValidationStatus validationStatus;

    @Size(max = 500, message = "验证结果长度不能超过500位")
    @Column(name = "validation_result", length = 500)
    @Schema(description = "验证结果")
    private String validationResult;

    @Size(max = 1000, message = "错误信息长度不能超过1000位")
    @Column(name = "error_message", length = 1000)
    @Schema(description = "错误信息")
    private String errorMessage;

    @Size(max = 256, message = "用户代理长度不能超过256位")
    @Column(name = "user_agent", length = 256)
    @Schema(description = "用户代理")
    private String userAgent;

    @Column(name = "request_data", length = 2000)
    @Schema(description = "请求数据")
    private String requestData;

    @Column(name = "response_data", length = 2000)
    @Schema(description = "响应数据")
    private String responseData;

    /**
     * 验证状态枚举
     */
    public enum ValidationStatus {
        SUCCESS("验证成功"),
        FAILED("验证失败"),
        EXPIRED("授权已过期"),
        INVALID("授权无效"),
        SUSPENDED("授权暂停"),
        REVOKED("授权已撤销"),
        ERROR("系统错误");

        private final String description;

        ValidationStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}