package com.monito.domains.dashboard.repository;

import com.monito.domains.container.domain.Container;
import com.monito.domains.dashboard.dto.response.metrics.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO;
import com.monito.domains.dashboard.dto.response.metrics.ContainerStorageUsageDTO;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends JpaRepository<Container, Long> {

    /**
     * Agent별 컨테이너 개수 집계
     * @return Agent별 컨테이너 개수 목록
     */
    @Query("SELECT new com.monito.domains.dashboard.dto.response.metrics.AgentContainerCountDTO(" +
            "a.id, a.agentName, COUNT(c.id)) " +
            "FROM Container c " +
            "JOIN c.agent a " +
            "GROUP BY a.id, a.agentName " +
            "ORDER BY COUNT(c.id) DESC")
    List<AgentContainerCountDTO> countContainersByAgent();

    /**
     * 전체 컨테이너의 스토리지 사용량 조회
     * @return 전체 컨테이너의 스토리지 할당량과 사용량 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.metrics.ContainerStorageUsageDTO(
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

    /**
     * 특정 컨테이너의 스토리지 사용량 조회
     * @param containerId 컨테이너 ID
     * @return 해당 컨테이너의 스토리지 사용량 (storageUsed)
     */
    @Query(value = """
            SELECT COALESCE(latest.sizeRootFs, 0L)
            FROM Container c
            LEFT JOIN ContainerStatsLog latest ON latest.container = c
                AND latest.createdAt = (
                    SELECT MAX(csl.createdAt)
                    FROM ContainerStatsLog csl
                    WHERE csl.container = c
                )
            WHERE c.id = :containerId
            """)
    Long findStorageUsedByContainerId(@Param("containerId") Long containerId);

    /**
     * 컨테이너 카드 목록 조회 (Container 테이블 기준, 필터 없음)
     * @param memberId 회원 ID (즐겨찾기 여부 확인용)
     * @param sort 정렬 기준 (FAVORITE, NAME, CPU, MEM)
     * @return 컨테이너 카드 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO(
                c.id,
                c.name,
                c.containerHash,
                a.id,
                (SELECT sl.cpuPercent
                 FROM ContainerStatsLog sl
                 WHERE sl.container.id = c.id
                   AND sl.collectedAt = (
                       SELECT MAX(sl2.collectedAt)
                       FROM ContainerStatsLog sl2
                       WHERE sl2.container.id = c.id
                   )
                ),
                (SELECT sl.memPercent
                 FROM ContainerStatsLog sl
                 WHERE sl.container.id = c.id
                   AND sl.collectedAt = (
                       SELECT MAX(sl2.collectedAt)
                       FROM ContainerStatsLog sl2
                       WHERE sl2.container.id = c.id
                   )
                ),
                c.state,
                CASE
                    WHEN c.state = com.monito.domains.container.domain.ContainerState.RUNNING THEN
                        (SELECT sl.health
                         FROM ContainerStatsLog sl
                         WHERE sl.container.id = c.id
                           AND sl.collectedAt = (
                               SELECT MAX(sl2.collectedAt)
                               FROM ContainerStatsLog sl2
                               WHERE sl2.container.id = c.id
                           )
                        )
                    ELSE com.monito.domains.container.domain.ContainerHealth.NONE
                END,
                CASE WHEN f.id IS NOT NULL THEN true ELSE false END
            )
            FROM Container c
            JOIN c.agent a
            LEFT JOIN com.monito.domains.favorite.domain.Favorite f
                ON f.container.id = c.id
                AND f.member.id = :memberId
            WHERE c.state != com.monito.domains.container.domain.ContainerState.UNKNOWN
            ORDER BY
                CASE WHEN :sort = 'FAVORITE' THEN
                    CASE WHEN f.id IS NOT NULL THEN 0 ELSE 1 END
                    ELSE NULL
                END ASC,
                CASE WHEN :sort = 'FAVORITE' THEN c.name ELSE NULL END ASC,
                CASE WHEN :sort = 'NAME' THEN c.name ELSE NULL END ASC,
                CASE WHEN :sort = 'CPU' THEN
                    (SELECT sl.cpuPercent
                     FROM ContainerStatsLog sl
                     WHERE sl.container.id = c.id
                       AND sl.collectedAt = (
                           SELECT MAX(sl2.collectedAt)
                           FROM ContainerStatsLog sl2
                           WHERE sl2.container.id = c.id
                       )
                    )
                    ELSE NULL
                END DESC,
                CASE WHEN :sort = 'MEM' THEN
                    (SELECT sl.memPercent
                     FROM ContainerStatsLog sl
                     WHERE sl.container.id = c.id
                       AND sl.collectedAt = (
                           SELECT MAX(sl2.collectedAt)
                           FROM ContainerStatsLog sl2
                           WHERE sl2.container.id = c.id
                       )
                    )
                    ELSE NULL
                END DESC
            """)
    List<ContainerCardResponseDTO> findAllContainerCardsForDashboard(
            @Param("memberId") Long memberId,
            @Param("sort") String sort
    );

    /**
     * 필터링된 컨테이너 카드 목록 조회 (Container 테이블 기준)
     * @param memberId 회원 ID (즐겨찾기 여부 확인용)
     * @param keyword 검색 키워드 (컨테이너 이름, 이미지명)
     * @param state 상태 필터 (단일 값)
     * @param health 헬스 필터 (단일 값)
     * @param favoriteOnly 즐겨찾기만 보기
     * @param agentId 에이전트 ID 필터 (단일 값)
     * @param sort 정렬 기준 (FAVORITE, NAME, CPU, MEM)
     * @return 필터링된 컨테이너 카드 목록
     */
    @Query(value = """
            SELECT new com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO(
                c.id,
                c.name,
                c.containerHash,
                a.id,
                (SELECT sl.cpuPercent
                 FROM ContainerStatsLog sl
                 WHERE sl.container.id = c.id
                   AND sl.collectedAt = (
                       SELECT MAX(sl2.collectedAt)
                       FROM ContainerStatsLog sl2
                       WHERE sl2.container.id = c.id
                   )
                ),
                (SELECT sl.memPercent
                 FROM ContainerStatsLog sl
                 WHERE sl.container.id = c.id
                   AND sl.collectedAt = (
                       SELECT MAX(sl2.collectedAt)
                       FROM ContainerStatsLog sl2
                       WHERE sl2.container.id = c.id
                   )
                ),
                c.state,
                CASE
                    WHEN c.state = com.monito.domains.container.domain.ContainerState.RUNNING THEN
                        (SELECT sl.health
                         FROM ContainerStatsLog sl
                         WHERE sl.container.id = c.id
                           AND sl.collectedAt = (
                               SELECT MAX(sl2.collectedAt)
                               FROM ContainerStatsLog sl2
                               WHERE sl2.container.id = c.id
                           )
                        )
                    ELSE com.monito.domains.container.domain.ContainerHealth.NONE
                END,
                CASE WHEN f.id IS NOT NULL THEN true ELSE false END
            )
            FROM Container c
            JOIN c.agent a
            LEFT JOIN com.monito.domains.favorite.domain.Favorite f
                ON f.container.id = c.id
                AND f.member.id = :memberId
            WHERE c.state != com.monito.domains.container.domain.ContainerState.UNKNOWN
                AND (:keyword IS NULL OR :keyword = ''
                     OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                     OR LOWER(c.imageName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                AND (:state IS NULL OR c.state = :state)
                AND (
                    :health IS NULL
                    OR (
                        c.state = com.monito.domains.container.domain.ContainerState.RUNNING
                        AND (
                            SELECT sl.health
                            FROM ContainerStatsLog sl
                            WHERE sl.container.id = c.id
                              AND sl.collectedAt = (
                                  SELECT MAX(sl2.collectedAt)
                                  FROM ContainerStatsLog sl2
                                  WHERE sl2.container.id = c.id
                              )
                        ) = :health
                    )
                )
                AND (:favoriteOnly = false OR f.id IS NOT NULL)
                AND (:agentId IS NULL OR c.agent.id = :agentId)
            ORDER BY
                CASE WHEN :sort = 'FAVORITE' THEN
                    CASE WHEN f.id IS NOT NULL THEN 0 ELSE 1 END
                    ELSE NULL
                END ASC,
                CASE WHEN :sort = 'FAVORITE' THEN c.name ELSE NULL END ASC,
                CASE WHEN :sort = 'NAME' THEN c.name ELSE NULL END ASC,
                CASE WHEN :sort = 'CPU' THEN
                    (SELECT sl.cpuPercent
                     FROM ContainerStatsLog sl
                     WHERE sl.container.id = c.id
                       AND sl.collectedAt = (
                           SELECT MAX(sl2.collectedAt)
                           FROM ContainerStatsLog sl2
                           WHERE sl2.container.id = c.id
                       )
                    )
                    ELSE NULL
                END DESC,
                CASE WHEN :sort = 'MEM' THEN
                    (SELECT sl.memPercent
                     FROM ContainerStatsLog sl
                     WHERE sl.container.id = c.id
                       AND sl.collectedAt = (
                           SELECT MAX(sl2.collectedAt)
                           FROM ContainerStatsLog sl2
                           WHERE sl2.container.id = c.id
                       )
                    )
                    ELSE NULL
                END DESC
            """)
    List<ContainerCardResponseDTO> findContainerCardsWithFilters(
            @Param("memberId") Long memberId,
            @Param("keyword") String keyword,
            @Param("state") com.monito.domains.container.domain.ContainerState state,
            @Param("health") com.monito.domains.container.domain.ContainerHealth health,
            @Param("favoriteOnly") Boolean favoriteOnly,
            @Param("agentId") Long agentId,
            @Param("sort") String sort
    );
}
