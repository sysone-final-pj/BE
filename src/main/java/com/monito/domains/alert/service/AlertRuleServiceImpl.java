package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import com.monito.domains.alert.dto.response.AlertRuleResponseDTO;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.member.domain.Member;
import com.monito.domains.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertRuleServiceImpl implements AlertRuleService {

    private final AlertRuleRepository alertRuleRepository;
    private final MemberRepository memberRepository;
    private final ContainerRepository containerRepository;

    /**
     * 알림 규칙 생성
     */
    @Override
    public AlertRuleResponseDTO createAlertRule(Long memberId, AlertRuleCreateRequestDTO request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        Container container = containerRepository.findById(request.getContainerId())
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다."));

        // 동일한 컨테이너 + 메트릭 타입 규칙이 이미 존재하는지 확인
        boolean exists = alertRuleRepository.existsByMemberIdAndContainerIdAndMetricTypeAndIsDeletedFalse(
                memberId, request.getContainerId(), request.getMetricType());

        if (exists) {
            throw new IllegalArgumentException(
                    String.format("해당 컨테이너(%s)의 %s 메트릭에 대한 알림 규칙이 이미 존재합니다.",
                            container.getName(), request.getMetricType()));
        }

        AlertRule alertRule = AlertRule.builder()
                .member(member)
                .container(container)
                .ruleName(request.getRuleName())
                .metricType(request.getMetricType())
                .isEnabled(true)
                .infoThreshold(request.getInfoThreshold())
                .warningThreshold(request.getWarningThreshold())
                .highThreshold(request.getHighThreshold())
                .criticalThreshold(request.getCriticalThreshold())
                .cooldownSeconds(request.getCooldownSeconds())
                .checkInterval(request.getCheckInterval())
                .build();

        AlertRule saved = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 생성 완료: ruleId={}, memberId={}, containerId={}, metricType={}",
                saved.getId(), memberId, request.getContainerId(), request.getMetricType());

        return AlertRuleResponseDTO.from(saved);
    }

    /**
     * 특정 알림 규칙 조회 (본인 규칙만 조회 가능)
     */
    @Override
    @Transactional(readOnly = true)
    public AlertRuleResponseDTO getAlertRule(Long ruleId, Long memberId) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("알림 규칙을 찾을 수 없습니다."));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림 규칙만 조회할 수 있습니다.");
        }

        return AlertRuleResponseDTO.from(alertRule);
    }

    /**
     * 사용자의 모든 알림 규칙 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getAllAlertRules(Long memberId) {
        return alertRuleRepository.findByMemberIdAndIsDeletedFalse(memberId)
                .stream()
                .map(AlertRuleResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 컨테이너의 알림 규칙 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getAlertRulesByContainer(Long memberId, Long containerId) {
        return alertRuleRepository.findByMemberIdAndContainerIdAndIsDeletedFalse(memberId, containerId)
                .stream()
                .map(AlertRuleResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 규칙 수정 (본인 규칙만 수정 가능)
     */
    @Override
    public AlertRuleResponseDTO updateAlertRule(Long ruleId, Long memberId, AlertRuleUpdateRequestDTO request) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("알림 규칙을 찾을 수 없습니다."));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림 규칙만 수정할 수 있습니다.");
        }

        // 업데이트할 필드만 반영 (null이 아닌 경우에만)
        if (request.getRuleName() != null) {
            alertRule.updateRuleName(request.getRuleName());
        }

        alertRule.updateThresholds(
                request.getInfoThreshold(),
                request.getWarningThreshold(),
                request.getHighThreshold(),
                request.getCriticalThreshold()
        );

        if (request.getCooldownSeconds() != null) {
            alertRule.updateCooldownSeconds(request.getCooldownSeconds());
        }
        if (request.getCheckInterval() != null) {
            alertRule.updateCheckInterval(request.getCheckInterval());
        }
        if (request.getIsEnabled() != null) {
            if (request.getIsEnabled()) {
                alertRule.enable();
            } else {
                alertRule.disable();
            }
        }

        AlertRule updated = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 수정 완료: ruleId={}, memberId={}", ruleId, memberId);

        return AlertRuleResponseDTO.from(updated);
    }

    /**
     * 알림 규칙 삭제 (Soft Delete, 본인 규칙만 삭제 가능)
     */
    @Override
    public void deleteAlertRule(Long ruleId, Long memberId) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("알림 규칙을 찾을 수 없습니다."));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림 규칙만 삭제할 수 있습니다.");
        }

        alertRule.delete();
        alertRuleRepository.save(alertRule);

        log.info("알림 규칙 삭제 완료: ruleId={}, memberId={}", ruleId, memberId);
    }

    /**
     * 알림 규칙 활성화/비활성화
     */
    @Override
    public AlertRuleResponseDTO toggleAlertRule(Long ruleId, Long memberId, boolean enabled) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("알림 규칙을 찾을 수 없습니다."));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인의 알림 규칙만 수정할 수 있습니다.");
        }

        if (enabled) {
            alertRule.enable();
        } else {
            alertRule.disable();
        }

        AlertRule updated = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 {}화 완료: ruleId={}, memberId={}", enabled ? "활성" : "비활성", ruleId, memberId);

        return AlertRuleResponseDTO.from(updated);
    }
}
