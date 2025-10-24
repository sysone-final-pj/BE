package com.monito.domains.container.dto.request;

import com.monito.domains.container.domain.ContainerState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Agent로부터 WebSocket으로 수신되는 컨테이너 메트릭 데이터
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerMetricsRequestDTO {
    private String containerHash;
    private String containerName;
    private ContainerState state;

    // CPU 관련
    private Long hostCpuUsageTotal;
    private Long cpuUsageTotal;
    private Long cpuUser;
    private Long cpuSystem;
    private Long cpuQuota;
    private Long cpuPeriod;
    private Long cpuLimit;
    private Integer onlineCpus;
    private Long throttlingPeriods;
    private Long throttledPeriods;
    private Long throttledTime;
    private Integer oomKills;

    // Memory 관련
    private Long memUsage;
    private Long memLimit;
    private Long memMaxUsage;
    private Long memRss;
    private Long memCache;

    // Block I/O 관련
    private Long blkRead;
    private Long blkWrite;

    // Network 관련
    private Long rxBytes;
    private Long txBytes;
    private Integer rxErrors;
    private Integer txErrors;
    private Integer rxDropped;
    private Integer txDropped;
}