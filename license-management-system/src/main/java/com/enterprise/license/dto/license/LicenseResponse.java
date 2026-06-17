package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 授权响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授权响应")
public class LicenseResponse {

    @Schema(description = "授权ID", example = "1")
    private Long id;

    @Schema(description = "授权码", example = "LIC-ABC123-DEF456-GHI789")
    private String licenseKey;

    @Schema(description = "客户信息")
    private CustomerInfo customer;

    @Schema(description = "产品名称", example = "企业管理系统 v1.0")
    private String productName;

    @Schema(description = "产品版本", example = "1.0.0")
    private String productVersion;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private String licenseType;

    @Schema(description = "授权类型描述", example = "商业")
    private String licenseTypeDescription;

    @Schema(description = "授权状态", example = "ACTIVE")
    private String status;

    @Schema(description = "授权状态描述", example = "激活")
    private String statusDescription;

    @Schema(description = "最大用户数", example = "100")
    private Integer maxUsers;

    @Schema(description = "最大设备数", example = "50")
    private Integer maxDevices;

    @Schema(description = "授权开始日期")
    private LocalDateTime startDate;

    @Schema(description = "授权结束日期")
    private LocalDateTime endDate;

    @Schema(description = "授权功能列表")
    private List<String> features;

    @Schema(description = "硬件指纹")
    private String hardwareId;

    @Schema(description = "MAC地址列表")
    private List<String> macAddresses;

    @Schema(description = "IP限制")
    private List<String> ipRestrictions;

    @Schema(description = "域名限制")
    private List<String> domainRestrictions;

    @Schema(description = "激活次数", example = "0")
    private Integer activationCount;

    @Schema(description = "最大激活次数", example = "3")
    private Integer maxActivationCount;

    @Schema(description = "最后激活时间")
    private LocalDateTime lastActivationTime;

    @Schema(description = "最后心跳时间")
    private LocalDateTime lastHeartbeatTime;

    @Schema(description = "撤销时间")
    private LocalDateTime revokedAt;

    @Schema(description = "撤销原因")
    private String revokeReason;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "创建用户")
    private UserInfo createdByUser;

    @Schema(description = "审批用户")
    private UserInfo approvedByUser;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "是否有效")
    private Boolean isValid;

    @Schema(description = "是否过期")
    private Boolean isExpired;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "客户信息")
    public static class CustomerInfo {
        @Schema(description = "客户ID", example = "1")
        private Long id;

        @Schema(description = "客户名称", example = "ABC科技有限公司")
        private String name;

        @Schema(description = "客户编码", example = "CUST001")
        private String code;
    }

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
    }

}