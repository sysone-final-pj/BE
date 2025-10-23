package com.monito.domains.alert.service;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.MetricType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 알림 규칙 평가 서비스 구현체
 * - 컨테이너 메트릭과 사용자별 AlertRule을 비교하여 알림 발생 여부 판단
 * - 중복 알림 방지 (쿨다운 기간)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertRuleEvaluatorImpl implements AlertRuleEvaluator {

    private final AlertRuleRepository alertRuleRepository;
    private final AlertService alertService;

    // 중복 알림 방지: Key = "memberId:containerId:ruleId", Value = 마지막 알림 시간
    private final Map<String, LocalDateTime> lastAlertTimes = new ConcurrentHashMap<>();

    /**
     * 컨테이너 메트릭 평가 및 알림 생성
     * 해당 컨테이너에 대한 모든 활성화된 AlertRule을 조회하여 평가
     */
    @Override
    public void evaluateContainer(Container container) {
        // 해당 컨테이너에 대한 활성화된 모든 규칙 조회
        List<AlertRule> activeRules = alertRuleRepository
                .findByContainerIdAndIsEnabledTrueAndIsDeletedFalse(container.getId());

        log.debug("컨테이너 {} 평가: {}개 규칙 발견", container.getId(), activeRules.size());

        for (AlertRule rule : activeRules) {
            evaluateRule(container, rule);
        }
    }

    /**
     * 특정 규칙에 대해 컨테이너 평가
     */
    private void evaluateRule(Container container, AlertRule rule) {
        try {
            // 메트릭 타입에 따라 현재 값 가져오기
            BigDecimal currentValue = getCurrentMetricValue(container, rule.getMetricType());

            if (currentValue == null) {
                log.debug("메트릭 값이 null: containerId={}, metricType={}",
                        container.getId(), rule.getMetricType());
                return;
            }

            // AlertRule의 determineAlertLevel 메서드 사용
            AlertLevel alertLevel = rule.determineAlertLevel(currentValue);

            if (alertLevel != null) {
                // 알림 발생 조건 충족
                triggerAlertIfNeeded(container, rule, alertLevel, currentValue);
            }

        } catch (Exception e) {
            log.error("규칙 평가 중 오류 발생: ruleId={}, containerId={}",
                    rule.getId(), container.getId(), e);
        }
    }

    /**
     * 컨테이너에서 메트릭 타입에 해당하는 현재 값 추출
     */
    private BigDecimal getCurrentMetricValue(Container container, MetricType metricType) {
        return switch (metricType) {
            case CPU -> container.getCpuPercent();
            case MEMORY -> container.getMemPercent();
            case NETWORK -> {
                // 네트워크는 RX + TX 합계 (Mbps)
                Long rxMbps = container.getRxMbps() != null ? container.getRxMbps() : 0L;
                Long txMbps = container.getTxMbps() != null ? container.getTxMbps() : 0L;
                yield BigDecimal.valueOf(rxMbps + txMbps);
            }
            default -> null;
        };
    }

    /**
     * 알림 발생 (쿨다운 체크 포함)
     */
    private void triggerAlertIfNeeded(Container container, AlertRule rule,
                                      AlertLevel alertLevel, BigDecimal currentValue) {
        String cooldownKey = getCooldownKey(rule.getMember().getId(), container.getId(), rule.getId());
        LocalDateTime now = LocalDateTime.now();

        // 쿨다운 체크
        if (!isCooldownExpired(cooldownKey, rule.getCooldownSeconds(), now)) {
            log.debug("쿨다운 기간 중 알림 스킵: memberId={}, containerId={}, ruleId={}",
                    rule.getMember().getId(), container.getId(), rule.getId());
            return;
        }

        // 알림 메시지 생성
        String message = buildAlertMessage(container, rule, alertLevel, currentValue);

        // 해당 사용자에게 알림 전송
        alertService.createAndSendAlert(
                rule.getMember(),
                rule,
                container,
                message,
                rule.getMetricType(),
                currentValue,
                alertLevel
        );

        // 마지막 알림 시간 기록
        lastAlertTimes.put(cooldownKey, now);

        log.info("알림 발생: memberId={}, containerId={}, ruleId={}, level={}, value={}",
                rule.getMember().getId(), container.getId(), rule.getId(), alertLevel, currentValue);
    }

    /**
     * 쿨다운 만료 여부 확인
     */
    private boolean isCooldownExpired(String cooldownKey, Integer cooldownSeconds, LocalDateTime now) {
        LocalDateTime lastAlertTime = lastAlertTimes.get(cooldownKey);

        if (lastAlertTime == null) {
            return true; // 이전 알림 없음
        }

        LocalDateTime cooldownExpireTime = lastAlertTime.plusSeconds(cooldownSeconds);
        return now.isAfter(cooldownExpireTime);
    }

    /**
     * 쿨다운 키 생성
     */
    private String getCooldownKey(Long memberId, Long containerId, Long ruleId) {
        return memberId + ":" + containerId + ":" + ruleId;
    }

    /**
     * 알림 메시지 생성
     */
    private String buildAlertMessage(Container container, AlertRule rule,
                                     AlertLevel alertLevel, BigDecimal currentValue) {
        String metricName = getMetricDisplayName(rule.getMetricType());
        BigDecimal threshold = getThresholdForLevel(rule, alertLevel);

        return String.format(
                "[%s] 컨테이너 '%s'의 %s 사용률이 임계값을 초과했습니다. (현재: %s%%, 임계값: %s%%)",
                alertLevel.getDescription(),
                container.getName(),
                metricName,
                currentValue.setScale(2, BigDecimal.ROUND_HALF_UP),
                threshold != null ? threshold.toString() : "N/A"
        );
    }

    /**
     * 알림 레벨에 해당하는 임계값 가져오기
     */
    private BigDecimal getThresholdForLevel(AlertRule rule, AlertLevel level) {
        return switch (level) {
            case CRITICAL -> rule.getCriticalThreshold();
            case HIGH -> rule.getHighThreshold();
            case WARNING -> rule.getWarningThreshold();
            case INFO -> rule.getInfoThreshold();
            default -> null;
        };
    }

    /**
     * 메트릭 타입의 표시 이름
     */
    private String getMetricDisplayName(MetricType metricType) {
        return switch (metricType) {
            case CPU -> "CPU";
            case MEMORY -> "메모리";
            case NETWORK -> "네트워크";
            default -> metricType.name();
        };
    }

    /**
     * 쿨다운 캐시 정리 (옵션)
     * 주기적으로 오래된 항목 제거 (메모리 관리)
     */
    @Override
    public void cleanupExpiredCooldowns(int retentionHours) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(retentionHours);

        lastAlertTimes.entrySet().removeIf(entry ->
                entry.getValue().isBefore(cutoffTime)
        );

        log.debug("쿨다운 캐시 정리 완료: 남은 항목 수 = {}", lastAlertTimes.size());
    }
}
