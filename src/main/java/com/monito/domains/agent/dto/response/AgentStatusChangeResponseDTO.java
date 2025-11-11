package com.monito.domains.agent.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Agent 상태 변경 WebSocket 메시지 DTO
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentStatusChangeResponseDTO {
    private Long agentId;
    private String agentKey;
    private String agentName;
    private AgentStatus previousStatus;
    private AgentStatus currentStatus;
    private LocalDateTime changedAt;

    public static AgentStatusChangeResponseDTO of(Agent agent, AgentStatus previousStatus) {
        return AgentStatusChangeResponseDTO.builder()
                .agentId(agent.getId())
                .agentKey(agent.getAgentKey())
                .agentName(agent.getAgentName())
                .previousStatus(previousStatus)
                .currentStatus(agent.getAgentStatus())
                .changedAt(LocalDateTime.now())
                .build();
    }
}