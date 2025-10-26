package com.enterprise.license.dto;

import com.enterprise.license.entity.License;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 授权许可数据传输对象
 */
@Data
@Schema(description = "授权许可信息DTO")
public class LicenseDto {

    @Schema(description = "授权ID", example = "1")
    private Long id;

    @Schema(description = "授权编码", example = "LIC2024001")
    private String licenseCode;

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long customerId;

    @Schema(description = "客户信息")
    private CustomerDto customer;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 128, message = "产品名称长度不能超过128位")
    @Schema(description = "产品名称", example = "企业版管理系统", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productName;

    @NotBlank(message = "产品版本不能为空")
    @Size(max = 32, message = "产品版本长度不能超过32位")
    @Schema(description = "产品版本", example = "v1.0.0", requiredMode = Schema.RequiredMode.REQUIRED)
    private String productVersion;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private License.LicenseType licenseType;

    @Schema(description = "授权状态", example = "ACTIVE")
    private License.LicenseStatus status;

    @NotNull(message = "开始时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime startTime;

    @NotNull(message = "到期时间不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "到期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Positive(message = "最大用户数必须为正数")
    @Schema(description = "最大用户数", example = "100")
    private Integer maxUsers;

    @Schema(description = "功能特性（JSON格式）")
    private String features;

    @Schema(description = "机器指纹")
    private String machineFingerprint;

    @Schema(description = "价格", example = "9999.99")
    private BigDecimal price;

    @Size(max = 500, message = "备注长度不能超过500位")
    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后验证时间")
    private LocalDateTime lastValidationTime;

    @Schema(description = "验证次数")
    private Long validationCount;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "创建时间")
    private LocalDateTime createdTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "更新时间")
    private LocalDateTime updatedTime;

    @Schema(description = "创建人")
    private String createdBy;

    @Schema(description = "更新人")
    private String updatedBy;

    @Schema(description = "是否即将过期")
    private Boolean isExpiring;

    @Schema(description = "剩余天数")
    private Long remainingDays;
}