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

    @Schema(description = "STDOUT 로그 개수 (당일, loggedAt 기준)")
    private Long stdoutCount;

    @Schema(description = "STDERR 로그 개수 (당일, loggedAt 기준)")
    private Long stderrCount;

    @Schema(description = "STDOUT 로그 개수 (당일, createdAt 기준)")
    private Long stdoutCountByCreatedAt;

    @Schema(description = "STDERR 로그 개수 (당일, createdAt 기준)")
    private Long stderrCountByCreatedAt;

    public static DashboardLogsMetricsDTO of(Long stdoutCount, Long stderrCount,
                                              Long stdoutCountByCreatedAt, Long stderrCountByCreatedAt) {
        return DashboardLogsMetricsDTO.builder()
                .stdoutCount(stdoutCount)
                .stderrCount(stderrCount)
                .stdoutCountByCreatedAt(stdoutCountByCreatedAt)
                .stderrCountByCreatedAt(stderrCountByCreatedAt)
                .build();
    }
}