package com.monito.domains.history.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.monito.domains.container.domain.ContainerStatsLog;

import java.time.LocalDateTime;
/**
 작성자: 이지민
 */
@Repository
public interface ContainerHistoryRepository extends JpaRepository<ContainerStatsLog, Long> {

    /**
     * 컨테이너 히스토리 조회 (페이지네이션)
     * 삭제된 컨테이너(is_deleted = 1)의 히스토리도 조회 가능
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param containerId 컨테이너 ID (null 가능)
     * @param isDeleted 삭제 여부 (null 가능)
     * @param pageable 페이지 정보
     * @return 컨테이너 히스토리 페이지
     */
    @Query(value = """
        SELECT
            csl.collected_at,
            c.name,
            c.container_hash,
            a.agent_name,
            c.image_name,
            csl.state,
            csl.health,
            c.created_at,
            c.is_deleted,

            csl.cpu_percent,
            csl.cpu_core_usage,
            csl.host_cpu_usage_total,
            csl.cpu_usage_total,
            csl.cpu_user,
            csl.cpu_system,
            csl.cpu_quota,
            csl.cpu_period,
            csl.online_cpus,
            csl.throttling_periods,
            csl.throttled_periods,
            csl.throttled_time,
            c.cpu_limit_cores,
            c.is_cpu_unlimited,

            csl.mem_percent,
            csl.mem_usage,
            csl.mem_max_usage,
            c.mem_limit,
            c.is_memory_unlimited,
            c.last_oom_killed_at,

            csl.blk_read,
            csl.blk_write,
            csl.blk_read_per_sec,
            csl.blk_write_per_sec,

            csl.rx_bytes,
            csl.tx_bytes,
            csl.rx_packets,
            csl.tx_packets,
            csl.network_total_bytes,
            csl.rx_bytes_per_sec,
            csl.tx_bytes_per_sec,
            csl.rx_pps,
            csl.tx_pps,
            csl.rx_failure_rate,
            csl.tx_failure_rate,
            csl.rx_errors,
            csl.tx_errors,
            csl.rx_dropped,
            csl.tx_dropped,

            csl.size_rw,
            csl.size_root_fs,
            c.storage_limit,
            c.is_storage_unlimited

        FROM container_stats_logs csl
        INNER JOIN containers c ON csl.container_id = c.id
        INNER JOIN agents a ON c.agent_id = a.id
        WHERE csl.collected_at BETWEEN :startTime AND :endTime
            AND (:containerId IS NULL OR c.id = :containerId)
            AND (:isDeleted IS NULL OR c.is_deleted = :isDeleted)
        ORDER BY csl.collected_at DESC
        """,
        countQuery = """
        SELECT COUNT(*)
        FROM container_stats_logs csl
        INNER JOIN containers c ON csl.container_id = c.id
        INNER JOIN agents a ON c.agent_id = a.id
        WHERE csl.collected_at BETWEEN :startTime AND :endTime
            AND (:containerId IS NULL OR c.id = :containerId)
            AND (:isDeleted IS NULL OR c.is_deleted = :isDeleted)
        """,
        nativeQuery = true)
    Page<Object[]> findContainerHistory(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("containerId") Long containerId,
            @Param("isDeleted") Integer isDeleted,
            Pageable pageable
    );
}