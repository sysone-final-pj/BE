/**
 * 컨테이너 로그 조회 요청 DTO (커서 기반 무한 스크롤)
 */
package com.monito.domains.container.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.domain.LogSortField;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerLogsRequest {

    /**
     * 커서: 마지막으로 받은 로그 ID
     * - 초기 로드 시 null
     * - 이후 요청 시 이전 응답의 lastLogId 사용
     */
    private Long lastLogId;

    /**
     * 커서: 마지막으로 받은 로그 시간
     * - 초기 로드 시 null
     * - 이후 요청 시 이전 응답의 lastLoggedAt 사용
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastLoggedAt;

    /**
     * 한번에 가져올 로그 개수 (기본: 50)
     */
    @Builder.Default
    private int size = 50;

    /**
     * Quick Range 사용 시 (초기 로드에만 적용)
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
     * Log Source 필터 (STDOUT, STDERR, RAW)
     */
    private LogSource logSource;

    /**
     * Agent Name 필터
     */
    private String agentName;

    /**
     * 정렬 필드 (기본: LOGGED_AT)
     */
    @Builder.Default
    private LogSortField sortBy = LogSortField.LOGGED_AT;

    /**
     * 정렬 방향 (기본: DESC)
     */
    @Builder.Default
    private Sort.Direction direction = Sort.Direction.DESC;

    /**
     * 초기 로드 여부 (커서가 없으면 초기 로드)
     */
    public boolean isInitialLoad() {
        return lastLogId == null && lastLoggedAt == null;
    }

    /**
     * Quick Range 우선 사용
     */
    public boolean isQuickRange() {
        return quickRange != null;
    }

    /**
     * 시작 시간 계산 (초기 로드에만 적용)
     */
    public LocalDateTime getCalculatedStartTime() {
        if (isQuickRange()) {
            return LocalDateTime.now().minusMinutes(quickRange.getMinutes());
        }
        return startTime != null ? startTime : LocalDateTime.now().minusHours(1);
    }

    /**
     * 종료 시간 계산 (초기 로드에만 적용)
     */
    public LocalDateTime getCalculatedEndTime() {
        if (isQuickRange()) {
            return LocalDateTime.now();
        }
        return endTime != null ? endTime : LocalDateTime.now();
    }

    public static ContainerLogsRequest of(Long lastLogId,
                                         LocalDateTime lastLoggedAt,
                                         int size,
                                         QuickRangeType quickRange,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime,
                                         LogSource logSource,
                                         String agentName,
                                         LogSortField sortBy,
                                         Sort.Direction direction) {
        return ContainerLogsRequest.builder()
                .lastLogId(lastLogId)
                .lastLoggedAt(lastLoggedAt)
                .size(size)
                .quickRange(quickRange)
                .startTime(startTime)
                .endTime(endTime)
                .logSource(logSource)
                .agentName(agentName)
                .sortBy(sortBy)
                .direction(direction)
                .build();
    }
}