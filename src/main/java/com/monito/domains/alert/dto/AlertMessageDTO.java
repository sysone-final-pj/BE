/**
 *  WebSocket을 통해 실시간으로 푸시되는 알림 메시지
 *  - 프론트엔드에서 토스트/팝업 알림을 표시하기 위한 간단한 메시지
 */

package com.monito.domains.alert.dto;

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
public class AlertMessageDTO {
    private Long alertId;
    private String metricType;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private ContainerInfoDTO containerInfo;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContainerInfoDTO {
        private Long containerId;
        private String containerName;
        private String containerHash;
        private String metricType;
        private BigDecimal metricValue;
    }
}