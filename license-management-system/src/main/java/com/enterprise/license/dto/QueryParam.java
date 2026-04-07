package com.enterprise.license.dto;

import com.enterprise.license.entity.Customer;
import com.enterprise.license.entity.License;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查询参数基类
 */
@Data
@Schema(description = "查询参数")
public class QueryParam {

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer size = 10;

    @Schema(description = "排序字段", example = "createdTime")
    private String sortBy = "createdTime";

    @Schema(description = "排序方向", example = "desc")
    private String sortDir = "desc";

    @Schema(description = "关键词搜索")
    private String keyword;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "开始时间")
    private LocalDateTime startTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "结束时间")
    private LocalDateTime endTime;

    /**
     * 客户查询参数
     */
    @Data
    @Schema(description = "客户查询参数")
    public static class CustomerQueryParam extends QueryParam {

        @Schema(description = "客户编码")
        private String customerCode;

        @Schema(description = "客户名称")
        private String customerName;

        @Schema(description = "联系人")
        private String contactPerson;

        @Schema(description = "邮箱")
        private String email;

        @Schema(description = "电话")
        private String phone;

        @Schema(description = "客户状态")
        private Customer.CustomerStatus status;

        @Schema(description = "客户类型")
        private Customer.CustomerType type;

        @Schema(description = "地址")
        private String address;
    }

    /**
     * 授权查询参数
     */
    @Data
    @Schema(description = "授权查询参数")
    public static class LicenseQueryParam extends QueryParam {

        @Schema(description = "授权编码")
        private String licenseCode;

        @Schema(description = "客户ID")
        private Long customerId;

        @Schema(description = "产品名称")
        private String productName;

        @Schema(description = "产品版本")
        private String productVersion;

        @Schema(description = "授权类型")
        private License.LicenseType licenseType;

        @Schema(description = "授权状态")
        private License.LicenseStatus status;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "到期开始时间")
        private LocalDateTime expireStartTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "到期结束时间")
        private LocalDateTime expireEndTime;

        @Schema(description = "是否即将过期")
        private Boolean isExpiring;

        @Schema(description = "剩余天数小于")
        private Integer remainingDaysLessThan;
    }

    /**
     * 验证日志查询参数
     */
    @Data
    @Schema(description = "验证日志查询参数")
    public static class ValidationLogQueryParam extends QueryParam {

        @Schema(description = "授权ID")
        private Long licenseId;

        @Schema(description = "客户端IP")
        private String clientIp;

        @Schema(description = "验证状态")
        private String validationStatus;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "验证开始时间")
        private LocalDateTime validationStartTime;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        @Schema(description = "验证结束时间")
        private LocalDateTime validationEndTime;
    }

    /**
     * 导出参数
     */
    @Data
    @Schema(description = "导出参数")
    public static class ExportParam {

        @Schema(description = "导出类型", example = "EXCEL", allowableValues = {"EXCEL", "CSV"})
        private String exportType = "EXCEL";

        @Schema(description = "导出字段")
        private String[] fields;

        @Schema(description = "文件名")
        private String fileName;

        @Schema(description = "查询条件")
        private Object queryParam;
    }
}