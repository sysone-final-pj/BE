package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Network 메트릭 (Agent가 보내는 구조)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class NetworkMetricsRequestDTO {
    private Long rxBytes;
    private Long txBytes;
    private Long rxPackets;
    private Long txPackets;
    private Long rxErrors;
    private Long txErrors;
    private Long rxDropped;
    private Long txDropped;
}