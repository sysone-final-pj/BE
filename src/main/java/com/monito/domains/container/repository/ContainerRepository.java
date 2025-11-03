package com.monito.domains.container.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long> {

    /**
     * Agent와 컨테이너 해시로 컨테이너 조회
     * @param agent Agent 엔티티
     * @param containerHash 컨테이너 해시값
     * @return Container
     */
    Optional<Container> findByAgentAndContainerHash(Agent agent, String containerHash);

    /**
     * Agent 스코프로 컨테이너 해시 조회 (파생 메서드)
     */
    Optional<Container> findByAgentIdAndContainerHash(Long agentId, String containerHash);

    /**
     * 특정 Agent의 활성 컨테이너 전체 조회 (soft delete 제외)
     */
    List<Container> findAllByAgent_Id(Long agentId);

    /**
     * 컨테이너 목록 조회 및 검색 (통합 메서드)
     * - keyword가 null이거나 빈 값이면 전체 조회
     * - keyword가 있으면 agentName, containerHash, containerName에서 검색
     * - 대소문자 구분 없이 검색 (LOWER 사용)
     * - Agent도 함께 JOIN FETCH (N+1 문제 해결)
     * @param keyword 검색어 (null 가능)
     * @return Container 리스트
     */
    @Query("SELECT DISTINCT c FROM Container c " +
            "LEFT JOIN FETCH c.agent a " +
            "WHERE (:keyword IS NULL OR :keyword = '' OR " +
            "LOWER(a.agentName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.containerHash) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
            "LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<Container> findAllWithSearch(@Param("keyword") String keyword);
}
