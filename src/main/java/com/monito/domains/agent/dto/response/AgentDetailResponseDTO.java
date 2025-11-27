package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentDetailResponseDTO {
    private Long id;

    private String agentKey;

    private String agentName;

    private String description;

    private AgentStatus agentStatus;

    public static AgentDetailResponseDTO from(Agent agent) {
        return AgentDetailResponseDTO.builder()
                .id(agent.getId())
                .agentKey(agent.getAgentKey())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentStatus(agent.getAgentStatus())
                .build();
    }
}
