package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权创建请求DTO
 */
@Data
@Schema(description = "授权创建请求")
public class LicenseCreateRequest {

    @NotNull(message = "客户ID不能为空")
    @Schema(description = "客户ID", example = "1", required = true)
    private Long customerId;

    @NotBlank(message = "产品名称不能为空")
    @Size(max = 200, message = "产品名称长度不能超过200个字符")
    @Schema(description = "产品名称", example = "企业管理系统 v1.0", required = true)
    private String productName;

    @NotBlank(message = "产品版本不能为空")
    @Size(max = 50, message = "产品版本长度不能超过50个字符")
    @Schema(description = "产品版本", example = "1.0.0", required = true)
    private String productVersion;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private String licenseType = "TRIAL";

    @Schema(description = "最大用户数", example = "100")
    private Integer maxUsers;

    @Schema(description = "最大设备数", example = "50")
    private Integer maxDevices;

    @NotNull(message = "授权开始日期不能为空")
    @Schema(description = "授权开始日期", required = true)
    private LocalDateTime startDate;

    @NotNull(message = "授权结束日期不能为空")
    @Schema(description = "授权结束日期", required = true)
    private LocalDateTime endDate;

    @Schema(description = "授权功能列表", example = "[\"user_management\", \"report_generation\"]")
    private List<String> features;

    @Schema(description = "硬件指纹")
    private String hardwareId;

    @Schema(description = "MAC地址列表", example = "[\"00:11:22:33:44:55\", \"66:77:88:99:AA:BB\"]")
    private List<String> macAddresses;

    @Schema(description = "IP限制", example = "[\"192.168.1.0/24\", \"10.0.0.1\"]")
    private List<String> ipRestrictions;

    @Schema(description = "域名限制", example = "[\"example.com\", \"*.example.com\"]")
    private List<String> domainRestrictions;

    @Schema(description = "最大激活次数", example = "3")
    private Integer maxActivationCount;

    @Schema(description = "备注")
    private String remarks;

}