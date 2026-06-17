package com.enterprise.license.dto;

import com.enterprise.license.entity.Customer;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 客户数据传输对象
 */
@Data
@Schema(description = "客户信息DTO")
public class CustomerDto {

    @Schema(description = "客户ID", example = "1")
    private Long id;

    @Schema(description = "客户编码", example = "CUST001")
    private String customerCode;

    @NotBlank(message = "客户名称不能为空")
    @Size(max = 128, message = "客户名称长度不能超过128位")
    @Schema(description = "客户名称", example = "ABC科技有限公司", requiredMode = Schema.RequiredMode.REQUIRED)
    private String customerName;

    @NotBlank(message = "联系人不能为空")
    @Size(max = 64, message = "联系人长度不能超过64位")
    @Schema(description = "联系人", example = "张三", requiredMode = Schema.RequiredMode.REQUIRED)
    private String contactPerson;

    @Email(message = "邮箱格式不正确")
    @Size(max = 128, message = "邮箱长度不能超过128位")
    @Schema(description = "邮箱", example = "zhangsan@abc.com")
    private String email;

    @Size(max = 32, message = "电话长度不能超过32位")
    @Schema(description = "电话", example = "13800138000")
    private String phone;

    @Size(max = 256, message = "地址长度不能超过256位")
    @Schema(description = "地址", example = "北京市朝阳区XX路XX号")
    private String address;

    @Schema(description = "客户状态", example = "ACTIVE")
    private Customer.CustomerStatus status;

    @Schema(description = "客户类型", example = "ENTERPRISE")
    private Customer.CustomerType type;

    @Size(max = 500, message = "备注长度不能超过500位")
    @Schema(description = "备注")
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "最后登录时间")
    private LocalDateTime lastLoginTime;

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

    @Schema(description = "授权数量")
    private Long licenseCount;

    @Schema(description = "有效授权数量")
    private Long activeLicenseCount;
}