package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.ContainerStatsLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 Block I/O 메트릭 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 Block I/O 메트릭")
public class DashboardBlockIOMetricsDTO {

    @Schema(description = "블록 읽기 (누적, bytes)")
    private Long blkRead;

    @Schema(description = "블록 쓰기 (누적, bytes)")
    private Long blkWrite;

    public static DashboardBlockIOMetricsDTO from(ContainerStatsLog statsLog) {
        return DashboardBlockIOMetricsDTO.builder()
                .blkRead(statsLog.getBlkRead())
                .blkWrite(statsLog.getBlkWrite())
                .build();
    }
}