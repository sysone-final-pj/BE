/**
 * 삭제된 컨테이너 목록 조회 응답 DTO
 */
package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DeletedContainerResponseDTO {
    private String agentName;
    private Long containerId;
    private String containerName;
    private String containerHash;
    private ContainerState state;
    private LocalDateTime deletedAt;  // updatedAt을 deletedAt으로 사용

    public static DeletedContainerResponseDTO from(Container container) {
        // Agent가 null이거나 삭제되었으면 "-" 표시
        String agentName = "-";
        if (container.getAgent() != null) {
            agentName = container.getAgent().getAgentName() != null
                    ? container.getAgent().getAgentName()
                    : "-";
        }

        return DeletedContainerResponseDTO.builder()
                .agentName(agentName)
                .containerId(container.getId())
                .containerName(container.getName())
                .containerHash(container.getContainerHash())
                .state(container.getState())
                .deletedAt(container.getUpdatedAt())  // updatedAt을 deletedAt으로 매핑
                .build();
    }
}