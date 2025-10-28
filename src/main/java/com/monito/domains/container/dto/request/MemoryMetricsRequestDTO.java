package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Memory 메트릭 (Agent가 보내는 구조)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class MemoryMetricsRequestDTO {
    private Long memUsage;
    private Long memLimit;
}