package com.monito.domains.container.dto.response.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Memory 메트릭 데이터
 */
@Getter
@Builder
@AllArgsConstructor
public class MemoryMetricsDTO {
    // 시계열 데이터 (차트용)
    private List<TimeSeriesDataDTO> memoryUsage;          // 메모리 사용량 추이
    private List<TimeSeriesDataDTO> memoryPercent;        // 메모리 사용률 추이

    // 현재 값
    private Long currentMemoryUsage;                      // 현재 메모리 사용량 (bytes)
    private BigDecimal currentMemoryPercent;              // 현재 메모리 사용률 (%)

    // 메모리 제한 정보
    private Long memLimit;                                // 메모리 제한 (bytes)
    private Long memMaxUsage;                             // 최대 메모리 사용량 (bytes)

    // OOM Kill 정보
    private Integer oomKills;                             // OOM Kill 발생 횟수
}