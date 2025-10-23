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
    private String password;
    private String osType;
    private String dockerVersion;
    private AgentStatus agentStatus;

    public Agent toEntity() {
        return Agent.builder()
                .agentName(agentName)
                .password(password)
                .osType(osType)
                .dockerVersion(dockerVersion)
                .agentStatus(agentStatus)
                .build();
    }
}
