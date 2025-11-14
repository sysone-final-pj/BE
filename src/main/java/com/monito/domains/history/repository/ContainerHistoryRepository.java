package com.monito.domains.history.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monito.domains.container.domain.ContainerStatsLog;

import java.time.LocalDateTime;

@Repository
public interface ContainerHistoryRepository extends JpaRepository<ContainerStatsLog, Long> {

    /**
     * 컨테이너 히스토리 조회 (페이지네이션)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param containerId 컨테이너 ID (null 가능)
     * @param isDeleted 삭제 여부 (null 가능)
     * @param pageable 페이지 정보
     * @return 컨테이너 히스토리 페이지
     */
    @Query("""
        SELECT
            csl.collectedAt,
            c.name,
            a.agentName,
            c.imageName,
            csl.state,
            csl.health,
            c.createdAt,
            c.isDeleted,

            csl.cpuPercent,
            csl.cpuCoreUsage,
            csl.hostCpuUsageTotal,
            csl.cpuUsageTotal,
            csl.cpuUser,
            csl.cpuSystem,
            csl.cpuQuota,
            csl.cpuPeriod,
            csl.onlineCpus,
            csl.throttlingPeriods,
            csl.throttledPeriods,
            csl.throttledTime,
            c.cpuLimitCores,
            c.isCpuUnlimited,

            csl.memPercent,
            csl.memUsage,
            csl.memMaxUsage,
            c.memLimit,
            c.isMemoryUnlimited,
            c.lastOomKilledAt,

            csl.blkRead,
            csl.blkWrite,
            csl.blkReadPerSec,
            csl.blkWritePerSec,

            csl.rxBytes,
            csl.txBytes,
            csl.rxPackets,
            csl.txPackets,
            csl.networkTotalBytes,
            csl.rxBytesPerSec,
            csl.txBytesPerSec,
            csl.rxPps,
            csl.txPps,
            csl.rxFailureRate,
            csl.txFailureRate,
            csl.rxErrors,
            csl.txErrors,
            csl.rxDropped,
            csl.txDropped,

            csl.sizeRw,
            csl.sizeRootFs,
            c.storageLimit,
            c.isStorageUnlimited

        FROM ContainerStatsLog csl
        JOIN csl.container c
        JOIN c.agent a
        WHERE csl.collectedAt BETWEEN :startTime AND :endTime
            AND (:containerId IS NULL OR c.id = :containerId)
            AND (:isDeleted IS NULL OR c.isDeleted = :isDeleted)
        ORDER BY csl.collectedAt DESC
        """)
    Page<Object[]> findContainerHistory(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("containerId") Long containerId,
            @Param("isDeleted") Integer isDeleted,
            Pageable pageable
    );
}