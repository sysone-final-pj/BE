package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.container.domain.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
/**
 작성자: 이지민
 */
@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    /**
     * 특정 사용자의 읽지 않은 알림 조회 (최신순)
     */
    @Query("SELECT a FROM Alert a WHERE a.member.id = :memberId AND a.isRead = false AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Alert> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 특정 사용자의 모든 알림 조회 (최신순) - 알림 페이지용
     */
    @Query("SELECT a FROM Alert a WHERE a.member.id = :memberId AND a.isDeleted = false ORDER BY a.createdAt DESC")
    List<Alert> findByMemberIdOrderByCreatedAtDesc(@Param("memberId") Long memberId);

    /**
     * 특정 컨테이너의 알림 조회 (최신순)
     */
    List<Alert> findByContainerIdOrderByCreatedAtDesc(Long containerId);

    /**
     * 특정 사용자의 특정 레벨 읽지 않은 알림 조회
     */
    @Query("SELECT a FROM Alert a WHERE a.member.id = :memberId AND a.alertLevel = :alertLevel AND a.isRead = false AND a.isDeleted = false")
    List<Alert> findByMemberIdAndAlertLevelAndIsReadFalse(@Param("memberId") Long memberId, @Param("alertLevel") AlertLevel alertLevel);

    /**
     * 특정 사용자의 읽지 않은 알림 개수 조회 (뱃지용)
     */
    @Query("SELECT COUNT(a) FROM Alert a WHERE a.member.id = :memberId AND a.isRead = false AND a.isDeleted = false")
    long countByMemberIdAndIsReadFalse(@Param("memberId") Long memberId);

    /**
     * 특정 컨테이너의 최근 알림 조회 (개수 제한)
     */
    List<Alert> findTop10ByContainerIdOrderByCreatedAtDesc(Long containerId);

    /**
     * 필터링된 알림 목록 조회
     * @param memberId 회원 ID
     * @param alertLevel 알림 레벨
     * @param alertLevelEmpty 알림 레벨 필터 사용 여부
     * @param metricType 메트릭 타입
     * @param metricTypeEmpty 메트릭 타입 필터 사용 여부
     * @param agentName 에이전트 이름 (LIKE 검색)
     * @param agentNameEmpty 에이전트 이름 필터 사용 여부
     * @param containerName 컨테이너 이름 (LIKE 검색)
     * @param containerNameEmpty 컨테이너 이름 필터 사용 여부
     * @param collectedAtFrom 수집 시작 시간
     * @param collectedAtFromEmpty 수집 시작 시간 필터 사용 여부
     * @param collectedAtTo 수집 종료 시간
     * @param collectedAtToEmpty 수집 종료 시간 필터 사용 여부
     * @param createdAtFrom 생성 시작 시간
     * @param createdAtFromEmpty 생성 시작 시간 필터 사용 여부
     * @param createdAtTo 생성 종료 시간
     * @param createdAtToEmpty 생성 종료 시간 필터 사용 여부
     * @param isRead 읽음 여부
     * @param isReadEmpty 읽음 여부 필터 사용 여부
     * @return 필터링된 알림 목록
     */
    @Query(value = """
            SELECT a
            FROM Alert a
            JOIN a.container c
            JOIN c.agent ag
            WHERE a.member.id = :memberId
            AND a.isDeleted = false
            AND (:alertLevelEmpty = true OR a.alertLevel = :alertLevel)
            AND (:metricTypeEmpty = true OR a.metricType = :metricType)
            AND (:agentNameEmpty = true OR LOWER(ag.agentName) LIKE LOWER(CONCAT('%', :agentName, '%')))
            AND (:containerNameEmpty = true OR LOWER(c.name) LIKE LOWER(CONCAT('%', :containerName, '%')))
            AND (:collectedAtFromEmpty = true OR a.collectedAt >= :collectedAtFrom)
            AND (:collectedAtToEmpty = true OR a.collectedAt <= :collectedAtTo)
            AND (:createdAtFromEmpty = true OR a.createdAt >= :createdAtFrom)
            AND (:createdAtToEmpty = true OR a.createdAt <= :createdAtTo)
            AND (:isReadEmpty = true OR a.isRead = :isRead)
            """)
    List<Alert> findAlertsWithFilters(
            @Param("memberId") Long memberId,
            @Param("alertLevel") AlertLevel alertLevel,
            @Param("alertLevelEmpty") boolean alertLevelEmpty,
            @Param("metricType") MetricType metricType,
            @Param("metricTypeEmpty") boolean metricTypeEmpty,
            @Param("agentName") String agentName,
            @Param("agentNameEmpty") boolean agentNameEmpty,
            @Param("containerName") String containerName,
            @Param("containerNameEmpty") boolean containerNameEmpty,
            @Param("collectedAtFrom") LocalDateTime collectedAtFrom,
            @Param("collectedAtFromEmpty") boolean collectedAtFromEmpty,
            @Param("collectedAtTo") LocalDateTime collectedAtTo,
            @Param("collectedAtToEmpty") boolean collectedAtToEmpty,
            @Param("createdAtFrom") LocalDateTime createdAtFrom,
            @Param("createdAtFromEmpty") boolean createdAtFromEmpty,
            @Param("createdAtTo") LocalDateTime createdAtTo,
            @Param("createdAtToEmpty") boolean createdAtToEmpty,
            @Param("isRead") Boolean isRead,
            @Param("isReadEmpty") boolean isReadEmpty
    );
}