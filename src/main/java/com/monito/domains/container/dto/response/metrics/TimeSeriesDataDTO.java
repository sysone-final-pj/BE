package com.monito.domains.container.dto.response.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 시계열 데이터 포인트
 * - 차트 표시용 데이터
 */
@Getter
@Builder
@AllArgsConstructor
public class TimeSeriesDataDTO {
    private LocalDateTime timestamp;
    private BigDecimal value;

    public static TimeSeriesDataDTO from(
        LocalDateTime timeStamp,
        BigDecimal value
    ){
        return TimeSeriesDataDTO.builder()
                .timestamp(timeStamp)
                .value(value)
                .build();
    }
}