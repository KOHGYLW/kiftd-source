package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "客户响应")
public class CustomerResponse {

    @Schema(description = "客户ID", example = "1")
    private Long id;

    @Schema(description = "客户名称", example = "ABC科技有限公司")
    private String name;

    @Schema(description = "客户编码", example = "CUST001")
    private String code;

    @Schema(description = "客户类型", example = "ENTERPRISE")
    private String type;

    @Schema(description = "客户类型描述", example = "企业")
    private String typeDescription;

    @Schema(description = "客户状态", example = "ACTIVE")
    private String status;

    @Schema(description = "客户状态描述", example = "活跃")
    private String statusDescription;

    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Schema(description = "邮箱", example = "contact@abc.com")
    private String email;

    @Schema(description = "地址", example = "北京市朝阳区xxx街道xxx号")
    private String address;

    @Schema(description = "行业", example = "软件开发")
    private String industry;

    @Schema(description = "注册号/统一社会信用代码", example = "91110108123456789X")
    private String registrationNumber;

    @Schema(description = "法定代表人", example = "李四")
    private String legalRepresentative;

    @Schema(description = "业务范围")
    private String businessScope;

    @Schema(description = "合同开始日期")
    private LocalDateTime contractStartDate;

    @Schema(description = "合同结束日期")
    private LocalDateTime contractEndDate;

    @Schema(description = "合同金额", example = "100000.00")
    private BigDecimal contractAmount;

    @Schema(description = "备注")
    private String remarks;

    @Schema(description = "最后联系时间")
    private LocalDateTime lastContactTime;

    @Schema(description = "创建用户")
    private UserInfo createdByUser;

    @Schema(description = "负责用户")
    private UserInfo assignedUser;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

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