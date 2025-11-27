/**
 * OOM 메트릭 DTO
 * - 시간대별 OOM 발생 횟수 (HOURS 고정)
 * - Bar 차트용: X축=시간대(00~23), Y축=OOM 횟수
 * - Hover 시 세부 정보 표시용
 */
package com.monito.domains.container.dto.response.metrics;

import com.monito.domains.container.domain.Container;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class OomMetricsDTO {

    /**
     * 시간대별 OOM 발생 횟수
     * Key: 시간대 (HOURS 단위로 절삭, 예: "2025-01-20T10:00:00")
     * Value: 해당 시간대 OOM 발생 횟수
     */
    private Map<LocalDateTime, Long> timeSeries;

    /**
     * 컨테이너 전체 누적 OOM 횟수 (생성 이후 전체 기간)
     */
    private Integer totalOomKills;

    /**
     * 마지막 OOM 발생 시각
     */
    private LocalDateTime lastOomKilledAt;

    /**
     * 실시간 WebSocket 발행용 OOM 메트릭 생성
     * - timeSeries는 빈 맵 (실시간에서는 불필요)
     * @param container 컨테이너
     * @return OOM 메트릭 DTO
     */
    public static OomMetricsDTO forRealtimeUpdate(Container container) {
        return OomMetricsDTO.builder()
                .timeSeries(Map.of())  // 실시간에서는 빈 맵
                .totalOomKills(container.getOomKills())
                .lastOomKilledAt(container.getLastOomKilledAt())
                .build();
    }
}