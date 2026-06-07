package com.enterprise.license.dto.dashboard;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 趋势数据DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "趋势数据")
public class TrendData {

    @Schema(description = "数据类型", example = "LICENSE")
    private String dataType;

    @Schema(description = "开始日期")
    private LocalDate startDate;

    @Schema(description = "结束日期")
    private LocalDate endDate;

    @Schema(description = "趋势数据点")
    private List<TrendPoint> dataPoints;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "趋势数据点")
    public static class TrendPoint {
        @Schema(description = "日期")
        private LocalDate date;

        @Schema(description = "数据值", example = "25")
        private Long value;

        @Schema(description = "额外数据")
        private Map<String, Object> extraData;
    }

}