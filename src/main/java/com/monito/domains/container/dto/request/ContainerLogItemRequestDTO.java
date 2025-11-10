package com.monito.domains.container.dto.request;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Agent로부터 받는 개별 로그 항목 DTO
 */
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
        return ContainerLog.builder()
                .container(container)
                .logMessage(message)
                .source(source)
                .loggedAt(timestamp)
                .build();
    }
}