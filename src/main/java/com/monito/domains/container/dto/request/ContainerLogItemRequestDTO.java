/**
 * Agent로부터 받는 개별 로그 항목 DTO
 */
package com.monito.domains.container.dto.request;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Slf4j
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerLogItemRequestDTO {

    /**
     * 로그 메시지 내용
     */
    private String message;

    /**
     * 로그 소스 (stdout, stderr 등)
     */
    private LogSource source;

    /**
     * 로그 발생 시각
     */
    private LocalDateTime timestamp;

    public ContainerLog toEntity(Container container) {
        // 파티션 범위 유효성 검증 (2025-01-01 ~ 현재 + 1일)
        LocalDateTime validTimestamp = validateAndFixTimestamp(timestamp);

        return ContainerLog.builder()
                .container(container)
                .logMessage(message)
                .source(source)
                .loggedAt(validTimestamp)
                .build();
    }

    /**
     * 타임스탬프 유효성 검증 및 보정
     * 파티션 범위를 벗어나는 경우 현재 시간으로 대체
     */
    private LocalDateTime validateAndFixTimestamp(LocalDateTime timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now();
        }

        // 파티션 범위: 2025-01-01 이후
        LocalDateTime minDate = LocalDateTime.of(2025, 1, 1, 0, 0);
        // 최대: 현재 + 1일 (미래 로그 방지)
        LocalDateTime maxDate = LocalDateTime.now().plusDays(1);

        if (timestamp.isBefore(minDate) || timestamp.isAfter(maxDate)) {
            // 범위를 벗어나면 현재 시간 사용
            log.warn("타임스탬프가 파티션 범위를 벗어남 - 원본: {}, 허용 범위: {} ~ {}, 현재 시간으로 대체",
                    timestamp, minDate, maxDate);
            return LocalDateTime.now();
        }

        return timestamp;
    }
}