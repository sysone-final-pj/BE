package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.dto.projection.ContainerLogProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
/**
 작성자: 백승준
 */
@Repository
public interface ContainerLogRepository extends JpaRepository<ContainerLog, Long> {

    /**
     * 통합 로그 조회 메서드 (초기 로드 + 무한 스크롤 + 다중 컨테이너 지원)
     *
     * @param containerIds 컨테이너 ID 리스트 (null이면 모든 컨테이너, 단일/다중 모두 지원)
     * @param logSource 로그 소스 필터 (null이면 필터링 안함)
     * @param agentName Agent 이름 필터 (null이면 필터링 안함)
     * @param lastLogId 커서 - 마지막 로그 ID (null이면 초기 로드)
     * @param lastLoggedAt 커서 - 마지막 로그 시간 (null이면 초기 로드)
     * @param startTime 시작 시간 (초기 로드 시 사용, null 가능)
     * @param endTime 종료 시간 (초기 로드 시 사용, null 가능)
     * @param pageable 페이지 정보 (size와 sort 사용)
     * @return 로그 목록
     */
    @Query("SELECT cl FROM ContainerLog cl " +
           "LEFT JOIN FETCH cl.container c " +
           "LEFT JOIN FETCH c.agent a " +
           "WHERE (:containerIds IS NULL OR cl.container.id IN :containerIds) " +
           "AND (:logSource IS NULL OR cl.source = :logSource) " +
           "AND (:agentName IS NULL OR a.agentName LIKE CONCAT('%', :agentName, '%')) " +
           "AND (:lastLoggedAt IS NULL OR " +
           "     cl.loggedAt < :lastLoggedAt OR " +
           "     (cl.loggedAt = :lastLoggedAt AND cl.id < :lastLogId)) " +
           "AND (:startTime IS NULL OR cl.loggedAt >= :startTime) " +
           "AND (:endTime IS NULL OR cl.loggedAt <= :endTime)")
    List<ContainerLog> findLogs(
            @Param("containerIds") List<Long> containerIds,
            @Param("logSource") LogSource logSource,
            @Param("agentName") String agentName,
            @Param("lastLogId") Long lastLogId,
            @Param("lastLoggedAt") LocalDateTime lastLoggedAt,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 로그 조회 (Projection, CLOB 제외) - 성능 최적화
     * - CLOB 전체 대신 처음 500자만 조회
     * - 10초 → 0.5초 성능 개선
     * - Native Query 사용 (CLOB 처리)
     */
    @Query(value = "SELECT " +
           "cl.id AS id, " +
           "c.id AS containerId, " +
           "c.container_hash AS containerHash, " +
           "c.name AS containerName, " +
           "a.id AS agentId, " +
           "a.agent_name AS agentName, " +
           "DBMS_LOB.SUBSTR(cl.log_message, 500, 1) AS logMessagePreview, " +
           "cl.source AS source, " +
           "cl.logged_at AS loggedAt, " +
           "cl.created_at AS createdAt " +
           "FROM container_logs cl " +
           "JOIN containers c ON cl.container_id = c.id " +
           "JOIN agents a ON c.agent_id = a.id " +
           "WHERE (COALESCE(:containerIdsSize, 0) = 0 OR c.id IN :containerIds) " +
           "AND (:logSource IS NULL OR cl.source = :logSource) " +
           "AND (:agentName IS NULL OR a.agent_name LIKE '%' || :agentName || '%') " +
           "AND (:lastLoggedAt IS NULL OR " +
           "     cl.logged_at < :lastLoggedAt OR " +
           "     (cl.logged_at = :lastLoggedAt AND cl.id < :lastLogId)) " +
           "AND (:startTime IS NULL OR cl.logged_at >= :startTime) " +
           "AND (:endTime IS NULL OR cl.logged_at <= :endTime) " +
           "ORDER BY cl.logged_at DESC, cl.id DESC",
           nativeQuery = true)
    List<Object[]> findLogsOptimizedNative(
            @Param("containerIds") List<Long> containerIds,
            @Param("containerIdsSize") Integer containerIdsSize,
            @Param("logSource") String logSource,
            @Param("agentName") String agentName,
            @Param("lastLogId") Long lastLogId,
            @Param("lastLoggedAt") LocalDateTime lastLoggedAt,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 특정 기간 동안 특정 소스의 로그 개수 카운트
     *
     * @param source 로그 소스 (STDOUT, STDERR 등)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 로그 개수
     */
    @Query("SELECT COUNT(cl) FROM ContainerLog cl " +
           "WHERE cl.source = :source " +
           "AND cl.loggedAt >= :startTime " +
           "AND cl.loggedAt < :endTime")
    long countBySourceAndLoggedAtBetween(
            @Param("source") LogSource source,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 컨테이너의 특정 기간 동안 특정 소스의 로그 개수 카운트
     *
     * @param containerId 컨테이너 ID
     * @param source 로그 소스 (STDOUT, STDERR 등)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 로그 개수
     */
    @Query("SELECT COUNT(cl) FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND cl.source = :source " +
           "AND cl.loggedAt >= :startTime " +
           "AND cl.loggedAt < :endTime")
    long countByContainerIdAndSourceAndLoggedAtBetween(
            @Param("containerId") Long containerId,
            @Param("source") LogSource source,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 특정 컨테이너의 특정 기간 동안 특정 소스의 로그 개수 카운트 (createdAt 기준)
     *
     * @param containerId 컨테이너 ID
     * @param source 로그 소스 (STDOUT, STDERR 등)
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @return 로그 개수
     */
    @Query("SELECT COUNT(cl) FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND cl.source = :source " +
           "AND cl.createdAt >= :startTime " +
           "AND cl.createdAt < :endTime")
    long countByContainerIdAndSourceAndCreatedAtBetween(
            @Param("containerId") Long containerId,
            @Param("source") LogSource source,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
