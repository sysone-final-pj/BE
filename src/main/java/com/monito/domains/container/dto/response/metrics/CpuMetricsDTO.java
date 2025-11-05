package com.monito.domains.container.dto.response.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * CPU 메트릭 데이터
 */
@Getter
@Builder
@AllArgsConstructor
public class CpuMetricsDTO {
    // 시계열 데이터 (차트용)
    private List<TimeSeriesDataDTO> cpuPercent;           // CPU 사용률 (%)
    private List<TimeSeriesDataDTO> cpuCoreUsage;         // CPU 코어 사용량

    // 현재 값
    private BigDecimal currentCpuPercent;                 // 현재 CPU 사용률
    private BigDecimal currentCpuCoreUsage;               // 현재 CPU 코어 사용량

    // CPU 상세 정보
    private Long hostCpuUsageTotal;                       // 호스트 전체 CPU 사용량
    private Long cpuUsageTotal;                           // 컨테이너 CPU 사용량 (nanoseconds)
    private Long cpuUser;                                 // User mode CPU 사용량
    private Long cpuSystem;                               // System mode CPU 사용량

    // CPU 제한 정보
    private Long cpuQuota;                                // CPU quota (microseconds per period)
    private Long cpuPeriod;                               // CPU period (microseconds)
    private Integer onlineCpus;                           // 온라인 CPU 코어 수
    private BigDecimal cpuLimitCores;                     // CPU 제한 (코어 단위)

    // Throttling 정보
    private Long throttlingPeriods;                       // 전체 스케줄링 기간 수
    private Long throttledPeriods;                        // Throttle된 기간 수
    private Long throttledTime;                           // Throttle된 총 시간 (nanoseconds)
    private BigDecimal throttleRate;                      // Throttle 비율 (%)

    // cpu 요약 정보 (1분, 5분, 15분, p95)
    private CpuMetricsSummaryDTO summary;
}