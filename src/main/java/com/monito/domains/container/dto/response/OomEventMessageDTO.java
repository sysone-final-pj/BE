/**
 * OOM 이벤트 WebSocket 메시지
 * - 실시간 OOM 발생 시 클라이언트에게 전송
 * - 클라이언트는 이 데이터로 차트를 증분 업데이트
 */
package com.monito.domains.container.dto.response;

import com.monito.global.cache.OomEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class OomEventMessageDTO {

    /**
     * 컨테이너 ID
     */
    private Long containerId;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * OOM 실제 발생 시각
     */
    private LocalDateTime occurredAt;

    /**
     * 버킷 시간 (차트에서 어느 시간대에 추가할지)
     * - bucketSize=HOURS → "2025-01-21T15:00:00"
     * - bucketSize=DAYS → "2025-01-21T00:00:00"
     */
    private LocalDateTime bucket;

    /**
     * 업데이트된 전체 누적 OOM 횟수
     */
    private Integer totalOomKills;

    /**
     * Agent 키
     */
    private String agentKey;

    /**
     * OomEvent와 버킷 크기로부터 메시지 생성
     */
    public static OomEventMessageDTO from(
            OomEvent event,
            Integer totalOomKills,
            ChronoUnit bucketSize
    ) {
        LocalDateTime bucket = truncateTime(event.getOccurredAt(), bucketSize);

        return OomEventMessageDTO.builder()
                .containerId(event.getContainerId())
                .containerName(event.getContainerName())
                .occurredAt(event.getOccurredAt())
                .bucket(bucket)
                .totalOomKills(totalOomKills)
                .agentKey(event.getAgentKey())
                .build();
    }

    /**
     * 시간 절삭 (버킷 크기에 맞춰)
     */
    private static LocalDateTime truncateTime(LocalDateTime time, ChronoUnit unit) {
        return switch (unit) {
            case HOURS -> time.truncatedTo(ChronoUnit.HOURS);
            case DAYS -> time.truncatedTo(ChronoUnit.DAYS);
            case MINUTES -> time.truncatedTo(ChronoUnit.MINUTES);
            default -> time;
        };
    }
}