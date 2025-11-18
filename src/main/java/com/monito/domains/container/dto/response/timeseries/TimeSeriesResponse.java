package com.monito.domains.container.dto.response.timeseries;

import com.monito.domains.container.dto.response.metrics.TimeSeriesDataDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 시계열 데이터 응답 (메타데이터 포함)
 */
@Getter
@Builder
@AllArgsConstructor
public class TimeSeriesResponse {
    /**
     * 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 종료 시간
     */
    private LocalDateTime endTime;

    /**
     * 반환된 데이터 포인트 수
     */
    private int dataPoints;

    /**
     * 시계열 데이터
     */
    private List<TimeSeriesDataDTO> data;

    public static TimeSeriesResponse of(
            LocalDateTime startTime,
            LocalDateTime endTime,
            List<TimeSeriesDataDTO> data
    ) {
        return TimeSeriesResponse.builder()
                .startTime(startTime)
                .endTime(endTime)
                .dataPoints(data.size())
                .data(data)
                .build();
    }
}