/**
 * 알림 목록 조회 응답 DTO (경량)
 * - 알림 목록 조회 (GET /api/alerts)
 * - 읽지 않은 알림 목록 조회 (GET /api/alerts/unread)
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
public class AlertListItemResponseDTO {

    private Long id;
    private String message;
    private AlertLevel alertLevel;
    private MetricType metricType;
    private BigDecimal metricValue;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime collectedAt;
    private String containerName;
    private String agentName;

    public static AlertListItemResponseDTO from(Alert alert) {
        return AlertListItemResponseDTO.builder()
                .id(alert.getId())
                .message(alert.getMessage())
                .alertLevel(alert.getAlertLevel())
                .metricType(alert.getMetricType())
                .metricValue(alert.getMetricValue())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .collectedAt(alert.getCollectedAt())
                .containerName(alert.getContainer() != null ? alert.getContainer().getName() : null)
                .agentName(alert.getContainer() != null && alert.getContainer().getAgent() != null
                        ? alert.getContainer().getAgent().getAgentName() : null)
                .build();
    }
}