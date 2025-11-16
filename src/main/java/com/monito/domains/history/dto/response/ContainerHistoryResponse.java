package com.monito.domains.history.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
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
public class ContainerHistoryResponse {

    // ========== 기본 정보 (UI 상단에 표시될 주요 정보) ==========

    /**
     * 수집 시간 (UI DatePicker 형식)
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime collectedAt;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * 컨테이너 해시
     */
    private String containerHash;

    /**
     * 에이전트 이름
     */
    private String agentName;

    /**
     * 이미지 이름:태그
     */
    private String imgNameTag;

    /**
     * 컨테이너 상태
     */
    private ContainerState state;

    /**
     * 컨테이너 헬스 상태
     */
    private ContainerHealth health;

    /**
     * 컨테이너 생성 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime containerCreatedAt;

    /**
     * 삭제 여부
     */
    private Integer isDeleted;

    // ========== CPU 메트릭 ==========

    private BigDecimal cpuPercent;
    private BigDecimal cpuCoreUsage;
    private Long hostCpuUsageTotal;
    private Long cpuUsageTotal;
    private Long cpuUser;
    private Long cpuSystem;
    private Long cpuQuota;
    private Long cpuPeriod;
    private Integer onlineCpus;
    private Long throttlingPeriods;
    private Long throttledPeriods;
    private Long throttledTime;

    /**
     * CPU 제한 (코어 수)
     */
    private BigDecimal cpuLimitCores;

    /**
     * CPU 무제한 여부
     */
    private Boolean isCpuUnlimited;

    // ========== Memory 메트릭 ==========

    private BigDecimal memPercent;
    private Long memUsage;
    private Long memMaxUsage;

    /**
     * 메모리 제한 (bytes)
     */
    private Long memLimit;

    /**
     * 메모리 무제한 여부
     */
    private Boolean isMemoryUnlimited;

    /**
     * 마지막 OOM Kill 시간
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastOomKilledAt;

    // ========== Block I/O 메트릭 ==========

    private Long blkRead;
    private Long blkWrite;
    private Long blkReadPerSec;
    private Long blkWritePerSec;

    // ========== Network 메트릭 ==========

    private Long rxBytes;
    private Long txBytes;
    private Long rxPackets;
    private Long txPackets;
    private Long networkTotalBytes;
    private Long rxBytesPerSec;
    private Long txBytesPerSec;
    private Long rxPps;
    private Long txPps;
    private BigDecimal rxFailureRate;
    private BigDecimal txFailureRate;
    private Integer rxErrors;
    private Integer txErrors;
    private Integer rxDropped;
    private Integer txDropped;

    // ========== Storage 메트릭 ==========

    private Long sizeRw;
    private Long sizeRootFs;

    /**
     * 스토리지 제한 (bytes)
     */
    private Long storageLimit;

    /**
     * 스토리지 무제한 여부
     */
    private Boolean isStorageUnlimited;
}