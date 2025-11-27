/**
 * 대시보드 네트워크 메트릭 DTO
 */
package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.ContainerStatsLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 네트워크 메트릭")
public class DashboardNetworkMetricsDTO {

    @Schema(description = "네트워크 송신 속도 (bytes/sec)")
    private Long txBytesPerSec;

    @Schema(description = "네트워크 수신 속도 (bytes/sec)")
    private Long rxBytesPerSec;

    public static DashboardNetworkMetricsDTO from(ContainerStatsLog statsLog) {
        return DashboardNetworkMetricsDTO.builder()
                .txBytesPerSec(statsLog.getTxBytesPerSec())
                .rxBytesPerSec(statsLog.getRxBytesPerSec())
                .build();
    }
}