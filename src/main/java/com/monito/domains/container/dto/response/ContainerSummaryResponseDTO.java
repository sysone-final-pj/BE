package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerSummaryResponseDTO {
    private Long id;
    private String agentName;
    private String containerHash;
    private String containerName;
    private BigDecimal cpuPercent;
    private Boolean isCpuUnlimited;
    private Long memUsage;
    private Long memLimit;
    private Boolean isMemoryUnlimited;
    private BigDecimal memPercent;
    private Long rxBytesPerSec;
    private Long txBytesPerSec;
    private ContainerState state;
    private ContainerHealth health;
    private Long imageSize;
    private Long sizeRootFs;
    private Long storageLimit;  // 0이면 무제한 (Agent 전체 디스크 용량)
    private Boolean isStorageUnlimited;
    private Boolean isFavorite;

    public static ContainerSummaryResponseDTO from(ContainerSummarySnapshot snapshot, boolean isFavorite) {
        return ContainerSummaryResponseDTO.builder()
                .id(snapshot.getId())
                .agentName(snapshot.getAgentName())
                .containerHash(snapshot.getContainerHash())
                .containerName(snapshot.getContainerName())
                .cpuPercent(snapshot.getCpuPercent())
                .isCpuUnlimited(snapshot.getIsCpuUnlimited())
                .memPercent(snapshot.getMemPercent())
                .memUsage(snapshot.getMemUsage())
                .memLimit(snapshot.getMemLimit())
                .isMemoryUnlimited(snapshot.getIsMemoryUnlimited())
                .rxBytesPerSec(snapshot.getRxBytesPerSec())
                .txBytesPerSec(snapshot.getTxBytesPerSec())
                .state(snapshot.getState())
                .health(snapshot.getHealth())
                .imageSize(snapshot.getImageSize())
                .sizeRootFs(snapshot.getSizeRootFs())
                .storageLimit(snapshot.getStorageLimit())
                .isStorageUnlimited(snapshot.getIsStorageUnlimited())
                .isFavorite(isFavorite)
                .build();
    }

    @Deprecated
    public static ContainerSummaryResponseDTO of(Container container, ContainerStatsLog containerStatsLog) {
        // 활성 상태일 때만 메트릭 표시 (RUNNING, RESTARTING, PAUSED)
        // 나머지 상태(CREATED, EXITED, DEAD, DELETED, UNKNOWN)는 메트릭 0 표시
        boolean isActiveState = container.getState() == ContainerState.RUNNING
                || container.getState() == ContainerState.RESTARTING
                || container.getState() == ContainerState.PAUSED;
        boolean hasStatsLog = containerStatsLog != null && isActiveState;

        return ContainerSummaryResponseDTO.builder()
                .id(container.getId())
                .agentName(container.getAgent() != null ? container.getAgent().getAgentName() : null)
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .cpuPercent(hasStatsLog ? containerStatsLog.getCpuPercent() : BigDecimal.ZERO)
                .isCpuUnlimited(container.getIsCpuUnlimited())
                .memPercent(hasStatsLog ? containerStatsLog.getMemPercent() : BigDecimal.ZERO)
                .memUsage(hasStatsLog ? containerStatsLog.getMemUsage() : 0L)
                .memLimit(container.getMemLimit())
                .isMemoryUnlimited(container.getIsMemoryUnlimited())
                .rxBytesPerSec(hasStatsLog ? containerStatsLog.getRxBytesPerSec() : 0L)
                .txBytesPerSec(hasStatsLog ? containerStatsLog.getTxBytesPerSec() : 0L)
                .imageSize(container.getImageSize())
                .state(container.getState())
                .health(hasStatsLog ? containerStatsLog.getHealth() : ContainerHealth.NONE)
                .sizeRootFs(hasStatsLog ? containerStatsLog.getSizeRootFs() : 0L)
                .storageLimit(container.getStorageLimit())
                .isStorageUnlimited(container.getIsStorageUnlimited())
                .isFavorite(false)  // 기본값
                .build();
    }

    /**
     * storageLimit을 변경한 새로운 DTO 반환 (불변성 유지)
     * @param newStorageLimit 새로운 스토리지 제한
     * @return storageLimit이 변경된 새 DTO
     */
    public ContainerSummaryResponseDTO changeStorageLimit(Long newStorageLimit) {
        return ContainerSummaryResponseDTO.builder()
                .id(this.id)
                .agentName(this.agentName)
                .containerHash(this.containerHash)
                .containerName(this.containerName)
                .cpuPercent(this.cpuPercent)
                .isCpuUnlimited(this.isCpuUnlimited)
                .memPercent(this.memPercent)
                .memUsage(this.memUsage)
                .memLimit(this.memLimit)
                .isMemoryUnlimited(this.isMemoryUnlimited)
                .rxBytesPerSec(this.rxBytesPerSec)
                .txBytesPerSec(this.txBytesPerSec)
                .state(this.state)
                .health(this.health)
                .imageSize(this.imageSize)
                .sizeRootFs(this.sizeRootFs)
                .storageLimit(newStorageLimit)
                .isStorageUnlimited(this.isStorageUnlimited)
                .isFavorite(this.isFavorite)
                .build();
    }
}
