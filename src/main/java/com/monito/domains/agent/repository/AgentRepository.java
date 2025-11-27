package com.monito.domains.agent.repository;

import com.monito.domains.agent.domain.Agent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
/**
 작성자: 백승준
 */
@Repository
public interface AgentRepository extends JpaRepository<Agent, Long> {
    /**
     * AgentKey로 Agent 조회 (WebSocket 인증용)
     * @param agentKey UUID 기반 Agent 식별 키
     * @return Agent
     */
    Optional<Agent> findByAgentKey(String agentKey);

    /**
     * Agent 목록 조회 및 검색 (통합 메서드)
     * - keyword가 null이거나 빈 값이면 전체 조회
     * - keyword가 있으면 agentName, agentKey, description에서 검색
     * - 대소문자 구분 없이 검색 (LOWER 사용)
     * @param keyword 검색어 (null 가능)
     * @return Agent 리스트
     */
    @Query("SELECT a FROM Agent a WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(a.agentName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.agentKey) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(a.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Agent> findAllWithSearch(@Param("keyword") String keyword);
}
