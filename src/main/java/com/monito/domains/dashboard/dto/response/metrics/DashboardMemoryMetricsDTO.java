/**
 * 대시보드 메모리 메트릭 DTO
 */
package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.Container;
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
@Schema(description = "대시보드 메모리 메트릭")
public class DashboardMemoryMetricsDTO {

    @Schema(description = "메모리 사용량 (bytes)")
    private Long memUsage;

    @Schema(description = "메모리 제한 (bytes), isMemoryUnlimited=true면 null")
    private Long memLimit;

    public static DashboardMemoryMetricsDTO from(Container container, ContainerStatsLog statsLog) {
        // Memory Limit: isMemoryUnlimited=true면 null 반환
        Long memLimit = container.getIsMemoryUnlimited() ? null : container.getMemLimit();

        return DashboardMemoryMetricsDTO.builder()
                .memUsage(statsLog.getMemUsage())
                .memLimit(memLimit)
                .build();
    }
}