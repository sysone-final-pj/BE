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
     * 초기 로드: 특정 컨테이너의 특정 기간 내 최신 로그 조회
     * @param containerId 컨테이너 ID
     * @param startTime 시작 시간
     * @param endTime 종료 시간
     * @param pageable 페이지 정보 (size만 사용)
     * @return 로그 목록
     */
    @Query("SELECT cl FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND cl.loggedAt BETWEEN :startTime AND :endTime " +
           "ORDER BY cl.loggedAt DESC, cl.id DESC")
    List<ContainerLog> findInitialLogs(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    /**
     * 초기 로드 + LogSource 필터
     */
    @Query("SELECT cl FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND cl.loggedAt BETWEEN :startTime AND :endTime " +
           "AND cl.source = :logSource " +
           "ORDER BY cl.loggedAt DESC, cl.id DESC")
    List<ContainerLog> findInitialLogsWithSource(
            @Param("containerId") Long containerId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("logSource") LogSource logSource,
            Pageable pageable
    );

    /**
     * 커서 기반: 이전 로그 조회 (무한 스크롤)
     * - lastLoggedAt 이전 시간의 로그
     * - lastLoggedAt과 같은 시간이면 lastLogId보다 작은 ID
     */
    @Query("SELECT cl FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND (cl.loggedAt < :lastLoggedAt " +
           "     OR (cl.loggedAt = :lastLoggedAt AND cl.id < :lastLogId)) " +
           "ORDER BY cl.loggedAt DESC, cl.id DESC")
    List<ContainerLog> findLogsAfterCursor(
            @Param("containerId") Long containerId,
            @Param("lastLogId") Long lastLogId,
            @Param("lastLoggedAt") LocalDateTime lastLoggedAt,
            Pageable pageable
    );

    /**
     * 커서 기반 + LogSource 필터
     */
    @Query("SELECT cl FROM ContainerLog cl " +
           "WHERE cl.container.id = :containerId " +
           "AND (cl.loggedAt < :lastLoggedAt " +
           "     OR (cl.loggedAt = :lastLoggedAt AND cl.id < :lastLogId)) " +
           "AND cl.source = :logSource " +
           "ORDER BY cl.loggedAt DESC, cl.id DESC")
    List<ContainerLog> findLogsAfterCursorWithSource(
            @Param("containerId") Long containerId,
            @Param("lastLogId") Long lastLogId,
            @Param("lastLoggedAt") LocalDateTime lastLoggedAt,
            @Param("logSource") LogSource logSource,
            Pageable pageable
    );
}
