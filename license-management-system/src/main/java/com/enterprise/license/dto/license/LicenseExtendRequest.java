package com.enterprise.license.dto.license;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 授权延期请求DTO
 */
@Data
@Schema(description = "授权延期请求")
public class LicenseExtendRequest {

    @NotNull(message = "新的结束日期不能为空")
    @Schema(description = "新的结束日期", required = true)
    private LocalDateTime newEndDate;

    @Schema(description = "延期原因")
    private String reason;

}