package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentSummaryResponseDTO {
    private Long id;

    private String agentKey;

    private String agentName;

    private String description;

    private AgentStatus agentStatus;

    private LocalDateTime createdAt;

    public static AgentSummaryResponseDTO from(Agent agent) {
        return AgentSummaryResponseDTO.builder()
                .id(agent.getId())
                .agentKey(agent.getAgentKey())
                .agentName(agent.getAgentName())
                .description(agent.getDescription())
                .agentStatus(agent.getAgentStatus())
                .createdAt(agent.getCreatedAt())
                .build();
    }
}
