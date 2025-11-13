package com.monito.domains.dashboard.dto.response.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 로그 메트릭 DTO (당일 0시 기준 집계)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 로그 메트릭")
public class DashboardLogsMetricsDTO {

    @Schema(description = "STDOUT 로그 개수 (당일)")
    private Long stdoutCount;

    @Schema(description = "STDERR 로그 개수 (당일)")
    private Long stderrCount;

    public static DashboardLogsMetricsDTO of(Long stdoutCount, Long stderrCount) {
        return DashboardLogsMetricsDTO.builder()
                .stdoutCount(stdoutCount)
                .stderrCount(stderrCount)
                .build();
    }
}