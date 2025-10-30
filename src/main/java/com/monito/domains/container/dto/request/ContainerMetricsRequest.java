package com.monito.domains.container.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 컨테이너 메트릭 조회 요청 DTO
 * - Quick Range 또는 Custom Range로 시간 범위 지정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerMetricsRequest {

    /**
     * Quick Range 사용 시
     */
    private QuickRangeType quickRange;

    /**
     * Custom Range 사용 시 - 시작 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * Custom Range 사용 시 - 종료 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * Quick Range 우선 사용
     * - Quick Range가 설정되어 있으면 Custom Range 무시
     */
    public boolean isQuickRange() {
        return quickRange != null;
    }

    /**
     * 시작 시간 계산
     */
    public LocalDateTime getCalculatedStartTime() {
        if (isQuickRange()) {
            return LocalDateTime.now().minusMinutes(quickRange.getMinutes());
        }
        return startTime != null ? startTime : LocalDateTime.now().minusHours(1);
    }

    /**
     * 종료 시간 계산
     */
    public LocalDateTime getCalculatedEndTime() {
        if (isQuickRange()) {
            return LocalDateTime.now();
        }
        return endTime != null ? endTime : LocalDateTime.now();
    }

    public static ContainerMetricsRequest of(QuickRangeType quickRange,
                                             LocalDateTime startTime,
                                             LocalDateTime endTime){
        return ContainerMetricsRequest.builder()
                .quickRange(quickRange)
                .startTime(startTime)
                .endTime(endTime)
                .build();
    }
}