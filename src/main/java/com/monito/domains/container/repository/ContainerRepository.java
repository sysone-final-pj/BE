package com.monito.domains.container.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerRepository extends JpaRepository<Container, Long>, JpaSpecificationExecutor<Container> {

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
     * 특정 Agent의 활성 컨테이너 전체 조회 (soft delete 제외 || ID 기반)
     */
    List<Container> findAllByAgent_Id(Long agentId);

    /**
     * 특정 Agent의 활성 컨테이너 전체 조회 (soft delete 제외 || 객체 기반)
     */
    List<Container> findAllByAgent(Agent agent);

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

    /**
     * 삭제된 컨테이너 목록 조회 (24시간 이내)
     * - is_deleted = 1인 컨테이너만 조회
     * - updatedAt(삭제 시간)이 24시간 이내인 것만 조회
     * - updatedAt 기준 내림차순 정렬 (최근 삭제된 것부터)
     * - @SQLRestriction 우회를 위해 네이티브 쿼리 사용
     * - N+1 문제는 Service 레이어에서 agent를 명시적으로 로드하여 해결
     * @param since 조회 시작 시간 (현재 시간 - 24시간)
     * @return 삭제된 Container 리스트
     */
    @Query(value = "SELECT c.* FROM containers c " +
            "WHERE c.is_deleted = 1 " +
            "AND c.updated_at >= :since " +
            "ORDER BY c.updated_at DESC",
            nativeQuery = true)
    List<Container> findAllDeletedWithin24Hours(@Param("since") LocalDateTime since);
}
