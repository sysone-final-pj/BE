package com.monito.domains.container.repository;

import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.LogSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

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
}
