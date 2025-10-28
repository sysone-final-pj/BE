package com.monito.domains.container.dto.response;

import com.monito.domains.container.domain.ContainerState;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 컨테이너 리스트 조회용 DTO
 * - Agent name, Container hash, name
 * - CPU (%), Memory (current/limit)
 * - Storage (blkRead/blkWrite)
 * - Network (download/upload speed)
 * - State
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerListResponseDTO {
    // 기본 정보
    private Long containerId;
    private String containerHash;
    private String containerName;
    private String agentName;
    private ContainerState state;

    // CPU
    private BigDecimal cpuPercent;

    // Memory
    private BigDecimal memPercent;
    private Long memUsage;
    private Long memLimit;

    // Storage (Block I/O)
    private Long blkRead;
    private Long blkWrite;

    // Network (bytes per second)
    private Long rxBytesPerSec;  // download speed (bytes/s)
    private Long txBytesPerSec;  // upload speed (bytes/s)
}