package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.repository.projection.TimeSeriesDataPoint;
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
     * 특정 컨테이너 해시의 최신 통계 로그 조회 (파티션 프루닝 최적화)
     * @param containerHash 컨테이너 해시
     * @param afterTime 조회 시작 시간 (파티션 프루닝용 - 권장: 1시간 전)
     * @return 최신 ContainerStatsLog
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.containerHash = :containerHash " +
           "AND csl.collectedAt >= :afterTime " +
           "ORDER BY csl.collectedAt DESC " +
           "LIMIT 1")
    Optional<ContainerStatsLog> findLatestByContainerHash(
            @Param("containerHash") String containerHash,
            @Param("afterTime") LocalDateTime afterTime
    );

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

    /**
     * 특정 컨테이너의 최신 통계 로그 조회 (파티션 프루닝 최적화)
     * @param containerId 컨테이너 ID
     * @param afterTime 조회 시작 시간 (파티션 프루닝용 - 권장: 1시간 전)
     * @return 최신 ContainerStatsLog
     */
    @Query("SELECT csl FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt >= :afterTime " +
           "ORDER BY csl.collectedAt DESC " +
           "LIMIT 1")
    Optional<ContainerStatsLog> findLatestByContainerId(
            @Param("containerId") Long containerId,
            @Param("afterTime") LocalDateTime afterTime
    );

    // ==================== 시계열 데이터 전용 최적화 쿼리 (Projection 사용) ====================

    /**
     * CPU 사용률(%) 시계열 데이터 조회 (최적화)
     * - Projection 사용으로 필요한 컬럼만 조회
     * - Covering Index (IDX_CONTAINER_STATS_TIMESERIES) 활용
     * - Index-Only Scan으로 테이블 접근 최소화
     *
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return CPU 사용률 시계열 데이터
     */
    @Query("SELECT csl.collectedAt as collectedAt, csl.cpuPercent as value " +
           "FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<TimeSeriesDataPoint> findCpuUsageTimeSeries(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 메모리 사용량(MB) 시계열 데이터 조회 (최적화)
     * - Projection 사용으로 필요한 컬럼만 조회
     * - Covering Index (IDX_CONTAINER_STATS_TIMESERIES) 활용
     * - bytes를 MB로 변환 (1 MB = 1024 * 1024 bytes)
     *
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 메모리 사용량 시계열 데이터 (단위: MB)
     */
    @Query("SELECT csl.collectedAt as collectedAt, " +
           "CAST(csl.memUsage / 1048576.0 AS java.math.BigDecimal) as value " +
           "FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<TimeSeriesDataPoint> findMemoryUsageTimeSeries(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 네트워크 수신(RX) 속도 시계열 데이터 조회 (최적화)
     * - Projection 사용으로 필요한 컬럼만 조회
     * - Covering Index (IDX_CONTAINER_STATS_TIMESERIES) 활용
     *
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 네트워크 수신 속도 시계열 데이터 (bytes/sec)
     */
    @Query("SELECT csl.collectedAt as collectedAt, " +
           "CAST(csl.rxBytesPerSec AS java.math.BigDecimal) as value " +
           "FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<TimeSeriesDataPoint> findNetworkRxTimeSeries(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 네트워크 송신(TX) 속도 시계열 데이터 조회 (최적화)
     * - Projection 사용으로 필요한 컬럼만 조회
     * - Covering Index (IDX_CONTAINER_STATS_TIMESERIES) 활용
     *
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 네트워크 송신 속도 시계열 데이터 (bytes/sec)
     */
    @Query("SELECT csl.collectedAt as collectedAt, " +
           "CAST(csl.txBytesPerSec AS java.math.BigDecimal) as value " +
           "FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<TimeSeriesDataPoint> findNetworkTxTimeSeries(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 네트워크 패킷 레이트 시계열 데이터 조회 (최적화)
     * - Projection 사용으로 필요한 컬럼만 조회
     * - Covering Index (IDX_CONTAINER_STATS_TIMESERIES) 활용
     * - RX + TX 패킷 레이트 합계
     *
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 네트워크 패킷 레이트 시계열 데이터 (packets/sec)
     */
    @Query("SELECT csl.collectedAt as collectedAt, " +
           "CAST((csl.rxPps + csl.txPps) AS java.math.BigDecimal) as value " +
           "FROM ContainerStatsLog csl " +
           "WHERE csl.container.id = :containerId " +
           "AND csl.collectedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY csl.collectedAt ASC")
    List<TimeSeriesDataPoint> findNetworkPacketsTimeSeries(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
