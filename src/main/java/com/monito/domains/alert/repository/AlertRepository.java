package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    // 특정 사용자의 읽지 않은 알림 조회 (최신순)
    List<Alert> findByMemberIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(Long memberId);

    // 특정 사용자의 모든 알림 조회 (최신순) - 알림 페이지용
    List<Alert> findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(Long memberId);

    // 특정 컨테이너의 알림 조회 (최신순)
    List<Alert> findByContainerIdAndIsDeletedFalseOrderByCreatedAtDesc(Long containerId);

    // 특정 사용자의 특정 레벨 읽지 않은 알림 조회
    List<Alert> findByMemberIdAndAlertLevelAndIsReadFalseAndIsDeletedFalse(
            Long memberId, AlertLevel alertLevel);

    // 특정 사용자의 읽지 않은 알림 개수 조회 (뱃지용)
    long countByMemberIdAndIsReadFalseAndIsDeletedFalse(Long memberId);

    // 특정 컨테이너의 최근 알림 조회 (개수 제한)
    List<Alert> findTop10ByContainerIdAndIsDeletedFalseOrderByCreatedAtDesc(Long containerId);
}