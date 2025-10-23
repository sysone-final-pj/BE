package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentCreateResponseDTO {
    private Long id;
    private String agentName;
    private String osType;
    private String agentKey;
    private String dockerVersion;
    private AgentStatus agentStatus;

    public static AgentCreateResponseDTO from(Agent agent) {
        return AgentCreateResponseDTO.builder()
                .id(agent.getId())
                .agentKey(agent.getAgentKey())
                .agentName(agent.getAgentName())
                .osType(agent.getOsType())
                .dockerVersion(agent.getDockerVersion())
                .agentStatus(agent.getAgentStatus())
                .build();
    }
}
