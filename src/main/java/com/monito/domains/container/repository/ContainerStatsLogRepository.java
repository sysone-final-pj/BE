package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerStatsLog;
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
}
