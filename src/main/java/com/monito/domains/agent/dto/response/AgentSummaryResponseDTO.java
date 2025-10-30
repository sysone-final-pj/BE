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
public class AgentSummaryResponseDTO {
    private Long id;

    private String agentName;

    private String description;

    private AgentStatus agentStatus;

    public static AgentSummaryResponseDTO from(Agent agent) {
        return AgentSummaryResponseDTO.builder()
                .id(agent.getId())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentStatus(agent.getAgentStatus())
                .build();
    }
}
