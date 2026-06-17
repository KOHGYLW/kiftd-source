package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 授权搜索请求DTO
 */
@Data
@Schema(description = "授权搜索请求")
public class LicenseSearchRequest {

    @Schema(description = "关键词（搜索授权码、产品名称等）", example = "ABC")
    private String keyword;

    @Schema(description = "客户ID", example = "1")
    private Long customerId;

    @Schema(description = "授权类型", example = "COMMERCIAL")
    private String licenseType;

    @Schema(description = "授权状态", example = "ACTIVE")
    private String status;

    @Schema(description = "产品名称", example = "企业管理系统")
    private String productName;

    @Schema(description = "是否即将过期（30天内）", example = "true")
    private Boolean expiringSoon;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortField = "createdAt";

    @Schema(description = "排序方向", example = "desc")
    private String sortDirection = "desc";

}