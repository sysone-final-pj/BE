package com.monito.domains.alert.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.dto.AlertMessageDTO;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.response.AlertResponseDTO;
import com.monito.domains.alert.repository.AlertRepository;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.alert.websocket.handler.AlertWebSocketHandler;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;

import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final AlertWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    /**
     * 알림 생성 및 웹소켓 전송
     */
    @Override
    public void createAndSendAlert(Member member, AlertRule alertRule, Container container,
                                   String message, MetricType metricType, BigDecimal metricValue,
                                   AlertLevel alertLevel) {
        try {
            // 1. DB에 알림 저장
            Alert alert = Alert.builder()
                    .member(member)
                    .alertRule(alertRule)
                    .container(container)
                    .message(message)
                    .metricType(metricType)
                    .metricValue(metricValue)
                    .alertLevel(alertLevel)
                    .isRead(false)
                    .build();

            alertRepository.save(alert);

            // 2. 웹소켓으로 실시간 전송
            AlertMessageDTO.ContainerInfoDTO containerInfo = AlertMessageDTO.ContainerInfoDTO.builder()
                    .containerId(container.getId())
                    .containerName(container.getName())
                    .containerHash(container.getContainerHash())
                    .metricType(metricType.name())
                    .metricValue(metricValue)
                    .build();

            AlertMessageDTO alertMessage = AlertMessageDTO.builder()
                    .alertId(alert.getId())
                    .metricType(metricType.name())
                    .title(alertLevel != null ? alertLevel.getDescription() : "알림")
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .containerInfo(containerInfo)
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(alertMessage);
            webSocketHandler.sendAlertToUser(String.valueOf(member.getId()), jsonMessage);

            log.info("알림 생성 및 전송 완료: memberId={}, containerId={}, alertLevel={}, metricValue={}",
                    member.getId(), container.getId(), alertLevel, metricValue);

        } catch (Exception e) {
            log.error("알림 생성 중 에러 발생: memberId={}, containerId={}",
                    member.getId(), container.getId(), e);
        }
    }

    /**
     * 읽지 않은 알림 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<Alert> getUnreadAlerts(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(memberId);
    }

    /**
     * 모든 알림 조회 (알림 페이지용)
     */
    @Override
    @Transactional(readOnly = true)
    public List<Alert> getAllAlerts(Long memberId) {
        return alertRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId);
    }

    /**
     * 알림 읽음 처리 (본인 알림만 처리 가능)
     */
    @Override
    public void markAsRead(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 읽음 처리할 수 있습니다.");
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
            AlertMessageDTO alertMessage = AlertMessageDTO.builder()
                    .metricType("SYSTEM")
                    .title(title)
                    .message(message)
                    .createdAt(LocalDateTime.now())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(alertMessage);
            webSocketHandler.broadcastAlert(jsonMessage);

            log.info("브로드캐스트 알림 전송: title={}, level={}", title, alertLevel);
        } catch (Exception e) {
            log.error("브로드캐스트 중 에러 발생", e);
        }
    }

    /**
     * 알림 생성 (수동 생성용)
     */
    @Override
    public AlertResponseDTO createAlert(Long memberId, AlertCreateRequestDTO request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        AlertRule alertRule = alertRuleRepository.findById(request.getRuleId())
                .orElseThrow(() -> new IllegalArgumentException("알림 규칙을 찾을 수 없습니다."));

        Container container = containerRepository.findById(request.getContainerId())
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다."));

        Alert alert = Alert.builder()
                .member(member)
                .alertRule(alertRule)
                .container(container)
                .message(request.getMessage())
                .metricType(request.getMetricType())
                .metricValue(request.getMetricValue())
                .alertLevel(request.getAlertLevel())
                .isRead(false)
                .build();

        Alert savedAlert = alertRepository.save(alert);
        log.info("알림 생성 완료: alertId={}, memberId={}", savedAlert.getId(), memberId);

        return AlertResponseDTO.from(savedAlert);
    }

    /**
     * 특정 알림 조회 (본인 알림만 조회 가능)
     */
    @Override
    @Transactional(readOnly = true)
    public AlertResponseDTO getAlert(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 조회할 수 있습니다.");
        }

        return AlertResponseDTO.from(alert);
    }

    /**
     * 사용자의 모든 알림 조회 (DTO 변환)
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDTO> getAllAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 읽지 않은 알림 조회 (DTO 변환)
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertResponseDTO> getUnreadAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 삭제 (본인 알림만 삭제 가능) - Soft Delete
     */
    @Override
    public void deleteAlert(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림만 삭제할 수 있습니다.");
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
        List<Alert> unreadAlerts = alertRepository.findByMemberIdAndIsReadFalseAndIsDeletedFalseOrderByCreatedAtDesc(memberId);

        unreadAlerts.forEach(Alert::markAsRead);
        alertRepository.saveAll(unreadAlerts);

        log.info("모든 알림 읽음 처리 완료: memberId={}, count={}", memberId, unreadAlerts.size());
    }

    /**
     * 사용자의 모든 알림 삭제 (Soft Delete)
     */
    @Override
    public void deleteAllAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId);
        alerts.forEach(Alert::delete);
        alertRepository.saveAll(alerts);

        log.info("모든 알림 삭제 완료: memberId={}, count={}", memberId, alerts.size());
    }

    /**
     * 사용자의 읽은 알림 모두 삭제 (Soft Delete)
     */
    @Override
    public void deleteReadAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdAndIsDeletedFalseOrderByCreatedAtDesc(memberId);
        List<Alert> readAlerts = alerts.stream()
                .filter(Alert::getIsRead)
                .collect(Collectors.toList());

        readAlerts.forEach(Alert::delete);
        alertRepository.saveAll(readAlerts);

        log.info("읽은 알림 삭제 완료: memberId={}, count={}", memberId, readAlerts.size());
    }

    /**
     * WebSocket: 알림 읽음 처리 상태 업데이트 전송
     */
    private void sendReadStatusUpdate(Long memberId, Long alertId, boolean isRead) {
        try {
            AlertMessageDTO statusUpdate = AlertMessageDTO.builder()
                    .alertId(alertId)
                    .metricType("ALERT_READ_STATUS")
                    .title("알림 읽음 처리")
                    .message("알림이 읽음 처리되었습니다.")
                    .createdAt(LocalDateTime.now())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(statusUpdate);
            webSocketHandler.sendAlertToUser(String.valueOf(memberId), jsonMessage);

            log.info("읽음 상태 업데이트 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            log.error("읽음 상태 업데이트 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }

    /**
     * WebSocket: 알림 삭제 알림 전송
     */
    private void sendDeleteNotification(Long memberId, Long alertId) {
        try {
            AlertMessageDTO deleteNotification = AlertMessageDTO.builder()
                    .alertId(alertId)
                    .metricType("ALERT_DELETED")
                    .title("알림 삭제")
                    .message("알림이 삭제되었습니다.")
                    .createdAt(LocalDateTime.now())
                    .build();

            String jsonMessage = objectMapper.writeValueAsString(deleteNotification);
            webSocketHandler.sendAlertToUser(String.valueOf(memberId), jsonMessage);

            log.info("삭제 알림 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            log.error("삭제 알림 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }
}
