package com.monito.domains.container.repository;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.dto.response.ContainerListResponseDTO;
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
     * Agent ID로 컨테이너 리스트 조회 (최신 통계 포함)
     * LATERAL JOIN을 사용하여 각 컨테이너의 최신 StatsLog 조회
     * @param agentId Agent ID
     * @return 컨테이너 리스트
     */
    @Query(value = """
            SELECT new com.monito.domains.container.dto.response.ContainerListResponseDTO(
                c.id,
                c.containerHash,
                c.name,
                a.agentName,
                latest.state,
                latest.cpuPercent,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.blkRead,
                latest.blkWrite,
                latest.rxMbps,
                latest.txMbps
            )
            FROM Container c
            JOIN c.agent a
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            WHERE a.id = :agentId
            """)
    List<ContainerListResponseDTO> findContainerListByAgentId(@Param("agentId") Long agentId);

    /**
     * 모든 컨테이너 리스트 조회 (최신 통계 포함)
     * @return 전체 컨테이너 리스트
     */
    @Query(value = """
            SELECT new com.monito.domains.container.dto.response.ContainerListResponseDTO(
                c.id,
                c.containerHash,
                c.name,
                a.agentName,
                latest.state,
                latest.cpuPercent,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.blkRead,
                latest.blkWrite,
                latest.rxMbps,
                latest.txMbps
            )
            FROM Container c
            JOIN c.agent a
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            """)
    List<ContainerListResponseDTO> findAllContainerList();
}
