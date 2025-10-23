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

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertResponseDTO {

    private Long id;
    private Long ruleId;
    private String ruleName;
    private Long memberId;
    private String memberUsername;
    private Long containerId;
    private String containerName;
    private String message;
    private LocalDateTime createdAt;
    private MetricType metricType;
    private BigDecimal metricValue;
    private Boolean isRead;
    private AlertLevel alertLevel;

    public static AlertResponseDTO from(Alert alert) {
        return AlertResponseDTO.builder()
                .id(alert.getId())
                .ruleId(alert.getAlertRule().getId())
                .ruleName(alert.getAlertRule().getRuleName())
                .memberId(alert.getMember().getId())
                .memberUsername(alert.getMember().getUsername())
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