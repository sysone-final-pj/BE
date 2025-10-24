package com.monito.domains.alert.dto.response;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 알림 목록 조회 응답 DTO (경량)
 * - 알림 목록 조회 (GET /api/alerts)
 * - 읽지 않은 알림 목록 조회 (GET /api/alerts/unread)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertListItemResponseDTO {

    private Long id;
    private String message;
    private AlertLevel alertLevel;
    private Boolean isRead;
    private LocalDateTime createdAt;
    private String containerName;

    public static AlertListItemResponseDTO from(Alert alert) {
        return AlertListItemResponseDTO.builder()
                .id(alert.getId())
                .message(alert.getMessage())
                .alertLevel(alert.getAlertLevel())
                .isRead(alert.getIsRead())
                .createdAt(alert.getCreatedAt())
                .containerName(alert.getContainer() != null ? alert.getContainer().getName() : null)
                .build();
    }
}