package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long>, JpaSpecificationExecutor<Alert> {
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
}