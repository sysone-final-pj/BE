/**
 * 컨테이너 카드 리스트용 경량 DTO
 * WebSocket 리스트 브로드캐스트에 사용 (/topic/dashboard/list)
 */
package com.monito.domains.dashboard.dto.response;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerCardResponseDTO {

    /**
     * 컨테이너 ID (식별용)
     */
    private Long containerId;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * 컨테이너 해시 (12자리)
     */
    private String containerHash;

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * Agent Name
     */
    private String agentName;

    /**
     * CPU 사용률 (%)
     */
    private BigDecimal cpuPercent;

    /**
     * 메모리 사용률 (%)
     */
    private BigDecimal memPercent;

    /**
     * 컨테이너 상태 (RUNNING, STOPPED 등)
     */
    private String state;

    /**
     * 헬스 상태
     */
    private String health;

    /**
     * 즐겨찾기 여부
     */
    private Boolean isFavorite;

    /**
     * Container와 StatsLog로부터 생성
     */
    public static ContainerCardResponseDTO of(Container container, ContainerStatsLog statsLog) {
        return ContainerCardResponseDTO.builder()
                .containerId(container.getId())
                .containerName(container.getName())
                .containerHash(container.getContainerHash())
                .agentId(container.getAgent().getId())
                .agentName(container.getAgent().getAgentName())
                .cpuPercent(statsLog.getCpuPercent())
                .memPercent(statsLog.getMemPercent())
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                .isFavorite(false)
                .build();
    }

    /**
     * Container와 StatsLog, 즐겨찾기 여부로부터 생성
     */
    public static ContainerCardResponseDTO of(Container container, ContainerStatsLog statsLog, Boolean isFavorite) {
        return ContainerCardResponseDTO.builder()
                .containerId(container.getId())
                .containerName(container.getName())
                .containerHash(container.getContainerHash())
                .agentId(container.getAgent().getId())
                .agentName(container.getAgent().getAgentName())
                .cpuPercent(statsLog.getCpuPercent())
                .memPercent(statsLog.getMemPercent())
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                .isFavorite(isFavorite)
                .build();
    }

    /**
     * JPQL 쿼리에서 사용하는 생성자 (enum 타입 직접 수용)
     */
    public ContainerCardResponseDTO(Long containerId, String containerName, String containerHash, Long agentId,
                                     String agentName, BigDecimal cpuPercent, BigDecimal memPercent,
                                     ContainerState state, ContainerHealth health,
                                     Boolean isFavorite) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.containerHash = containerHash;
        this.agentId = agentId;
        this.agentName = agentName;
        this.cpuPercent = cpuPercent;
        this.memPercent = memPercent;
        this.state = state != null ? state.name() : null;
        this.health = health != null ? health.name() : null;
        this.isFavorite = isFavorite;
    }
}