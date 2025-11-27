/**
 * 알림에 포함되는 컨테이너 정보 응답 DTO
 */
package com.monito.domains.alert.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerInfoResponseDTO {
    private Long containerId;
    private String agentName;
    private String containerName;
    private String containerHash;
    private String metricType;
    private BigDecimal metricValue;
}