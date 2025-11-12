package com.monito.domains.dashboard.dto.response;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 컨테이너 카드 리스트용 경량 DTO
 * WebSocket 리스트 브로드캐스트에 사용 (/topic/dashboard/list)
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
     * Container와 StatsLog로부터 생성
     */
    public static ContainerCardResponseDTO of(Container container, ContainerStatsLog statsLog) {
        return ContainerCardResponseDTO.builder()
                .containerId(container.getId())
                .containerName(container.getName())
                .cpuPercent(statsLog.getCpuPercent())
                .memPercent(statsLog.getMemPercent())
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                .build();
    }
}