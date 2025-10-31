package com.monito.domains.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.dto.internal.AlertCreationDTO;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.response.AlertDetailResponseDTO;
import com.monito.domains.alert.dto.response.AlertListItemResponseDTO;
import com.monito.domains.alert.dto.response.AlertMessageResponseDTO;
import com.monito.domains.alert.dto.response.ContainerInfoResponseDTO;
import com.monito.domains.alert.repository.AlertRepository;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.alert.repository.AlertSpecification;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.ForbiddenException;
import com.monito.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertServiceImpl implements AlertService {

    private final AlertRepository alertRepository;
    private final AlertRuleRepository alertRuleRepository;
    private final MemberRepository memberRepository;
    private final ContainerRepository containerRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 알림 생성 및 웹소켓 전송
     */
    @Override
    public void createAndSendAlert(AlertCreationDTO dto) {
        try {
            // 1. DTO를 엔티티로 변환 후 DB에 저장
            Alert alert = dto.toEntity();
            alertRepository.save(alert);

            // 2. 웹소켓으로 실시간 전송
            ContainerInfoResponseDTO containerInfo = ContainerInfoResponseDTO.builder()
                    .containerId(dto.getContainer().getId())
                    .containerName(dto.getContainer().getName())
                    .containerHash(dto.getContainer().getContainerHash())
                    .metricType(dto.getMetricType().name())
                    .metricValue(dto.getMetricValue())
                    .build();

            AlertMessageResponseDTO alertMessage = AlertMessageResponseDTO.builder()
                    .alertId(alert.getId())
                    .metricType(dto.getMetricType().name())
                    .title(dto.getAlertLevel() != null ? dto.getAlertLevel().getDescription() : "알림")
                    .message(dto.getMessage())
                    .createdAt(LocalDateTime.now())
                    .containerInfo(containerInfo)
                    .build();

            // STOMP를 통한 사용자별 알림 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.getMember().getId()),
                    "/queue/alerts",
                    alertMessage
            );

            log.info("알림 생성 및 전송 완료: memberId={}, containerId={}, alertLevel={}, metricValue={}",
                    dto.getMember().getId(), dto.getContainer().getId(), dto.getAlertLevel(), dto.getMetricValue());

        } catch (Exception e) {
            log.error("알림 생성 중 에러 발생: memberId={}, containerId={}",
                    dto.getMember().getId(), dto.getContainer().getId(), e);
        }
    }

    /**
     * 읽지 않은 알림 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<Alert> getUnreadAlerts(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
    }

    /**
     * 모든 알림 조회 (알림 페이지용)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAllAlerts(Long memberId) {
        return alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    /**
     * 알림 읽음 처리 (본인 알림만 처리 가능)
     */
    @Override
    public void markAsRead(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_NOT_FOUND));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_READ_ACCESS_DENIED);
        }

        alert.markAsRead();
        alertRepository.save(alert);

        // 실시간 읽음 처리 알림 전송
        sendReadStatusUpdate(memberId, alertId, true);

        log.info("알림 읽음 처리 완료: alertId={}, memberId={}", alertId, memberId);
    }

    /**
     * 모든 사용자에게 브로드캐스트 (관리자용)
     */
    @Override
    public void broadcastAlert(String title, String message, AlertLevel alertLevel) {
        try {
            AlertMessageResponseDTO alertMessage = AlertMessageResponseDTO.builder()
                    .metricType("SYSTEM")
                    .title(title)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            // STOMP를 통한 브로드캐스트 (/topic/alerts 구독자 전체에게 전송)
            messagingTemplate.convertAndSend("/topic/alerts", alertMessage);

            log.info("브로드캐스트 알림 전송: title={}, level={}", title, alertLevel);
        } catch (Exception e) {
            log.error("브로드캐스트 중 에러 발생", e);
        }
    }

    /**
     * 알림 생성 (수동 생성용)
     */
    @Override
    public AlertDetailResponseDTO createAlert(Long memberId, AlertCreateRequestDTO request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));

        AlertRule alertRule = alertRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        Container container = containerRepository.findById(request.getContainerId())
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.CONTAINER_NOT_FOUND));

        Alert alert = Alert.builder()
                .member(member)
                .alertRule(alertRule)
                .container(container)
                .message(request.getMessage())
                .metricType(request.getMetricType())
                .metricValue(request.getMetricValue())
                .alertLevel(request.getAlertLevel())
                .collectedAt(request.getCollectedAt())
                .isRead(false)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("알림 생성 완료: alertId={}, memberId={}", savedAlert.getId(), memberId);

        return AlertDetailResponseDTO.from(savedAlert);
    }

    /**
     * 특정 알림 조회 (본인 알림만 조회 가능)
     */
    @Override
    @Transactional(readOnly = true)
    public AlertDetailResponseDTO getAlert(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_NOT_FOUND));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_VIEW_ACCESS_DENIED);
        }

        return AlertDetailResponseDTO.from(alert);
    }

    /**
     * 사용자의 모든 알림 목록 조회 (DTO 변환)
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertListItemResponseDTO> getAllAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertListItemResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 읽지 않은 알림 목록 조회 (DTO 변환)
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertListItemResponseDTO> getUnreadAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertListItemResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 삭제 (본인 알림만 삭제 가능) - Soft Delete
     */
    @Override
    public void deleteAlert(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_NOT_FOUND));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_DELETE_ACCESS_DENIED);
        }

        alert.delete();
        alertRepository.save(alert);

        // 실시간 삭제 알림 전송
        sendDeleteNotification(memberId, alertId);

        log.info("알림 삭제 완료: alertId={}, memberId={}", alertId, memberId);
    }

    /**
     * 사용자의 모든 알림 읽음 처리
     */
    @Override
    public void markAllAsRead(Long memberId) {
        List<Alert> unreadAlerts = alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);

        unreadAlerts.forEach(Alert::markAsRead);
        alertRepository.saveAll(unreadAlerts);

        log.info("모든 알림 읽음 처리 완료: memberId={}, count={}", memberId, unreadAlerts.size());
    }

    /**
     * 사용자의 모든 알림 삭제 (Soft Delete)
     */
    @Override
    public void deleteAllAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        alerts.forEach(Alert::delete);
        alertRepository.saveAll(alerts);

        log.info("모든 알림 삭제 완료: memberId={}, count={}", memberId, alerts.size());
    }

    /**
     * 사용자의 읽은 알림 모두 삭제 (Soft Delete)
     */
    @Override
    public void deleteReadAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        List<Alert> readAlerts = alerts.stream()
                .filter(Alert::getIsRead)
                .collect(Collectors.toList());

        readAlerts.forEach(Alert::delete);
        alertRepository.saveAll(readAlerts);

        log.info("읽은 알림 삭제 완료: memberId={}, count={}", memberId, readAlerts.size());
    }

    /**
     * 읽지 않은 알림 개수 조회 (배지용)
     */
    @Override
    @Transactional(readOnly = true)
    public long getUnreadAlertCount(Long memberId) {
        return alertRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * STOMP: 알림 읽음 처리 상태 업데이트 전송
     */
    private void sendReadStatusUpdate(Long memberId, Long alertId, boolean isRead) {
        try {
            AlertMessageResponseDTO statusUpdate = AlertMessageResponseDTO.builder()
                    .alertId(alertId)
                    .metricType("ALERT_READ_STATUS")
                    .title("알림 읽음 처리")
                    .message("알림이 읽음 처리되었습니다.")
                    .createdAt(LocalDateTime.now())
                    .build();

            // STOMP를 통한 사용자별 상태 업데이트 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(memberId),
                    "/queue/alerts",
                    statusUpdate
            );

            log.info("읽음 상태 업데이트 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            log.error("읽음 상태 업데이트 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }

    /**
     * STOMP: 알림 삭제 알림 전송
     */
    private void sendDeleteNotification(Long memberId, Long alertId) {
        try {
            AlertMessageResponseDTO deleteNotification = AlertMessageResponseDTO.builder()
                    .alertId(alertId)
                    .metricType("ALERT_DELETED")
                    .title("알림 삭제")
                    .message("알림이 삭제되었습니다.")
                    .createdAt(LocalDateTime.now())
                    .build();

            // STOMP를 통한 사용자별 삭제 알림 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(memberId),
                    "/queue/alerts",
                    deleteNotification
            );

            log.info("삭제 알림 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            log.error("삭제 알림 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }

    /**
     * 필터 조건에 따른 알림 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertListItemResponseDTO> getAlertsWithFilter(Long memberId, AlertFilterDTO filter) {
        return alertRepository.findAll(AlertSpecification.withFilter(memberId, filter))
                .stream()
                .map(AlertListItemResponseDTO::from)
                .collect(Collectors.toList());
    }
}
