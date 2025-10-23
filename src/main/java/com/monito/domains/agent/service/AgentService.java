package com.monito.domains.agent.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;

public interface AgentService {

    /**
     * AgentKey로 Agent 인증 (WebSocket용)
     * @param agentKey UUID 기반 Agent 식별 키
     * @return 인증 성공 시 Agent 객체, 실패 시 empty
     */
    Agent authenticateAgent(String agentKey);

    /**
     * Agent 상태 업데이트
     * @param agentKey Agent 식별 키
     * @param status 변경할 상태
     */
    void updateAgentStatus(String agentKey, AgentStatus status);

    /**
     * AgentKey로 Agent 조회
     * @param agentKey Agent 식별 키
     * @return Agent
     */
    Agent findByAgentKey(String agentKey);

    AgentCreateResponseDTO createAgent(AgentCreateRequestDTO dto);
}
