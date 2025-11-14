package com.monito.domains.container.dto.response.metrics;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.util.ImageIdUtil;
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
    private String imageId;
    private Long imageSize;
    private ContainerState state;
    private ContainerHealth health;

    public static ContainerInfoDTO from(
            Container container,
            Agent agent,
            ContainerState state
    ) {
        return ContainerInfoDTO.builder()
                .containerId(container.getId())
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .agentName(agent.getAgentName())
                .imageName(ImageIdUtil.removePrefix(container.getImageName()))
                .imageId(ImageIdUtil.removePrefix(container.getImageId()))
                .imageSize(container.getImageSize())
                .state(state)
                .build();
    }
}