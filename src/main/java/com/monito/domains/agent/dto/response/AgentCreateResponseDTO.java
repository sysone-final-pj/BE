package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentCreateResponseDTO {
    private Long id;
    private String agentName;
    private String agentKey;
    private AgentStatus agentStatus;

    public static AgentCreateResponseDTO from(Agent agent) {
        return AgentCreateResponseDTO.builder()
                .id(agent.getId())
                .agentKey(agent.getAgentKey())
                .agentName(agent.getAgentName())
                .agentStatus(agent.getAgentStatus())
                .build();
    }
}
