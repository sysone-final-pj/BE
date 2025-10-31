package com.monito.domains.container.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerSummaryResponseDTO {
    private String agentName;
    private String containerHash;
    private String containerName;
    private BigDecimal cpuPercent;
    private Long memUsage;
    private Long memLimit;
    private Long rxBytesPerSec;
    private Long txBytesPerSec;
    private ContainerState state;
    private ContainerHealth health;
    private Long imageSize;
    private Long sizeRootFs;
    ContainerState containerState;

    public static ContainerSummaryResponseDTO of(Agent agent, Container container, ContainerStatsLog containerStatsLog) {
        ContainerState resolvedState = containerStatsLog.getState();
        LocalDateTime collectedAt = containerStatsLog.getCollectedAt();
        if (collectedAt != null && Duration.between(collectedAt, LocalDateTime.now()).getSeconds() > 30) {
            resolvedState = ContainerState.UNKNOWN;
        }

        return ContainerSummaryResponseDTO.builder()
                .agentName(agent.getAgentName())
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .cpuPercent(containerStatsLog.getCpuPercent())
                .memUsage(containerStatsLog.getMemUsage())
                .memLimit(container.getMemLimit())
                .rxBytesPerSec(containerStatsLog.getRxBytesPerSec())
                .txBytesPerSec(containerStatsLog.getTxBytesPerSec())
                .imageSize(container.getImageSize())
                .state(resolvedState)
                .health(containerStatsLog.getHealth())
                .sizeRootFs(containerStatsLog.getSizeRootFs())
                .containerState(resolvedState)
                .build();
    }
}
