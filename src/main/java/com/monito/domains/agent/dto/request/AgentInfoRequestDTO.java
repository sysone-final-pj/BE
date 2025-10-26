package com.monito.domains.agent.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agent 메타데이터 정보 DTO
 * - WebSocket AGENT_INFO 메시지로 수신
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoRequestDTO {
    /**
     * Agent 식별 키
     */
    private String agentKey;

    /**
     * Host 정보
     */
    private AgentInfoDetailRequestDTO host;
}