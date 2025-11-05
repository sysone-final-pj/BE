package com.monito.domains.container.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * 실시간 WebSocket 발행용 Memory 메트릭 생성 (단일 데이터 포인트)
     * @param container 컨테이너
     * @param statsLog 통계 로그
     * @param timestamp 타임스탬프
     * @return Memory 메트릭 DTO
     */
    public static MemoryMetricsDTO forRealtimeUpdate(Container container, ContainerStatsLog statsLog, LocalDateTime timestamp) {
        return MemoryMetricsDTO.builder()
                .memoryUsage(List.of(TimeSeriesDataDTO.from(timestamp, BigDecimal.valueOf(statsLog.getMemUsage()))))
                .memoryPercent(List.of(TimeSeriesDataDTO.from(timestamp, statsLog.getMemPercent())))
                .currentMemoryUsage(statsLog.getMemUsage())
                .currentMemoryPercent(statsLog.getMemPercent())
                .memLimit(container.getMemLimit())
                .memMaxUsage(statsLog.getMemMaxUsage())
                .oomKills(container.getOomKills())
                .build();
    }
}