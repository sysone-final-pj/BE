/**
 * 캐시에 저장되는 컨테이너 스냅샷 (사용자별 상태 없음)
 * - 모든 사용자가 공유하는 순수 컨테이너 데이터
 * - isFavorite 필드 없음 (사용자별 상태는 FavoriteCache에서 관리)
 */
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
public class ContainerSummarySnapshot {
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

    /**
     * Container + ContainerStatsLog → Snapshot 변환
     */
    public static ContainerSummarySnapshot of(Container container, ContainerStatsLog containerStatsLog) {
        // 활성 상태일 때만 메트릭 표시 (RUNNING, RESTARTING, PAUSED)
        // 나머지 상태(CREATED, EXITED, DEAD, DELETED, UNKNOWN)는 메트릭 0 표시
        boolean isActiveState = container.getState() == ContainerState.RUNNING
                || container.getState() == ContainerState.RESTARTING
                || container.getState() == ContainerState.PAUSED;
        boolean hasStatsLog = containerStatsLog != null && isActiveState;

        return ContainerSummarySnapshot.builder()
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
                .build();
    }

    /**
     * storageLimit을 변경한 새로운 Snapshot 반환 (불변성 유지)
     * @param newStorageLimit 새로운 스토리지 제한
     * @return storageLimit이 변경된 새 Snapshot
     */
    public ContainerSummarySnapshot changeStorageLimit(Long newStorageLimit) {
        return ContainerSummarySnapshot.builder()
                .id(this.id)
                .agentName(this.agentName)
                .containerHash(this.containerHash)
                .containerName(this.containerName)
                .cpuPercent(this.cpuPercent)
                .isCpuUnlimited(this.isCpuUnlimited)
                .memUsage(this.memUsage)
                .memLimit(this.memLimit)
                .isMemoryUnlimited(this.isMemoryUnlimited)
                .memPercent(this.memPercent)
                .rxBytesPerSec(this.rxBytesPerSec)
                .txBytesPerSec(this.txBytesPerSec)
                .state(this.state)
                .health(this.health)
                .imageSize(this.imageSize)
                .sizeRootFs(this.sizeRootFs)
                .storageLimit(newStorageLimit)
                .isStorageUnlimited(this.isStorageUnlimited)
                .build();
    }
}