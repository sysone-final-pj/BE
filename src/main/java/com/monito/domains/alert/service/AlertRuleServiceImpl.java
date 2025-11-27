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
 작성자: 이지민
 */
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
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.MEMBER_NOT_FOUND));

        // 동일 metricType에 enable=true인 룰이 있으면 비활성화 (새 룰이 enable=true로 생성되므로)
        disableOtherRulesForMetricType(memberId, request.getMetricType());

        AlertRule alertRule = AlertRule.builder()
                .member(member)
                .ruleName(request.getRuleName())
                .metricType(request.getMetricType())
                .isEnabled(true)
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
     */
    @Override
    @Transactional(readOnly = true)
    public AlertRuleResponseDTO getAlertRule(Long ruleId, Long memberId) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_VIEW_ACCESS_DENIED);
        }

        return AlertRuleResponseDTO.from(alertRule);
    }

    /**
     * 사용자의 모든 알림 규칙 조회
     */
    @Override
    @Transactional(readOnly = true)
    public List<AlertRuleResponseDTO> getAllAlertRules(Long memberId) {
        return alertRuleRepository.findByMemberId(memberId)
                .stream()
                .map(AlertRuleResponseDTO::from)
                .collect(Collectors.toList());
    }

    /**
     * 알림 규칙 수정
     */
    @Override
    public AlertRuleResponseDTO updateAlertRule(Long ruleId, Long memberId, AlertRuleUpdateRequestDTO request) {
        AlertRule alertRule = alertRuleRepository.findById(ruleId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_UPDATE_ACCESS_DENIED);
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
        if (request.getIsEnabled() != null) {
            if (request.getIsEnabled()) {
                // enable=true로 변경 시 동일 metricType의 다른 룰들 비활성화
                disableOtherRulesForMetricType(memberId, alertRule.getMetricType(), ruleId);
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
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_DELETE_ACCESS_DENIED);
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
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.ALERT_RULE_NOT_FOUND));

        if (!alertRule.getMember().getId().equals(memberId)) {
            throw new ForbiddenException(ExceptionMessage.ALERT_RULE_UPDATE_ACCESS_DENIED);
        }

        if (enabled) {
            // enable=true로 변경 시 동일 metricType의 다른 룰들 비활성화
            disableOtherRulesForMetricType(memberId, alertRule.getMetricType(), ruleId);
            alertRule.enable();
        } else {
            alertRule.disable();
        }

        AlertRule updated = alertRuleRepository.save(alertRule);
        log.info("알림 규칙 {}화 완료: ruleId={}, memberId={}", enabled ? "활성" : "비활성", ruleId, memberId);

        return AlertRuleResponseDTO.from(updated);
    }

    /**
     * 동일 metricType의 다른 활성화된 룰들을 비활성화
     * @param memberId 사용자 ID
     * @param metricType 메트릭 타입
     */
    private void disableOtherRulesForMetricType(Long memberId, MetricType metricType) {
        disableOtherRulesForMetricType(memberId, metricType, null);
    }

    /**
     * 동일 metricType의 다른 활성화된 룰들을 비활성화 (특정 룰 제외)
     * @param memberId 사용자 ID
     * @param metricType 메트릭 타입
     * @param excludeRuleId 제외할 룰 ID (null 가능)
     */
    private void disableOtherRulesForMetricType(Long memberId, MetricType metricType, Long excludeRuleId) {
        List<AlertRule> enabledRules = alertRuleRepository.findByMemberIdAndMetricTypeAndIsEnabledTrue(memberId, metricType);

        for (AlertRule rule : enabledRules) {
            // excludeRuleId가 지정되어 있으면 해당 룰은 제외
            if (excludeRuleId != null && rule.getId().equals(excludeRuleId)) {
                continue;
            }
            rule.disable();
            alertRuleRepository.save(rule);
            log.info("동일 metricType의 기존 활성 룰 비활성화: ruleId={}, metricType={}", rule.getId(), metricType);
        }
    }
}
