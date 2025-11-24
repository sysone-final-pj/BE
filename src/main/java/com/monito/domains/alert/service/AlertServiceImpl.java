package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.dto.internal.AlertCreationDTO;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.response.AlertDetailResponseDTO;
import com.monito.domains.alert.dto.response.AlertListItemResponseDTO;
import com.monito.domains.alert.dto.response.AlertMessageResponseDTO;
import com.monito.domains.alert.dto.response.ContainerInfoResponseDTO;
import com.monito.domains.alert.repository.AlertRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.ForbiddenException;
import com.monito.global.exception.NotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import com.monito.domains.alert.dto.request.AlertSortType;

/**
 * 알림(Alert) 서비스 구현체
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>알림 생성 및 실시간 전송 (Business Logic Layer)</li>
 *   <li>알림 조회, 읽음 처리, 삭제 등 CRUD 작업</li>
 *   <li>WebSocket(STOMP)을 통한 실시간 알림 브로드캐스팅</li>
 *   <li>필터링 및 정렬 기능 제공</li>
 * </ul>
 *
 * <p><b>주요 패턴:</b></p>
 * <ul>
 *   <li>Service 패턴: 비즈니스 로직을 캡슐화하여 재사용성 향상</li>
 *   <li>DTO 변환 패턴: Entity와 DTO 분리로 계층 간 결합도 감소</li>
 *   <li>Soft Delete 패턴: 물리적 삭제 대신 논리적 삭제로 데이터 보존</li>
 *   <li>권한 검증 패턴: 본인 소유 데이터만 접근 가능하도록 제어</li>
 *   <li>WebSocket 메시징: STOMP 프로토콜로 사용자별 실시간 알림 전송</li>
 * </ul>
 *
 * <p><b>보안 고려사항:</b></p>
 * <ul>
 *   <li>모든 작업에서 memberId 기반 권한 검증</li>
 *   <li>다른 사용자의 알림 조회/수정/삭제 방지</li>
 * </ul>
 */
@Slf4j
// Lombok: Logger 인스턴스 자동 생성
// - log.info(), log.error() 등으로 로깅 가능
// - SLF4J 추상화 레이어 사용 (Logback, Log4j2 등 구현체 교체 가능)

@Service
// Spring의 서비스 컴포넌트로 등록
// - @Component의 특수화된 형태로, 비즈니스 로직 계층임을 명시
// - 컴포넌트 스캔으로 자동 빈 등록
// 장점:
// - 명확한 계층 구분 (Controller -> Service -> Repository)
// - AOP 적용 가능 (트랜잭션, 로깅 등)
// - 테스트 시 Mock 객체로 교체 가능

@RequiredArgsConstructor
// Lombok: final 필드에 대한 생성자 자동 생성 (생성자 주입 패턴)
// 장점:
// - 불변성 보장: final 필드는 초기화 후 변경 불가
// - 순환 참조 방지: 생성자 주입은 순환 참조 시 컴파일 에러 발생
// - 테스트 용이성: Mock 객체 주입 간편
// - NPE 방지: Spring이 빈 생성 시 의존성 자동 주입

@Transactional
// 클래스 레벨 트랜잭션 설정: 모든 public 메서드를 트랜잭션으로 실행
// - 기본값 readOnly=false: 쓰기 작업 허용
// - 읽기 전용 메서드는 @Transactional(readOnly=true)로 오버라이드
// 장점:
// - ACID 보장: 원자성, 일관성, 격리성, 영속성
// - 자동 롤백: RuntimeException 발생 시 자동 롤백
// - Dirty Checking: 영속 상태 엔티티 변경 시 자동 UPDATE 쿼리
// - 예외 전파: 트랜잭션 경계 내에서 예외 통합 관리
public class AlertServiceImpl implements AlertService {

    /**
     * 알림 저장소
     * <p>알림 데이터(Alert) 조회 및 저장을 담당</p>
     */
    private final AlertRepository alertRepository;

    /**
     * WebSocket 메시징 템플릿
     * <p>STOMP 프로토콜로 사용자별 실시간 알림 전송</p>
     * <p>convertAndSendToUser(): 특정 사용자에게만 메시지 전송</p>
     */
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 알림 생성 및 웹소켓 전송
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>AlertCreationDTO를 Alert 엔티티로 변환 후 DB 저장</li>
     *   <li>컨테이너 정보를 포함한 AlertMessageResponseDTO 생성</li>
     *   <li>STOMP를 통해 해당 사용자에게 실시간 알림 전송</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>예외 처리:</b> try-catch로 알림 전송 실패 시에도 서비스 정상 작동</li>
     *   <li><b>사용자별 전송:</b> convertAndSendToUser()로 특정 사용자에게만 전송</li>
     *   <li><b>Builder 패턴:</b> DTO 객체 생성 시 가독성 향상</li>
     * </ul>
     *
     * @param dto 알림 생성 정보 (회원, 컨테이너, 메트릭, 알림 레벨 등)
     */
    @Override
    public void createAndSendAlert(AlertCreationDTO dto) {
        try {
            // 1. DTO를 엔티티로 변환 후 DB에 저장
            Alert alert = dto.toEntity();
            alertRepository.save(alert);

            // 2. 웹소켓으로 실시간 전송
            // ContainerInfoResponseDTO: 알림과 관련된 컨테이너 정보
            ContainerInfoResponseDTO containerInfo = ContainerInfoResponseDTO.builder()
                    .containerId(dto.getContainer().getId())
                    .containerName(dto.getContainer().getName())
                    .containerHash(dto.getContainer().getContainerHash())
                    .metricType(dto.getMetricType().name())
                    .metricValue(dto.getMetricValue())
                    .build();

            // AlertMessageResponseDTO: 클라이언트로 전송할 알림 메시지
            AlertMessageResponseDTO alertMessage = AlertMessageResponseDTO.builder()
                    .alertId(alert.getId())
                    .agentName(dto.getContainer().getAgent().getAgentName())
                    .metricType(dto.getMetricType().name())
                    .title(dto.getAlertLevel() != null ? dto.getAlertLevel().getDescription() : "알림")
                    .message(dto.getMessage())
                    .createdAt(LocalDateTime.now())
                    .containerInfo(containerInfo)
                    .build();

            // STOMP를 통한 사용자별 알림 전송
            // - convertAndSendToUser(): 특정 사용자에게만 메시지 전송
            // - 첫 번째 인자: 사용자 ID (String)
            // - 두 번째 인자: 목적지 경로 (/queue/alerts)
            // - 세 번째 인자: 전송할 메시지 객체
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(dto.getMember().getId()),
                    "/queue/alerts",
                    alertMessage
            );

            log.info("알림 생성 및 전송 완료: agentId={}, memberId={}, containerId={}, alertLevel={}, metricValue={}",
                    dto.getContainer().getAgent().getAgentName(), dto.getMember().getId(), dto.getContainer().getId(), dto.getAlertLevel(), dto.getMetricValue());

        } catch (Exception e) {
            log.error("알림 생성 중 에러 발생: memberId={}, containerId={}",
                    dto.getMember().getId(), dto.getContainer().getId(), e);
        }
    }

    /**
     * 읽지 않은 알림 조회
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 목록 (생성 시간 내림차순)
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<Alert> getUnreadAlerts(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);
    }

    /**
     * 모든 알림 조회 (알림 페이지용)
     *
     * @param memberId 회원 ID
     * @return 모든 알림 목록 (생성 시간 내림차순)
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<Alert> getAllAlerts(Long memberId) {
        return alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
    }

    /**
     * 알림 읽음 처리 (본인 알림만 처리 가능)
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>알림 조회 (없으면 NotFoundException)</li>
     *   <li>권한 검증: 본인 알림인지 확인 (아니면 ForbiddenException)</li>
     *   <li>읽음 상태로 변경 (Dirty Checking으로 자동 UPDATE)</li>
     *   <li>실시간 읽음 처리 알림 전송 (WebSocket)</li>
     * </ol>
     *
     * <p><b>보안:</b> 다른 사용자의 알림 읽음 처리 방지</p>
     *
     * @param alertId 알림 ID
     * @param memberId 회원 ID
     * @throws NotFoundException 알림이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    public void markAsRead(Long alertId, Long memberId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_NOT_FOUND));

        if (!alert.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_READ_ACCESS_DENIED);
        }

        // Dirty Checking: 엔티티 상태 변경 시 자동 UPDATE
        alert.markAsRead();
        alertRepository.save(alert);

        // 실시간 읽음 처리 알림 전송 (WebSocket)
        sendReadStatusUpdate(memberId, alertId, true);

        log.info("알림 읽음 처리 완료: alertId={}, memberId={}", alertId, memberId);
    }

    /**
     * 특정 알림 조회 (본인 알림만 조회 가능)
     *
     * <p><b>권한 검증:</b> 본인 소유 알림만 조회 가능</p>
     *
     * @param alertId 알림 ID
     * @param memberId 회원 ID
     * @return 알림 상세 정보 DTO
     * @throws NotFoundException 알림이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
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
     *
     * <p><b>Stream API:</b> 엔티티를 DTO로 변환하여 반환</p>
     *
     * @param memberId 회원 ID
     * @return 알림 목록 DTO (생성 시간 내림차순)
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<AlertListItemResponseDTO> getAllAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertListItemResponseDTO::from) // 메서드 레퍼런스로 DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 사용자의 읽지 않은 알림 목록 조회 (DTO 변환)
     *
     * <p><b>Stream API:</b> 엔티티를 DTO로 변환하여 반환</p>
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 목록 DTO (생성 시간 내림차순)
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<AlertListItemResponseDTO> getUnreadAlertsAsResponse(Long memberId) {
        return alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlertListItemResponseDTO::from) // 메서드 레퍼런스로 DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 알림 삭제 (본인 알림만 삭제 가능) - Soft Delete
     *
     * <p><b>Soft Delete 패턴:</b></p>
     * <ul>
     *   <li>물리적 삭제 대신 isDeleted 플래그를 true로 설정</li>
     *   <li>데이터 복구 가능, 감사(Audit) 기록 유지</li>
     *   <li>실시간 삭제 알림 전송 (WebSocket)</li>
     * </ul>
     *
     * @param alertId 알림 ID
     * @param memberId 회원 ID
     * @throws NotFoundException 알림이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
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
     *
     * <p><b>일괄 처리:</b> forEach()로 모든 알림의 상태를 일괄 변경</p>
     *
     * @param memberId 회원 ID
     */
    @Override
    public void markAllAsRead(Long memberId) {
        List<Alert> unreadAlerts = alertRepository.findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId);

        // forEach(): 각 알림의 읽음 상태 변경
        unreadAlerts.forEach(Alert::markAsRead);
        // saveAll(): 변경된 엔티티들을 일괄 저장 (성능 최적화)
        alertRepository.saveAll(unreadAlerts);

        log.info("모든 알림 읽음 처리 완료: memberId={}, count={}", memberId, unreadAlerts.size());
    }

    /**
     * 사용자의 모든 알림 삭제 (Soft Delete)
     *
     * <p><b>Soft Delete:</b> 물리적 삭제 대신 isDeleted 플래그 설정</p>
     *
     * @param memberId 회원 ID
     */
    @Override
    public void deleteAllAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        // forEach(): 각 알림의 삭제 플래그 설정
        alerts.forEach(Alert::delete);
        // saveAll(): 변경된 엔티티들을 일괄 저장
        alertRepository.saveAll(alerts);

        log.info("모든 알림 삭제 완료: memberId={}, count={}", memberId, alerts.size());
    }

    /**
     * 사용자의 읽은 알림 모두 삭제 (Soft Delete)
     *
     * <p><b>필터링:</b> Stream API로 읽은 알림만 선택하여 삭제</p>
     *
     * @param memberId 회원 ID
     */
    @Override
    public void deleteReadAlerts(Long memberId) {
        List<Alert> alerts = alertRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        // filter(): 읽은 알림만 필터링
        List<Alert> readAlerts = alerts.stream()
                .filter(Alert::getIsRead)
                .collect(Collectors.toList());

        // forEach(): 각 알림의 삭제 플래그 설정
        readAlerts.forEach(Alert::delete);
        // saveAll(): 변경된 엔티티들을 일괄 저장
        alertRepository.saveAll(readAlerts);

        log.info("읽은 알림 삭제 완료: memberId={}, count={}", memberId, readAlerts.size());
    }

    /**
     * 읽지 않은 알림 개수 조회 (배지용)
     *
     * <p><b>카운트 쿼리:</b> COUNT(*) 사용으로 성능 최적화</p>
     *
     * @param memberId 회원 ID
     * @return 읽지 않은 알림 개수
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public long getUnreadAlertCount(Long memberId) {
        return alertRepository.countByMemberIdAndIsReadFalse(memberId);
    }

    /**
     * STOMP: 알림 읽음 처리 상태 업데이트 전송
     *
     * <p><b>WebSocket 실시간 통신:</b></p>
     * <ul>
     *   <li>알림 읽음 처리 시 클라이언트에게 실시간 알림</li>
     *   <li>UI 자동 업데이트 (읽음 배지, 알림 목록 등)</li>
     * </ul>
     *
     * @param memberId 회원 ID
     * @param alertId 알림 ID
     * @param isRead 읽음 상태
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
            // convertAndSendToUser(): 특정 사용자에게만 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(memberId),
                    "/queue/alerts",
                    statusUpdate
            );

            log.info("읽음 상태 업데이트 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            // WebSocket 전송 실패는 치명적이지 않으므로 로그만 기록
            log.error("읽음 상태 업데이트 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }

    /**
     * STOMP: 알림 삭제 알림 전송
     *
     * <p><b>WebSocket 실시간 통신:</b></p>
     * <ul>
     *   <li>알림 삭제 시 클라이언트에게 실시간 알림</li>
     *   <li>UI 자동 업데이트 (알림 목록에서 제거)</li>
     * </ul>
     *
     * @param memberId 회원 ID
     * @param alertId 알림 ID
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
            // convertAndSendToUser(): 특정 사용자에게만 메시지 전송
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(memberId),
                    "/queue/alerts",
                    deleteNotification
            );

            log.info("삭제 알림 전송: memberId={}, alertId={}", memberId, alertId);
        } catch (Exception e) {
            // WebSocket 전송 실패는 치명적이지 않으므로 로그만 기록
            log.error("삭제 알림 전송 실패: memberId={}, alertId={}", memberId, alertId, e);
        }
    }

    /**
     * 필터 조건에 따른 알림 조회
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>QuickRangeType이 있으면 실제 날짜 범위로 변환</li>
     *   <li>Repository에서 동적 쿼리로 필터링된 알림 조회</li>
     *   <li>정렬 조건 적용 (sortAlerts 메서드)</li>
     *   <li>엔티티를 DTO로 변환하여 반환</li>
     * </ol>
     *
     * <p><b>핵심 패턴:</b></p>
     * <ul>
     *   <li><b>동적 쿼리:</b> null 체크 플래그로 선택적 필터 적용</li>
     *   <li><b>정렬:</b> AlertSortType enum으로 다양한 정렬 기준 지원</li>
     * </ul>
     *
     * @param memberId 회원 ID
     * @param filter 필터 조건 (알림 레벨, 메트릭 타입, 날짜 범위 등)
     * @return 필터링 및 정렬된 알림 목록 DTO
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<AlertListItemResponseDTO> getAlertsWithFilter(Long memberId, AlertFilterDTO filter) {
        // QuickRangeType이 있으면 실제 날짜 범위로 변환
        // (예: LAST_1_HOUR -> collectedAtFrom/To 계산)
        AlertFilterDTO processedFilter = processQuickRangeType(filter);

        // 필터가 null이면 빈 필터로 처리 (모든 알림 조회)
        if (processedFilter == null) {
            processedFilter = AlertFilterDTO.builder().build();
        }

        // 필터링된 알림 조회
        // Repository의 동적 쿼리: null 체크 플래그로 선택적 필터 적용
        List<Alert> alerts = alertRepository.findAlertsWithFilters(
                memberId,
                processedFilter.getAlertLevel(),
                processedFilter.getAlertLevel() == null,
                processedFilter.getMetricType(),
                processedFilter.getMetricType() == null,
                processedFilter.getAgentName(),
                processedFilter.getAgentName() == null || processedFilter.getAgentName().isBlank(),
                processedFilter.getContainerName(),
                processedFilter.getContainerName() == null || processedFilter.getContainerName().isBlank(),
                processedFilter.getCollectedAtFrom(),
                processedFilter.getCollectedAtFrom() == null,
                processedFilter.getCollectedAtTo(),
                processedFilter.getCollectedAtTo() == null,
                processedFilter.getCreatedAtFrom(),
                processedFilter.getCreatedAtFrom() == null,
                processedFilter.getCreatedAtTo(),
                processedFilter.getCreatedAtTo() == null,
                processedFilter.getIsRead(),
                processedFilter.getIsRead() == null
        );

        // 정렬 적용
        // sortAlerts(): AlertSortType에 따라 다양한 정렬 기준 적용
        List<Alert> sortedAlerts = sortAlerts(alerts, processedFilter.getSortType());

        // Stream API: 엔티티를 DTO로 변환
        return sortedAlerts.stream()
                .map(AlertListItemResponseDTO::from) // 메서드 레퍼런스로 DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 알림 목록 정렬
     *
     * <p><b>지원 정렬 기준:</b></p>
     * <ul>
     *   <li>ALERT_LEVEL: 알림 레벨 (CRITICAL > HIGH > WARNING > INFO)</li>
     *   <li>METRIC_TYPE: 메트릭 타입 (알파벳 순)</li>
     *   <li>CONTAINER_NAME: 컨테이너 이름 (알파벳 순)</li>
     *   <li>METRIC_VALUE: 메트릭 값 (큰 값 우선)</li>
     *   <li>COLLECTED_AT: 수집 시간 (최신순)</li>
     *   <li>기본값: 생성 시간 내림차순 (최신순)</li>
     * </ul>
     *
     * <p><b>nullsLast:</b> null 값은 정렬 순서의 마지막에 배치</p>
     *
     * @param alerts 알림 목록
     * @param sortType 정렬 타입
     * @return 정렬된 알림 목록
     */
    private List<Alert> sortAlerts(List<Alert> alerts, AlertSortType sortType) {
        if (sortType == null) {
            // 기본 정렬: 생성 시간 내림차순 (최신순)
            return alerts.stream()
                    .sorted(Comparator.comparing(Alert::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }

        // Java 17+ switch expression: 패턴 매칭으로 가독성 향상
        return switch (sortType) {
            case ALERT_LEVEL -> alerts.stream()
                    .sorted(Comparator.comparing(Alert::getAlertLevel, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            case METRIC_TYPE -> alerts.stream()
                    .sorted(Comparator.comparing(Alert::getMetricType, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            case CONTAINER_NAME -> alerts.stream()
                    .sorted(Comparator.comparing(a -> a.getContainer().getName(), Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());
            case METRIC_VALUE -> alerts.stream()
                    .sorted(Comparator.comparing(Alert::getMetricValue, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
            case COLLECTED_AT -> alerts.stream()
                    .sorted(Comparator.comparing(Alert::getCollectedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        };
    }

    /**
     * QuickRangeType을 실제 날짜 범위로 변환
     *
     * <p><b>변환 로직:</b></p>
     * <ul>
     *   <li>quickRangeType이 있으면 현재 시간 기준으로 collectedAtFrom/To 계산</li>
     *   <li>quickRangeType이 없으면 기존 collectedAtFrom/To 사용</li>
     *   <li>예: LAST_1_HOUR -> 현재 시간 - 60분 ~ 현재 시간</li>
     * </ul>
     *
     * @param filter 원본 필터 DTO
     * @return 날짜 범위가 계산된 필터 DTO
     */
    private AlertFilterDTO processQuickRangeType(AlertFilterDTO filter) {
        if (filter == null || filter.getQuickRangeType() == null) {
            return filter;
        }

        // 현재 시간 기준으로 날짜 범위 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime from = now.minusMinutes(filter.getQuickRangeType().getMinutes());

        // QuickRangeType이 있으면 기존 collectedAtFrom/To는 무시하고 새로 계산된 값 사용
        // Builder 패턴: 가독성 높은 객체 생성
        return AlertFilterDTO.builder()
                .alertLevel(filter.getAlertLevel())
                .metricType(filter.getMetricType())
                .agentName(filter.getAgentName())
                .containerName(filter.getContainerName())
                .quickRangeType(null) // 이미 처리했으므로 null로 설정 (중복 처리 방지)
                .collectedAtFrom(from) // 계산된 시작 시간
                .collectedAtTo(now) // 현재 시간
                .createdAtFrom(filter.getCreatedAtFrom())
                .createdAtTo(filter.getCreatedAtTo())
                .isRead(filter.getIsRead())
                .sortType(filter.getSortType()) // 정렬 타입 유지
                .build();
    }
}
