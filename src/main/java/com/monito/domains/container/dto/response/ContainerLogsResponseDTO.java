/**
 * 컨테이너 로그 목록 응답 DTO (커서 기반)
 */
package com.monito.domains.container.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerLogsResponseDTO {
    /**
     * 로그 목록
     */
    private List<ContainerLogEntryDTO> logs;

    /**
     * 다음 요청에 사용할 커서 - 로그 ID
     */
    private Long lastLogId;

    /**
     * 다음 요청에 사용할 커서 - 로그 시간
     */
    private LocalDateTime lastLoggedAt;

    /**
     * 더 가져올 데이터가 있는지
     */
    private boolean hasMore;

    /**
     * 이번에 반환된 로그 개수
     */
    private int returnedCount;

    /**
     * 요청한 사이즈
     */
    private int requestedSize;
}