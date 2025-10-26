package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 活动记录DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "活动记录")
public class ActivityRecord {

    @Schema(description = "活动ID", example = "1")
    private Long id;

    @Schema(description = "活动类型", example = "LICENSE_CREATED")
    private String activityType;

    @Schema(description = "活动描述", example = "创建了新的授权")
    private String description;

    @Schema(description = "操作用户", example = "admin")
    private String username;

    @Schema(description = "目标对象ID", example = "123")
    private String targetId;

    @Schema(description = "目标对象类型", example = "LICENSE")
    private String targetType;

    @Schema(description = "活动时间")
    private LocalDateTime activityTime;

    @Schema(description = "IP地址", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "优先级", example = "NORMAL")
    private String priority;

}