package com.monito.domains.dashboard.dto.response;

import com.monito.domains.dashboard.dto.request.TimeRange;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Block I/O 통계 시계열 데이터 DTO
 * 특정 시간 범위 동안의 읽기/쓰기 속도 변화를 나타냄
 */
@Getter
@Builder
@AllArgsConstructor
public class BlockIOStatsTimeSeriesDTO {
    /**
     * 컨테이너 ID
     */
    private Long containerId;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * 조회한 시간 범위
     */
    private TimeRange timeRange;

    /**
     * 실제 조회된 데이터 포인트 개수
     */
    private Integer dataPointCount;

    /**
     * 시계열 데이터 포인트 리스트
     * 시간순으로 정렬됨 (과거 -> 현재)
     */
    private List<BlockIOStatsDataPointDTO> dataPoints;
}