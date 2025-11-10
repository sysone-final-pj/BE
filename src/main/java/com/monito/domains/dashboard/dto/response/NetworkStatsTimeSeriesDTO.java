package com.monito.domains.dashboard.dto.response;

import com.monito.domains.dashboard.dto.request.TimeRange;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "네트워크 통계 시계열 데이터")
public class NetworkStatsTimeSeriesDTO {

    @Schema(description = "컨테이너 ID")
    private Long containerId;

    @Schema(description = "컨테이너 이름")
    private String containerName;

    @Schema(description = "시간 범위")
    private TimeRange timeRange;

    @Schema(description = "데이터 포인트 목록")
    private List<NetworkStatsDataPointDTO> dataPoints;
}