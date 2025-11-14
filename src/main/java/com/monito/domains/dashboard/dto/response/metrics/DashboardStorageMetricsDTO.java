package com.monito.domains.dashboard.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대시보드 스토리지 메트릭 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 스토리지 메트릭")
public class DashboardStorageMetricsDTO {

    @Schema(description = "스토리지 할당량 (bytes), 0이면 무제한")
    private Long storageLimit;

    @Schema(description = "현재 스토리지 사용량 (bytes)")
    private Long storageUsed;

    public static DashboardStorageMetricsDTO of(Container container, Long storageUsed) {
        return DashboardStorageMetricsDTO.builder()
                .storageLimit(container.getStorageLimit())
                .storageUsed(storageUsed)
                .build();
    }
}