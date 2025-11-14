package com.monito.domains.dashboard.dto.response;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.util.ImageIdUtil;
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
     * 이미지 이름
     */
    private String imageName;

    /**
     * 이미지 ID (짧은 버전)
     */
    private String imageId;

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
                .cpuPercent(statsLog.getCpuPercent())
                .memPercent(statsLog.getMemPercent())
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                .imageName(ImageIdUtil.removePrefix(container.getImageName()))
                .imageId(ImageIdUtil.removePrefix(container.getImageId()))
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
                .cpuPercent(statsLog.getCpuPercent())
                .memPercent(statsLog.getMemPercent())
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                .imageName(ImageIdUtil.removePrefix(container.getImageName()))
                .imageId(ImageIdUtil.removePrefix(container.getImageId()))
                .isFavorite(isFavorite)
                .build();
    }

    /**
     * JPQL 쿼리에서 사용하는 생성자 (enum 타입 직접 수용)
     */
    public ContainerCardResponseDTO(Long containerId, String containerName,
                                     BigDecimal cpuPercent, BigDecimal memPercent,
                                     ContainerState state, ContainerHealth health,
                                     String imageName, String imageId, Boolean isFavorite) {
        this.containerId = containerId;
        this.containerName = containerName;
        this.cpuPercent = cpuPercent;
        this.memPercent = memPercent;
        this.state = state != null ? state.name() : null;
        this.health = health != null ? health.name() : null;
        this.imageName = ImageIdUtil.removePrefix(imageName);
        this.imageId = ImageIdUtil.removePrefix(imageId);
        this.isFavorite = isFavorite;
    }
}