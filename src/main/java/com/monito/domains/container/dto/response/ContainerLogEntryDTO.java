package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 컨테이너 로그 엔트리 DTO
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
}