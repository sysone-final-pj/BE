package com.monito.domains.alert.dto.response;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.container.domain.MetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
/**
 * AlertRule의 CRUD API
 * - 규칙 생성/단일조회/목록조회/단일수정
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleResponseDTO {
    private Long id;
    private Long memberId;
    private String memberUsername;
    private String ruleName;
    private MetricType metricType;
    private Boolean isEnabled;
    private BigDecimal infoThreshold;
    private BigDecimal warningThreshold;
    private BigDecimal highThreshold;
    private BigDecimal criticalThreshold;
    private Integer cooldownSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static AlertRuleResponseDTO from(AlertRule alertRule) {
        return AlertRuleResponseDTO.builder()
                .id(alertRule.getId())
                .memberId(alertRule.getMember().getId())
                .memberUsername(alertRule.getMember().getUsername())
                .ruleName(alertRule.getRuleName())
                .metricType(alertRule.getMetricType())
                .isEnabled(alertRule.getIsEnabled())
                .infoThreshold(alertRule.getInfoThreshold())
                .warningThreshold(alertRule.getWarningThreshold())
                .highThreshold(alertRule.getHighThreshold())
                .criticalThreshold(alertRule.getCriticalThreshold())
                .cooldownSeconds(alertRule.getCooldownSeconds())
                .createdAt(alertRule.getCreatedAt())
                .updatedAt(alertRule.getUpdatedAt())
                .build();
    }
}
