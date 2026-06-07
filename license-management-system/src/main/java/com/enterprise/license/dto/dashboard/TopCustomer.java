package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 重要客户DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "重要客户")
public class TopCustomer {

    @Schema(description = "客户ID", example = "1")
    private Long id;

    @Schema(description = "客户名称", example = "ABC科技有限公司")
    private String name;

    @Schema(description = "客户编码", example = "CUST001")
    private String code;

    @Schema(description = "授权数量", example = "15")
    private Long licenseCount;

    @Schema(description = "活跃授权数", example = "12")
    private Long activeLicenseCount;

    @Schema(description = "合同金额", example = "500000.00")
    private BigDecimal contractAmount;

    @Schema(description = "客户类型", example = "ENTERPRISE")
    private String customerType;

    @Schema(description = "行业", example = "软件开发")
    private String industry;

    @Schema(description = "负责人", example = "张三")
    private String assignedUser;

}