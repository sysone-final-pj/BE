/**
 * OOM 시계열 데이터 응답 (HTTP 초기 로드용)
 * - Histogram/Heatmap 차트 렌더링용
 * - WebSocket 실시간 업데이트와 함께 사용
 */
package com.monito.domains.container.dto.response;

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
public class OomTimeSeriesResponseDTO {

    /**
     * 컨테이너 ID
     */
    private Long containerId;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * 조회 시작 시간
     */
    private LocalDateTime startTime;

    /**
     * 조회 종료 시간
     */
    private LocalDateTime endTime;

    /**
     * 버킷 크기 (HOURS, DAYS 등)
     */
    private String bucketSize;

    /**
     * 시간대별 OOM 발생 횟수
     * Key: 시간대 (버킷 단위로 절삭됨, 예: "2025-01-20T10:00:00")
     * Value: 해당 시간대 OOM 발생 횟수
     */
    private Map<LocalDateTime, Long> timeSeries;

    /**
     * 조회 기간 내 총 OOM 발생 횟수
     */
    private int totalCount;

    /**
     * 컨테이너 전체 누적 OOM 횟수 (생성 이후 전체 기간)
     */
    private Integer totalOomKills;

    /**
     * 마지막 OOM 발생 시각
     */
    private LocalDateTime lastOomKilledAt;
}