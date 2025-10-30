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
     * 특정 컨테이너의 특정 기간 내 통계 로그 조회
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 통계 로그 목록 (시간 순 정렬)
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<ContainerStatsLog> findByContainerIdAndTimeRange(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
