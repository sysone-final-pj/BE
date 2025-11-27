/**
 * CPU 메트릭 데이터
 */
package com.monito.domains.container.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
/**
 작성자: 백승준
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

    /**
     * 실시간 WebSocket 발행용 CPU 메트릭 생성 (단일 데이터 포인트)
     * @param container 컨테이너
     * @param statsLog 통계 로그
     * @param timestamp 타임스탬프
     * @return CPU 메트릭 DTO
     */
    public static CpuMetricsDTO forRealtimeUpdate(Container container, ContainerStatsLog statsLog, LocalDateTime timestamp) {
        BigDecimal throttleRate = calculateThrottleRate(statsLog);

        return CpuMetricsDTO.builder()
                .cpuPercent(List.of(TimeSeriesDataDTO.from(timestamp, statsLog.getCpuPercent())))
                .cpuCoreUsage(List.of(TimeSeriesDataDTO.from(timestamp, statsLog.getCpuCoreUsage())))
                .currentCpuPercent(statsLog.getCpuPercent())
                .currentCpuCoreUsage(statsLog.getCpuCoreUsage())
                .hostCpuUsageTotal(statsLog.getHostCpuUsageTotal())
                .cpuUsageTotal(statsLog.getCpuUsageTotal())
                .cpuUser(statsLog.getCpuUser())
                .cpuSystem(statsLog.getCpuSystem())
                .cpuQuota(statsLog.getCpuQuota())
                .cpuPeriod(statsLog.getCpuPeriod())
                .onlineCpus(statsLog.getOnlineCpus())
                .cpuLimitCores(container.getCpuLimitCores())
                .throttlingPeriods(statsLog.getThrottlingPeriods())
                .throttledPeriods(statsLog.getThrottledPeriods())
                .throttledTime(statsLog.getThrottledTime())
                .throttleRate(throttleRate)
                .summary(null)  // 실시간 업데이트에서는 summary 불필요
                .build();
    }

    /**
     * Throttle Rate 계산
     */
    private static BigDecimal calculateThrottleRate(ContainerStatsLog statsLog) {
        if (statsLog.getThrottlingPeriods() != null && statsLog.getThrottlingPeriods() > 0) {
            return BigDecimal.valueOf(statsLog.getThrottledPeriods())
                    .divide(BigDecimal.valueOf(statsLog.getThrottlingPeriods()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }
        return null;
    }
}