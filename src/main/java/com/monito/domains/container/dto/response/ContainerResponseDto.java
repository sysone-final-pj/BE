package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.ContainerState;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agent로부터 받아오는 컨테이너 정보 DTO
 * todo: Agent API 응답 구조에 맞춰 필드를 정의해야 함
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerResponseDto {

    private String containerHash;
    private String name;
    private ContainerState status;

    // CPU 관련
    private BigDecimal cpuPercent;
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
    private BigDecimal memPercent;
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
    private Long rxMbps;
    private Long txMbps;
    private Long rxPps;
    private Long txPps;
    private Integer rxErrors;
    private Integer txErrors;
    private Integer rxDropped;
    private Integer txDropped;
}