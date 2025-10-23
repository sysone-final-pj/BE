package com.monito.domains.agent.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    /**
     * AgentKey로 Agent 조회 (WebSocket 인증용)
     * @param agentKey UUID 기반 Agent 식별 키
     * @return Agent
     */
    Optional<Agent> findByAgentKey(String agentKey);
}
