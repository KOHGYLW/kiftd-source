package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户更新请求DTO
 */
@Data
@Schema(description = "客户更新请求")
public class CustomerUpdateRequest {

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 200, message = "客户名称长度不能超过200个字符")
    @Schema(description = "客户名称", example = "ABC科技有限公司", required = true)
    private String name;

    @Schema(description = "客户类型", example = "ENTERPRISE")
    private String type;

    @Schema(description = "客户状态", example = "ACTIVE")
    private String status;

    @Size(max = 200, message = "联系人长度不能超过200个字符")
    @Schema(description = "联系人", example = "张三")
    private String contactPerson;

    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    @Schema(description = "联系电话", example = "13800138000")
    private String contactPhone;

    @Email(message = "邮箱格式不正确")
    @Size(max = 200, message = "邮箱长度不能超过200个字符")
    @Schema(description = "邮箱", example = "contact@abc.com")
    private String email;

    @Size(max = 500, message = "地址长度不能超过500个字符")
    @Schema(description = "地址", example = "北京市朝阳区xxx街道xxx号")
    private String address;

    @Size(max = 100, message = "行业长度不能超过100个字符")
    @Schema(description = "行业", example = "软件开发")
    private String industry;

    @Size(max = 100, message = "注册号长度不能超过100个字符")
    @Schema(description = "注册号/统一社会信用代码", example = "91110108123456789X")
    private String registrationNumber;

    @Size(max = 100, message = "法定代表人长度不能超过100个字符")
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

    @Schema(description = "负责用户ID", example = "1")
    private Long assignedUserId;

}