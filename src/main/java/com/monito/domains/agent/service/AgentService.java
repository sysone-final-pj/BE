package com.monito.domains.agent.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentCreateRequestDTO;
import com.monito.domains.agent.dto.request.AgentUpdateRequestDTO;
import com.monito.domains.agent.dto.response.AgentCreateResponseDTO;
import com.monito.domains.agent.dto.response.AgentDetailResponseDTO;
import com.monito.domains.agent.dto.response.AgentSummaryResponseDTO;
import com.monito.domains.agent.dto.response.AgentUpdateResponseDTO;

import java.util.List;

public interface AgentService {

    /**
     * @param id Agent 기본 키
     * @return id로 조회 가능한 경우 AgentDetailResponseDTO 반환
     */
    AgentDetailResponseDTO getAgent(Long id);

    /**
     * Agent 목록 조회 및 검색
     * @param keyword 검색어 (null이면 전체 조회, 값이 있으면 agentName, agentKey, description에서 검색)
     * @return 조회된 Agent 리스트
     */
    List<AgentSummaryResponseDTO> getAgentList(String keyword);

    /**
     * @param id Agent 기본 키
     * @param agentUpdateRequestDTO 수정할 내용이 담겨 있는 DTO (agentName, description)
     * @return 수정이 완료된 Agent 객체를 AgentUpdateResponseDTO 형식으로 반환
     */
    AgentUpdateResponseDTO updateAgent(Long id, AgentUpdateRequestDTO agentUpdateRequestDTO);

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
     * AgentKey로 Agent 삭제
     * @param id Agent 식별 키
     */
    void deleteAgent(Long id);

    /**
     * AgentKey로 Agent 조회
     * @param agentKey Agent 식별 키
     * @return Agent
     */
    Agent findByAgentKey(String agentKey);

    AgentCreateResponseDTO createAgent(AgentCreateRequestDTO dto);
}
