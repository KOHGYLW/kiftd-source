package com.enterprise.license.dto.customer;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 客户批量操作请求DTO
 */
@Data
@Schema(description = "客户批量操作请求")
public class CustomerBatchRequest {

    @NotEmpty(message = "客户ID列表不能为空")
    @Schema(description = "客户ID列表", example = "[1, 2, 3]", required = true)
    private List<Long> customerIds;

    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型", example = "ACTIVATE", 
            allowableValues = {"ACTIVATE", "DEACTIVATE", "SUSPEND", "DELETE"}, required = true)
    private BatchOperation operation;

    @Schema(description = "操作原因", example = "批量激活客户")
    private String reason;

    public enum BatchOperation {
        ACTIVATE("激活"),
        DEACTIVATE("停用"),
        SUSPEND("暂停"),
        DELETE("删除");

        private final String description;

        BatchOperation(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

}