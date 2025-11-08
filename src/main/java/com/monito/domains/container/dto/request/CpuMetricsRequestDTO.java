package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * CPU 메트릭 (Agent가 보내는 구조)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class CpuMetricsRequestDTO {
    private Long cpuUsageTotal;
    private Long cpuUser;
    private Long cpuSystem;
    private Long systemCpuUsage;  // → hostCpuUsageTotal로 매핑
    private Integer onlineCpus;
    private Long cpuQuota;
    private Long cpuPeriod;
    private Boolean isCpuUnlimited;  // CPU 제한이 없는지 여부
    private Long throttlingPeriods;
    private Long throttledPeriods;
    private Long throttledTime;
}