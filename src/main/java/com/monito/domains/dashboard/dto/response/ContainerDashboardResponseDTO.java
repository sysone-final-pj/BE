package com.monito.domains.dashboard.dto.response;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 컨테이너 리스트 및 상세 조회용 DTO
 * - 대시보드 카드: 기본 정보 + 주요 메트릭
 * - 상세 패널: 모든 메트릭 (실시간 업데이트)
 * - REST API: 주요 필드만 제공 (13개)
 * - WebSocket: 모든 필드 제공 (실시간)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerDashboardResponseDTO {

    // 기본 정보
    private Long containerId;
    private String containerHash;
    private String containerName;
    private String agentName;
    private ContainerState state;
    private ContainerHealth health;

    // CPU 메트릭
    private BigDecimal cpuPercent;          // CPU 사용률 (%)
    private BigDecimal cpuCoreUsage;        // Core 사용량
    private Long cpuUsageTotal;             // 컨테이너 누적 CPU 사용량
    private Long hostCpuUsageTotal;         // 호스트 누적 CPU 사용량
    private Long cpuUser;                   // User mode CPU
    private Long cpuSystem;                 // System mode CPU
    private Long cpuQuota;                  // CPU 할당량
    private Long cpuPeriod;                 // CPU 주기
    private Integer onlineCpus;             // 온라인 CPU 개수
    private Long throttlingPeriods;         // Throttling 발생 주기
    private Long throttledPeriods;          // Throttled 주기
    private Long throttledTime;             // Throttled 시간 (ns)

    // Memory 메트릭
    private BigDecimal memPercent;          // 메모리 사용률 (%)
    private Long memUsage;                  // 현재 메모리 사용량
    private Long memLimit;                  // 메모리 제한
    private Long memMaxUsage;               // 최대 메모리 사용량

    // Block I/O 메트릭
    private Long blkRead;                   // 블록 읽기 누적
    private Long blkWrite;                  // 블록 쓰기 누적
    private Long blkReadPerSec;             // 읽기 속도 (bytes/s)
    private Long blkWritePerSec;            // 쓰기 속도 (bytes/s)

    // Network 메트릭
    private Long rxBytes;                   // 수신 바이트 누적
    private Long txBytes;                   // 송신 바이트 누적
    private Long rxPackets;                 // 수신 패킷 수
    private Long txPackets;                 // 송신 패킷 수
    private Long networkTotalBytes;         // 총 네트워크 바이트
    private Long rxBytesPerSec;             // 수신 속도 (bytes/s)
    private Long txBytesPerSec;             // 송신 속도 (bytes/s)
    private Long rxPps;                     // 수신 PPS
    private Long txPps;                     // 송신 PPS
    private BigDecimal rxFailureRate;       // 수신 실패율 (%)
    private BigDecimal txFailureRate;       // 송신 실패율 (%)
    private Integer rxErrors;               // 수신 에러
    private Integer txErrors;               // 송신 에러
    private Integer rxDropped;              // 수신 드롭
    private Integer txDropped;              // 송신 드롭

    // Storage 메트릭
    private Long sizeRw;                    // 컨테이너 쓰기 크기
    private Long sizeRootFs;                // 루트 파일시스템 크기

    /**
     * REST API용 생성자 (JPQL에서 사용)
     * - 주요 메트릭만 포함 (13개 필드)
     */
    public ContainerDashboardResponseDTO(
            Long containerId,
            String containerHash,
            String containerName,
            String agentName,
            ContainerState state,
            ContainerHealth health,
            BigDecimal cpuPercent,
            BigDecimal memPercent,
            Long memUsage,
            Long memLimit,
            Long blkRead,
            Long blkWrite,
            Long rxBytesPerSec,
            Long txBytesPerSec
    ) {
        this.containerId = containerId;
        this.containerHash = containerHash;
        this.containerName = containerName;
        this.agentName = agentName;
        this.state = state;
        this.health = health;
        this.cpuPercent = cpuPercent;
        this.memPercent = memPercent;
        this.memUsage = memUsage;
        this.memLimit = memLimit;
        this.blkRead = blkRead;
        this.blkWrite = blkWrite;
        this.rxBytesPerSec = rxBytesPerSec;
        this.txBytesPerSec = txBytesPerSec;

        // 나머지 필드는 null (WebSocket에서만 전체 제공)
        this.cpuCoreUsage = null;
        this.cpuUsageTotal = null;
        this.hostCpuUsageTotal = null;
        this.cpuUser = null;
        this.cpuSystem = null;
        this.cpuQuota = null;
        this.cpuPeriod = null;
        this.onlineCpus = null;
        this.throttlingPeriods = null;
        this.throttledPeriods = null;
        this.throttledTime = null;
        this.memMaxUsage = null;
        this.blkReadPerSec = null;
        this.blkWritePerSec = null;
        this.rxBytes = null;
        this.txBytes = null;
        this.rxPackets = null;
        this.txPackets = null;
        this.networkTotalBytes = null;
        this.rxPps = null;
        this.txPps = null;
        this.rxFailureRate = null;
        this.txFailureRate = null;
        this.rxErrors = null;
        this.txErrors = null;
        this.rxDropped = null;
        this.txDropped = null;
        this.sizeRw = null;
        this.sizeRootFs = null;
    }
}
