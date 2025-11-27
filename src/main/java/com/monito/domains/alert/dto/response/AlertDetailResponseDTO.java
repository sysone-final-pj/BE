/**
 * 알림 상세 조회 응답 DTO
 * - 단일 알림 조회 (GET /api/alerts/{id})
 * - 알림 생성 후 응답 (POST /api/alerts)
 */
package com.monito.domains.alert.dto.response;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.container.domain.MetricType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertDetailResponseDTO {

    private Long id;
    private Long ruleId;
    private String ruleName;
    private Long containerId;
    private String containerName;
    private String message;
    private LocalDateTime createdAt;
    private MetricType metricType;
    private BigDecimal metricValue;
    private Boolean isRead;
    private AlertLevel alertLevel;

    public static AlertDetailResponseDTO from(Alert alert) {
        return AlertDetailResponseDTO.builder()
                .id(alert.getId())
                .ruleId(alert.getAlertRule().getId())
                .ruleName(alert.getAlertRule().getRuleName())
                .containerId(alert.getContainer() != null ? alert.getContainer().getId() : null)
                .containerName(alert.getContainer() != null ? alert.getContainer().getName() : null)
                .message(alert.getMessage())
                .createdAt(alert.getCreatedAt())
                .metricType(alert.getMetricType())
                .metricValue(alert.getMetricValue())
                .isRead(alert.getIsRead())
                .alertLevel(alert.getAlertLevel())
                .build();
    }
}