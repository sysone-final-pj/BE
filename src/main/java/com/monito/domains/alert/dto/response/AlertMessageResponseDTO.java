package com.monito.domains.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WebSocket을 통해 실시간으로 푸시되는 알림 메시지
 * - 프론트엔드에서 토스트/팝업 알림을 표시하기 위한 응답 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertMessageResponseDTO {
    private Long alertId;
    private String metricType;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private ContainerInfoResponseDTO containerInfo;
}
