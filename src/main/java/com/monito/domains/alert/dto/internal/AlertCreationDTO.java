package com.monito.domains.alert.dto.internal;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.member.domain.Member;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * AlertRuleEvaluator에서 AlertService로 알림 생성 요청 시 사용하는 DTO
 * - 파라미터 간소화 및 응집도 향상 용도
 */
@Getter
@Builder
public class AlertCreationDTO {

    private Member member;
    private AlertRule alertRule;
    private Container container;
    private String message;
    private MetricType metricType;
    private BigDecimal metricValue;
    private AlertLevel alertLevel;

    public Alert toEntity() {
        return Alert.builder()
                .member(member)
                .alertRule(alertRule)
                .container(container)
                .message(message)
                .metricType(metricType)
                .metricValue(metricValue)
                .alertLevel(alertLevel)
                .isRead(false)
                .build();
    }
}