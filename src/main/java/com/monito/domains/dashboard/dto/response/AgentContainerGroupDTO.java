package com.monito.domains.dashboard.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Agent별 컨테이너 그룹 DTO
 * - Agent 정보와 해당 Agent의 컨테이너 목록 포함
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentContainerGroupDTO {

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * Agent 이름
     */
    private String agentName;

    /**
     * 해당 Agent의 컨테이너 개수
     */
    private Long containerCount;

    /**
     * 해당 Agent의 컨테이너 목록
     */
    private List<ContainerDashboardResponseDTO> containers;
}