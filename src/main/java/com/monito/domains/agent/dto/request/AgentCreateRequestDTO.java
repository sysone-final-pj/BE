package com.monito.domains.agent.dto.request;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentCreateRequestDTO {
    private String agentName;
    private AgentStatus agentStatus;
    private String description;

    public Agent toEntity() {
        return Agent.builder()
                .agentName(agentName)
                .agentStatus(agentStatus)
                .description(description)
                .build();
    }
}
