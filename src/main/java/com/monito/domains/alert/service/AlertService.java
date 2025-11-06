package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.dto.internal.AlertCreationDTO;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.response.AlertDetailResponseDTO;
import com.monito.domains.alert.dto.response.AlertListItemResponseDTO;

import java.util.List;

public interface AlertService {

    /**
     * 알림 생성 및 웹소켓 전송
     */
    void createAndSendAlert(AlertCreationDTO dto);

    /**
     * 읽지 않은 알림 조회
     */
    List<Alert> getUnreadAlerts(Long memberId);

    /**
     * 모든 알림 조회 (알림 페이지용)
     */
    List<Alert> getAllAlerts(Long memberId);

    /**
     * 알림 읽음 처리
     */
    void markAsRead(Long alertId, Long memberId);

    /**
     * 모든 사용자에게 브로드캐스트 (관리자용)
     */
    void broadcastAlert(String title, String message, AlertLevel alertLevel);

    /**
     * 알림 생성 (수동)
     */
    AlertDetailResponseDTO createAlert(Long memberId, AlertCreateRequestDTO request);

    /**
     * 특정 알림 조회
     */
    AlertDetailResponseDTO getAlert(Long alertId, Long memberId);

    /**
     * 사용자의 모든 알림 목록 조회 (DTO 변환)
     */
    List<AlertListItemResponseDTO> getAllAlertsAsResponse(Long memberId);

    /**
     * 사용자의 읽지 않은 알림 목록 조회 (DTO 변환)
     */
    List<AlertListItemResponseDTO> getUnreadAlertsAsResponse(Long memberId);

    /**
     * 알림 삭제
     */
    void deleteAlert(Long alertId, Long memberId);

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    void markAllAsRead(Long memberId);

    /**
     * 사용자의 모든 알림 삭제
     */
    void deleteAllAlerts(Long memberId);

    /**
     * 사용자의 읽은 알림 모두 삭제
     */
    void deleteReadAlerts(Long memberId);

    /**
     * 읽지 않은 알림 개수 조회 (배지용)
     */
    long getUnreadAlertCount(Long memberId);

    /**
     * 필터 조건에 따른 알림 조회
     */
    List<AlertListItemResponseDTO> getAlertsWithFilter(Long memberId, AlertFilterDTO filter);
}
