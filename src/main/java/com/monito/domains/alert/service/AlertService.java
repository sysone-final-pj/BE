package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.response.AlertResponseDTO;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.member.domain.Member;

import java.math.BigDecimal;
import java.util.List;

/**
 * 알림 서비스 인터페이스
 */
public interface AlertService {

    /**
     * 알림 생성 및 웹소켓 전송
     */
    void createAndSendAlert(Member member, AlertRule alertRule, Container container,
                            String message, MetricType metricType, BigDecimal metricValue,
                            AlertLevel alertLevel);

    /**
     * 읽지 않은 알림 조회
     */
    List<Alert> getUnreadAlerts(Long memberId);

    /**
     * 모든 알림 조회 (알림 페이지용)
     */
    List<Alert> getAllAlerts(Long memberId);

    /**
     * 알림 읽음 처리 (본인 알림만 처리 가능)
     */
    void markAsRead(Long alertId, Long memberId);

    /**
     * 모든 사용자에게 브로드캐스트 (관리자용)
     */
    void broadcastAlert(String title, String message, AlertLevel alertLevel);

    /**
     * 알림 생성 (수동 생성용)
     */
    AlertResponseDTO createAlert(Long memberId, AlertCreateRequestDTO request);

    /**
     * 특정 알림 조회 (본인 알림만 조회 가능)
     */
    AlertResponseDTO getAlert(Long alertId, Long memberId);

    /**
     * 사용자의 모든 알림 조회 (DTO 변환)
     */
    List<AlertResponseDTO> getAllAlertsAsResponse(Long memberId);

    /**
     * 사용자의 읽지 않은 알림 조회 (DTO 변환)
     */
    List<AlertResponseDTO> getUnreadAlertsAsResponse(Long memberId);

    /**
     * 알림 삭제 (본인 알림만 삭제 가능) - Soft Delete
     */
    void deleteAlert(Long alertId, Long memberId);

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    void markAllAsRead(Long memberId);

    /**
     * 사용자의 모든 알림 삭제 (Soft Delete)
     */
    void deleteAllAlerts(Long memberId);

    /**
     * 사용자의 읽은 알림 모두 삭제 (Soft Delete)
     */
    void deleteReadAlerts(Long memberId);
}
