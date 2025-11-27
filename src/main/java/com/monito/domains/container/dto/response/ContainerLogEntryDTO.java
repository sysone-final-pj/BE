/**
 * 컨테이너 로그 엔트리 DTO
 */
package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.dto.projection.ContainerLogProjection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerLogEntryDTO {
    private Long id;
    private String containerHash;
    private String containerName;
    private String agentName;
    private String logMessage;
    private LogSource source;
    private LocalDateTime loggedAt;

    public static ContainerLogEntryDTO from(ContainerLog log) {
        return ContainerLogEntryDTO.builder()
                .id(log.getId())
                .containerHash(log.getContainer().getContainerHash())
                .containerName(log.getContainer().getName())
                .agentName(log.getContainer().getAgent().getAgentName())
                .logMessage(log.getLogMessage())
                .source(log.getSource())
                .loggedAt(log.getLoggedAt())
                .build();
    }

    /**
     * Projection에서 DTO 생성 (성능 최적화)
     * - CLOB 전체 대신 미리보기(500자)만 사용
     */
    public static ContainerLogEntryDTO from(ContainerLogProjection projection) {
        return ContainerLogEntryDTO.builder()
                .id(projection.getId())
                .containerHash(projection.getContainerHash())
                .containerName(projection.getContainerName())
                .agentName(projection.getAgentName())
                .logMessage(projection.getLogMessagePreview())
                .source(projection.getSource())
                .loggedAt(projection.getLoggedAt())
                .build();
    }
}