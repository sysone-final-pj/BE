package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 대시보드 CPU 메트릭 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 CPU 메트릭")
public class DashboardCpuMetricsDTO {

    @Schema(description = "CPU 사용률 (%)")
    private BigDecimal cpuPercent;

    @Schema(description = "CPU 사용량 (cores) = (cpuPercent / 100) * cpuLimitCores")
    private BigDecimal cpuUsage;

    @Schema(description = "CPU 제한 (cores)")
    private BigDecimal cpuLimitCores;

    public static DashboardCpuMetricsDTO from(Container container, ContainerStatsLog statsLog) {
        // CPU Usage 계산: (cpuPercent / 100) * cpuLimitCores
        BigDecimal cpuUsage = null;
        if (statsLog.getCpuPercent() != null && container.getCpuLimitCores() != null) {
            cpuUsage = statsLog.getCpuPercent()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(container.getCpuLimitCores())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        return DashboardCpuMetricsDTO.builder()
                .cpuPercent(statsLog.getCpuPercent())
                .cpuUsage(cpuUsage)
                .cpuLimitCores(container.getCpuLimitCores())
                .build();
    }
}