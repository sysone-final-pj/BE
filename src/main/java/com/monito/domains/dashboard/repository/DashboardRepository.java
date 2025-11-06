package com.monito.domains.dashboard.repository;

import com.monito.domains.container.domain.Container;
import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.dashboard.dto.response.ContainerStorageUsageDTO;
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
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

    /**
     * 특정 멤버의 즐겨찾기 컨테이너 ID 목록 조회
     * @param memberId Member ID
     * @return 즐겨찾기 컨테이너 ID 목록
     */
    @Query("SELECT f.container.id FROM com.monito.domains.favorite.domain.Favorite f WHERE f.member.id = :memberId")
    List<Long> findFavoriteContainerIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 필터링된 컨테이너 목록 조회 (최신 통계 기준)
     * @param keyword 검색 키워드
     * @param favoriteOnly 즐겨찾기만 보기
     * @param states 상태 필터
     * @param healths 헬스 필터
     * @param agentIds 에이전트 ID 필터
     * @param memberId 회원 ID (즐겨찾기 필터 시 사용)
     * @return 필터링된 컨테이너 목록
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
                latest.cpuCoreUsage,
                latest.cpuUsageTotal,
                latest.hostCpuUsageTotal,
                latest.cpuUser,
                latest.cpuSystem,
                latest.cpuQuota,
                latest.cpuPeriod,
                latest.onlineCpus,
                latest.throttlingPeriods,
                latest.throttledPeriods,
                latest.throttledTime,
                latest.memPercent,
                latest.memUsage,
                c.memLimit,
                latest.memMaxUsage,
                latest.blkRead,
                latest.blkWrite,
                latest.blkReadPerSec,
                latest.blkWritePerSec,
                latest.rxBytes,
                latest.txBytes,
                latest.rxPackets,
                latest.txPackets,
                latest.networkTotalBytes,
                latest.rxBytesPerSec,
                latest.txBytesPerSec,
                latest.rxPps,
                latest.txPps,
                latest.rxFailureRate,
                latest.txFailureRate,
                latest.rxErrors,
                latest.txErrors,
                latest.rxDropped,
                latest.txDropped,
                latest.sizeRw,
                latest.sizeRootFs
            )
            FROM Container c
            JOIN c.agent a
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            WHERE (:keywordEmpty = true OR
                   LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
                   LOWER(c.imageName) LIKE LOWER(CONCAT('%', :keyword, '%')))
            AND (:favoriteOnly = false OR c.id IN (
                SELECT f.container.id FROM com.monito.domains.favorite.domain.Favorite f
                WHERE f.member.id = :memberId
            ))
            AND (:statesEmpty = true OR latest.state IN :states)
            AND (:healthsEmpty = true OR latest.health IN :healths)
            AND (:agentIdsEmpty = true OR a.id IN :agentIds)
            """)
    List<ContainerDashboardResponseDTO> findContainersWithFilters(
            @Param("keyword") String keyword,
            @Param("keywordEmpty") boolean keywordEmpty,
            @Param("favoriteOnly") boolean favoriteOnly,
            @Param("states") List<com.monito.domains.container.domain.ContainerState> states,
            @Param("statesEmpty") boolean statesEmpty,
            @Param("healths") List<com.monito.domains.container.domain.ContainerHealth> healths,
            @Param("healthsEmpty") boolean healthsEmpty,
            @Param("agentIds") List<Long> agentIds,
            @Param("agentIdsEmpty") boolean agentIdsEmpty,
            @Param("memberId") Long memberId
    );

    /**
     * 전체 컨테이너의 스토리지 사용량 조회
     * @return 전체 컨테이너의 스토리지 할당량과 사용량 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerStorageUsageDTO(
                c.id,
                c.name,
                c.storageLimit,
                COALESCE(latest.sizeRootFs, 0L)
            )
            FROM Container c
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            """)
    List<ContainerStorageUsageDTO> findAllContainerStorageUsage();
}
