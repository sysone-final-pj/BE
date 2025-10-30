package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerStatsLog;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContainerStatsLogRepository extends JpaRepository<ContainerStatsLog, Long> {

    /**
     * 특정 컨테이너 해시의 최신 통계 로그 조회
     * @param containerHash 컨테이너 해시
     * @return 최신 ContainerStatsLog
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.containerHash = :containerHash " +
           "ORDER BY csl.createdAt DESC " +
           "LIMIT 1")
    Optional<ContainerStatsLog> findLatestByContainerHash(@Param("containerHash") String containerHash);

    /**
     * 특정 컨테이너의 시간 범위 내 통계 로그 조회 (Container ID 기준)
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 시간 범위 내 ContainerStatsLog 리스트 (시간 오름차순)
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.createdAt ASC")
    List<ContainerStatsLog> findByContainerIdAndTimeRange(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 컨테이너의 시간 범위 내 통계 로그 조회 (Container Hash 기준)
     * @param containerHash 컨테이너 해시
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 시간 범위 내 ContainerStatsLog 리스트 (시간 오름차순)
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.containerHash = :containerHash " +
           "AND csl.createdAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.createdAt ASC")
    List<ContainerStatsLog> findByContainerHashAndTimeRange(
            @Param("containerHash") String containerHash,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 컨테이너의 최근 N개 통계 로그 조회
     * @param containerId 컨테이너 ID
     * @param limit 조회 개수
     * @return 최근 N개의 ContainerStatsLog 리스트 (시간 내림차순)
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "ORDER BY csl.createdAt DESC " +
           "LIMIT :limit")
    List<ContainerStatsLog> findRecentStatsByContainerId(
            @Param("containerId") Long containerId,
            @Param("limit") int limit
    );
}
