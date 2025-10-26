package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 客户搜索请求DTO
 */
@Data
@Schema(description = "客户搜索请求")
public class CustomerSearchRequest {

    @Schema(description = "关键词（搜索客户名称、编码、联系人等）", example = "ABC")
    private String keyword;

    @Schema(description = "客户类型", example = "ENTERPRISE")
    private String type;

    @Schema(description = "客户状态", example = "ACTIVE")
    private String status;

    @Schema(description = "行业", example = "软件开发")
    private String industry;

    @Schema(description = "负责用户ID", example = "1")
    private Long assignedUserId;

    @Schema(description = "页码", example = "1")
    private Integer page = 1;

    @Schema(description = "每页大小", example = "20")
    private Integer size = 20;

    @Schema(description = "排序字段", example = "createdAt")
    private String sortField = "createdAt";

    @Schema(description = "排序方向", example = "desc")
    private String sortDirection = "desc";

}