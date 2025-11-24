package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import com.monito.domains.alert.dto.response.AlertRuleResponseDTO;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.ForbiddenException;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 알림 규칙(AlertRule) 서비스 구현체
 *
 * <p><b>역할:</b></p>
 * <ul>
 *   <li>알림 규칙 생성, 조회, 수정, 삭제 (CRUD)</li>
 *   <li>메트릭 타입별 활성 규칙 단일화 관리 (동일 메트릭 타입에 하나의 규칙만 활성)</li>
 *   <li>알림 임계값 설정 (INFO, WARNING, HIGH, CRITICAL)</li>
 *   <li>쿨다운 타이머 관리 (중복 알림 방지)</li>
 * </ul>
 *
 * <p><b>주요 패턴:</b></p>
 * <ul>
 *   <li>Service 패턴: 비즈니스 로직을 캡슐화하여 재사용성 향상</li>
 *   <li>DTO 변환 패턴: Entity와 DTO 분리로 계층 간 결합도 감소</li>
 *   <li>Soft Delete 패턴: 물리적 삭제 대신 논리적 삭제로 데이터 보존</li>
 *   <li>권한 검증 패턴: 본인 소유 규칙만 접근 가능하도록 제어</li>
 *   <li>단일 활성 규칙 패턴: 동일 메트릭 타입에 하나의 규칙만 활성화</li>
 * </ul>
 *
 * <p><b>비즈니스 규칙:</b></p>
 * <ul>
 *   <li>각 메트릭 타입(CPU, Memory, Disk 등)별로 하나의 활성 규칙만 존재</li>
 *   <li>새 규칙 활성화 시 기존 활성 규칙은 자동 비활성화</li>
 *   <li>임계값: INFO < WARNING < HIGH < CRITICAL 순서</li>
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
public class AlertRuleServiceImpl implements AlertRuleService {

    /**
     * 알림 규칙 저장소
     * <p>알림 규칙(AlertRule) 조회 및 저장을 담당</p>
     */
    private final AlertRuleRepository alertRuleRepository;

    /**
     * 회원 저장소
     * <p>회원(Member) 조회를 담당</p>
     */
    private final MemberRepository memberRepository;

    /**
     * 컨테이너 저장소
     * <p>컨테이너(Container) 조회를 담당</p>
     */
    private final ContainerRepository containerRepository;

    /**
     * 알림 규칙 생성
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>회원 존재 여부 확인</li>
     *   <li>동일 메트릭 타입의 기존 활성 규칙 비활성화</li>
     *   <li>새 규칙 생성 (기본적으로 활성 상태)</li>
     *   <li>DB 저장 후 DTO 변환하여 반환</li>
     * </ol>
     *
     * <p><b>비즈니스 규칙:</b></p>
     * <ul>
     *   <li>각 메트릭 타입별로 하나의 활성 규칙만 존재</li>
     *   <li>새 규칙은 기본적으로 활성화 상태(isEnabled=true)로 생성</li>
     * </ul>
     *
     * @param memberId 회원 ID
     * @param request 알림 규칙 생성 요청 DTO
     * @return 생성된 알림 규칙 DTO
     * @throws NotFoundException 회원이 존재하지 않을 때
     */
    @Override
    public AlertRuleResponseDTO createAlertRule(Long memberId, AlertRuleCreateRequestDTO request) {
        // 회원 존재 여부 확인
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));

        // 동일 metricType에 enable=true인 룰이 있으면 비활성화
        // 비즈니스 규칙: 각 메트릭 타입별로 하나의 활성 규칙만 존재
        disableOtherRulesForMetricType(memberId, request.getMetricType());

        // Builder 패턴: 가독성 높은 객체 생성
        AlertRule alertRule = AlertRule.builder()
                .member(member)
                .ruleName(request.getRuleName())
                .metricType(request.getMetricType())
                .isEnabled(true) // 새 규칙은 기본적으로 활성화
                .infoThreshold(request.getInfoThreshold())
                .warningThreshold(request.getWarningThreshold())
                .highThreshold(request.getHighThreshold())
                .criticalThreshold(request.getCriticalThreshold())
                .cooldownSeconds(request.getCooldownSeconds())
                .build();

        AlertRule saved = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 생성 완료: ruleId={}, memberId={}, metricType={}",
                saved.getId(), memberId, request.getMetricType());

        return AlertRuleResponseDTO.from(saved);
    }

    /**
     * 특정 알림 규칙 조회
     *
     * <p><b>권한 검증:</b> 본인 소유 규칙만 조회 가능</p>
     *
     * @param ruleId 알림 규칙 ID
     * @param memberId 회원 ID
     * @return 알림 규칙 DTO
     * @throws NotFoundException 알림 규칙이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public AlertRuleResponseDTO getAlertRule(Long ruleId, Long memberId) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        // 권한 검증: 본인 소유 규칙만 조회 가능
        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_VIEW_ACCESS_DENIED);
        }

        return AlertRuleResponseDTO.from(alertRule);
    }

    /**
     * 사용자의 모든 알림 규칙 조회
     *
     * <p><b>Stream API:</b> 엔티티를 DTO로 변환하여 반환</p>
     *
     * @param memberId 회원 ID
     * @return 알림 규칙 목록 DTO
     */
    @Override
    @Transactional(readOnly = true)
    // 읽기 전용 트랜잭션: Dirty Checking 비활성화로 성능 최적화
    public List<AlertRuleResponseDTO> getAllAlertRules(Long memberId) {
        return alertRuleRepository.findByMemberId(memberId)
                .stream()
                .map(AlertRuleResponseDTO::from) // 메서드 레퍼런스로 DTO 변환
                .collect(Collectors.toList());
    }

    /**
     * 알림 규칙 수정
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>알림 규칙 조회 및 권한 검증</li>
     *   <li>null이 아닌 필드만 선택적 업데이트 (Partial Update)</li>
     *   <li>활성화 변경 시 동일 메트릭 타입의 다른 규칙 비활성화</li>
     *   <li>Dirty Checking으로 자동 UPDATE 쿼리 실행</li>
     * </ol>
     *
     * <p><b>Partial Update:</b> null이 아닌 필드만 업데이트하여 유연성 제공</p>
     *
     * @param ruleId 알림 규칙 ID
     * @param memberId 회원 ID
     * @param request 수정 요청 DTO
     * @return 수정된 알림 규칙 DTO
     * @throws NotFoundException 알림 규칙이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    public AlertRuleResponseDTO updateAlertRule(Long ruleId, Long memberId, AlertRuleUpdateRequestDTO request) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        // 권한 검증: 본인 소유 규칙만 수정 가능
        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_UPDATE_ACCESS_DENIED);
        }

        // Partial Update: null이 아닌 필드만 선택적으로 업데이트
        if (request.getRuleName() != null) {
            alertRule.updateRuleName(request.getRuleName());
        }

        // 임계값 업데이트 (엔티티 메서드 사용)
        alertRule.updateThresholds(
                request.getInfoThreshold(),
                request.getWarningThreshold(),
                request.getHighThreshold(),
                request.getCriticalThreshold()
        );

        // 쿨다운 시간 업데이트
        if (request.getCooldownSeconds() != null) {
            alertRule.updateCooldownSeconds(request.getCooldownSeconds());
        }

        // 활성화 상태 변경
        if (request.getIsEnabled() != null) {
            if (request.getIsEnabled()) {
                // enable=true로 변경 시 동일 metricType의 다른 룰들 비활성화
                // 비즈니스 규칙: 각 메트릭 타입별로 하나의 활성 규칙만 존재
                disableOtherRulesForMetricType(memberId, alertRule.getMetricType(), ruleId);
                alertRule.enable();
            } else {
                alertRule.disable();
            }
        }

        // Dirty Checking: 엔티티 상태 변경 시 자동 UPDATE
        AlertRule updated = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 수정 완료: ruleId={}, memberId={}", ruleId, memberId);

        return AlertRuleResponseDTO.from(updated);
    }

    /**
     * 알림 규칙 삭제 (Soft Delete, 본인 규칙만 삭제 가능)
     *
     * <p><b>Soft Delete 패턴:</b></p>
     * <ul>
     *   <li>물리적 삭제 대신 isDeleted 플래그를 true로 설정</li>
     *   <li>데이터 복구 가능, 감사(Audit) 기록 유지</li>
     * </ul>
     *
     * @param ruleId 알림 규칙 ID
     * @param memberId 회원 ID
     * @throws NotFoundException 알림 규칙이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    public void deleteAlertRule(Long ruleId, Long memberId) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        // 권한 검증: 본인 소유 규칙만 삭제 가능
        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_DELETE_ACCESS_DENIED);
        }

        // Soft Delete: isDeleted 플래그 설정
        alertRule.delete();
        alertRuleRepository.save(alertRule);

        log.info("알림 규칙 삭제 완료: ruleId={}, memberId={}", ruleId, memberId);
    }

    /**
     * 알림 규칙 활성화/비활성화
     *
     * <p><b>토글 기능:</b></p>
     * <ul>
     *   <li>활성화 시: 동일 메트릭 타입의 다른 규칙 자동 비활성화</li>
     *   <li>비활성화 시: 해당 규칙만 비활성화</li>
     * </ul>
     *
     * @param ruleId 알림 규칙 ID
     * @param memberId 회원 ID
     * @param enabled 활성화 여부 (true: 활성화, false: 비활성화)
     * @return 수정된 알림 규칙 DTO
     * @throws NotFoundException 알림 규칙이 존재하지 않을 때
     * @throws ForbiddenException 권한이 없을 때
     */
    @Override
    public AlertRuleResponseDTO toggleAlertRule(Long ruleId, Long memberId, boolean enabled) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        // 권한 검증: 본인 소유 규칙만 수정 가능
        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_UPDATE_ACCESS_DENIED);
        }

        if (enabled) {
            // enable=true로 변경 시 동일 metricType의 다른 룰들 비활성화
            // 비즈니스 규칙: 각 메트릭 타입별로 하나의 활성 규칙만 존재
            disableOtherRulesForMetricType(memberId, alertRule.getMetricType(), ruleId);
            alertRule.enable();
        } else {
            alertRule.disable();
        }

        // Dirty Checking: 엔티티 상태 변경 시 자동 UPDATE
        AlertRule updated = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 {}화 완료: ruleId={}, memberId={}", enabled ? "활성" : "비활성", ruleId, memberId);

        return AlertRuleResponseDTO.from(updated);
    }

    /**
     * 동일 metricType의 다른 활성화된 룰들을 비활성화
     *
     * <p><b>비즈니스 규칙:</b> 각 메트릭 타입별로 하나의 활성 규칙만 존재</p>
     *
     * @param memberId 사용자 ID
     * @param metricType 메트릭 타입
     */
    private void disableOtherRulesForMetricType(Long memberId, MetricType metricType) {
        // excludeRuleId 없이 호출 (모든 활성 규칙 비활성화)
        disableOtherRulesForMetricType(memberId, metricType, null);
    }

    /**
     * 동일 metricType의 다른 활성화된 룰들을 비활성화 (특정 룰 제외)
     *
     * <p><b>처리 흐름:</b></p>
     * <ol>
     *   <li>해당 회원의 동일 메트릭 타입의 활성 규칙 조회</li>
     *   <li>excludeRuleId를 제외한 모든 규칙 비활성화</li>
     *   <li>각 규칙마다 로그 기록</li>
     * </ol>
     *
     * <p><b>메서드 오버로딩:</b> excludeRuleId 유무에 따라 두 개의 메서드 제공</p>
     *
     * @param memberId 사용자 ID
     * @param metricType 메트릭 타입
     * @param excludeRuleId 제외할 룰 ID (null 가능)
     */
    private void disableOtherRulesForMetricType(Long memberId, MetricType metricType, Long excludeRuleId) {
        // 동일 메트릭 타입의 활성 규칙 조회
        List<AlertRule> enabledRules = alertRuleRepository.findByMemberIdAndMetricTypeAndIsEnabledTrue(memberId, metricType);

        // 각 규칙을 순회하며 비활성화
        for (AlertRule rule : enabledRules) {
            // excludeRuleId가 지정되어 있으면 해당 룰은 제외
            // (현재 활성화하려는 규칙은 비활성화하지 않음)
            if (excludeRuleId != null && rule.getId().equals(excludeRuleId)) {
                continue;
            }
            // 규칙 비활성화 (Dirty Checking으로 자동 UPDATE)
            rule.disable();
            alertRuleRepository.save(rule);
            log.info("동일 metricType의 기존 활성 룰 비활성화: ruleId={}, metricType={}", rule.getId(), metricType);
        }
    }
}
