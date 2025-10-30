package com.monito.domains.container.dto.response.metrics;

import com.monito.domains.container.domain.ContainerState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 컨테이너 정보
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerInfoDTO {
    private Long containerId;
    private String containerHash;
    private String containerName;
    private String agentName;
    private String imageName;
    private Long imageSize;
    private ContainerState state;
}