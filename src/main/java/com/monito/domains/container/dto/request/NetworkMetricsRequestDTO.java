/**
 * Network 메트릭 (Agent가 보내는 구조)
 */
package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkMetricsRequestDTO {
    private Long rxBytes;
    private Long txBytes;
    private Long rxPackets;
    private Long txPackets;
    private Integer rxErrors;
    private Integer txErrors;
    private Integer rxDropped;
    private Integer txDropped;
}