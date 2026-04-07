package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 系统告警DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "系统告警")
public class SystemAlert {

    @Schema(description = "告警ID", example = "1")
    private Long id;

    @Schema(description = "告警类型", example = "LICENSE_EXPIRING")
    private String alertType;

    @Schema(description = "告警级别", example = "WARNING")
    private String severity;

    @Schema(description = "告警标题", example = "授权即将过期")
    private String title;

    @Schema(description = "告警消息", example = "有15个授权将在30天内过期")
    private String message;

    @Schema(description = "告警时间")
    private LocalDateTime alertTime;

    @Schema(description = "是否已读", example = "false")
    private Boolean isRead;

    @Schema(description = "相关对象ID", example = "123")
    private String relatedObjectId;

    @Schema(description = "相关对象类型", example = "LICENSE")
    private String relatedObjectType;

    @Schema(description = "处理建议")
    private String suggestion;

}