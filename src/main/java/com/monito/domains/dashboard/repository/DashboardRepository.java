package com.monito.domains.dashboard.repository;

import com.monito.domains.container.domain.Container;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends JpaRepository<Container, Long> {

    /**
     * 대시보드용 전체 컨테이너 리스트 조회 (최신 통계 포함)
     * @return 전체 컨테이너 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO(
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
                latest.rxBytesPerSec,
                latest.txBytesPerSec
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
    List<ContainerDashboardResponseDTO> findAllContainersForDashboard();

    /**
     * Agent별 컨테이너 리스트 조회 (최신 통계 포함)
     * @param agentId Agent ID
     * @return Agent에 속한 컨테이너 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO(
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
                latest.rxBytesPerSec,
                latest.txBytesPerSec
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
    List<ContainerDashboardResponseDTO> findContainersByAgentId(@Param("agentId") Long agentId);
}
