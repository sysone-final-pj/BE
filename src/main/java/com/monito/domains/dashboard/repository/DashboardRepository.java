package com.monito.domains.dashboard.repository;

import com.monito.domains.container.domain.Container;
import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
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
                a.id,
                a.agentName,
                latest.state,
                latest.health,
                c.imageName,
                c.imageSize,
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
                a.id,
                a.agentName,
                latest.state,
                latest.health,
                c.imageName,
                c.imageSize,
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

    /**
     * Agent별 컨테이너 개수 집계
     * @return Agent별 컨테이너 개수 목록
     */
    @Query("SELECT new com.monito.domains.dashboard.dto.response.AgentContainerCountDTO(" +
            "a.id, a.agentName, COUNT(c.id)) " +
            "FROM Container c " +
            "JOIN c.agent a " +
            "GROUP BY a.id, a.agentName " +
            "ORDER BY COUNT(c.id) DESC")
    List<AgentContainerCountDTO> countContainersByAgent();

    /**
     * 구동중인 컨테이너 목록 조회 (state = RUNNING)
     * @return RUNNING 상태의 컨테이너 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO(
                c.id,
                c.containerHash,
                c.name,
                a.id,
                a.agentName,
                latest.state,
                latest.health,
                c.imageName,
                c.imageSize,
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
            WHERE latest.state = com.monito.domains.container.domain.ContainerState.RUNNING
            """)
    List<ContainerDashboardResponseDTO> findRunningContainers();

    /**
     * 특정 ID의 컨테이너 상세 정보 조회
     * @param containerId 컨테이너 ID
     * @return 컨테이너 상세 정보
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO(
                c.id,
                c.containerHash,
                c.name,
                a.id,
                a.agentName,
                latest.state,
                latest.health,
                c.imageName,
                c.imageSize,
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
            WHERE c.id = :containerId
            """)
    ContainerDashboardResponseDTO findContainerDetailById(@Param("containerId") Long containerId);

    /**
     * 특정 멤버의 즐겨찾기 컨테이너 목록 조회
     * @param memberId Member ID
     * @return 즐겨찾기 컨테이너 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO(
                c.id,
                c.containerHash,
                c.name,
                a.id,
                a.agentName,
                latest.state,
                latest.health,
                c.imageName,
                c.imageSize,
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
            JOIN com.monito.domains.favorite.domain.Favorite f ON f.container = c
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            WHERE f.member.id = :memberId
            """)
    List<ContainerDashboardResponseDTO> findFavoriteContainersByMemberId(@Param("memberId") Long memberId);
}
