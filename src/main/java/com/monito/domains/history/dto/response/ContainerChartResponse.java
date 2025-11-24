package com.monito.domains.history.dto.response;

import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 컨테이너 차트 데이터 응답 DTO
 * - 특정 메트릭 필드의 시계열 데이터 목록
 */
@Schema(description = "컨테이너 차트 데이터 응답 - 시계열 데이터 목록")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerChartResponse {

    @Schema(description = "컨테이너 ID", example = "123")
    private Long containerId;

    @Schema(description = "메트릭 필드명", example = "cpuPercent")
    private String metricField;

    @Schema(description = "시계열 데이터 포인트 목록 (timestamp, value)")
    private List<TimeSeriesDataDTO> dataPoints;

    @Schema(description = "다운샘플링 전 원본 데이터 포인트 개수", example = "1000")
    private Integer originalCount;

    @Schema(description = "다운샘플링 후 반환된 데이터 포인트 개수", example = "60")
    private Integer sampledCount;
}