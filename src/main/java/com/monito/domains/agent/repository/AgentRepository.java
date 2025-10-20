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
     * 특정 상태의 Agent 목록 조회
     * @param status Agent 상태 (ONLINE, OFFLINE 등)
     * @return 해당 상태의 Agent 목록
     */
    List<Agent> findByAgentStatus(AgentStatus status);

    /**
     * Host IP와 Port로 Agent 조회
     * @param hostIp 호스트 IP
     * @param hostPort 호스트 포트
     * @return Agent
     */
    Optional<Agent> findByHostIpAndHostPort(String hostIp, Integer hostPort);
}
