package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 批量操作结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "批量操作结果")
public class BatchOperationResult {

    @Schema(description = "成功数量", example = "5")
    private Integer successCount;

    @Schema(description = "失败数量", example = "1")
    private Integer failureCount;

    @Schema(description = "操作详情")
    private String details;

}