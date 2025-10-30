package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentUpdateResponseDTO {
    private String agentName;
    private String description;

    public static AgentUpdateResponseDTO from(Agent agent) {
        return AgentUpdateResponseDTO.builder()
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .build();
    }
}
