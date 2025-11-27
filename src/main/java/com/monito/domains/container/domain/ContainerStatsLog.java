package com.monito.domains.container.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
/**
 작성자: 백승준
 */
@Entity
@Table(
        name = "container_stats_logs",
        indexes = {
                @jakarta.persistence.Index(name = "IDX_CONTAINER_STATS_COLLECTED_AT", columnList = "collected_at"),
                @jakarta.persistence.Index(name = "IDX_CONTAINER_STATS_CONTAINER_COLLECTED_AT", columnList = "container_id, collected_at")
        }
)
@IdClass(ContainerStatsLogId.class)
@SuperBuilder
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ContainerStatsLog {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "container_stats_log_seq")
    @SequenceGenerator(
            name = "container_stats_log_seq",
            sequenceName = "CONTAINER_STATS_LOG_SEQ",
            allocationSize = 1
    )
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "container_id", nullable = false)
    private Container container;

    @Column(nullable = false, length = 64)
    private String containerHash;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContainerState state;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private ContainerHealth health;

    // CPU 관련
    @Column(precision = 6, scale = 2)
    private BigDecimal cpuPercent;

    @Column(precision = 6, scale = 2)
    private BigDecimal cpuCoreUsage;

    @Column(nullable = false)
    private Long hostCpuUsageTotal;

    @Column(nullable = false)
    private Long cpuUsageTotal;

    @Column(nullable = false)
    private Long cpuUser;

    @Column(nullable = false)
    private Long cpuSystem;

    @Column(nullable = false)
    private Long cpuQuota;

    @Column(nullable = false)
    private Long cpuPeriod;

    @Column(nullable = false)
    private Integer onlineCpus;

    @Column(nullable = false)
    private Long throttlingPeriods;

    @Column(nullable = false)
    private Long throttledPeriods;

    @Column(nullable = false)
    private Long throttledTime;

    // Memory 관련
    @Column(nullable = false, precision = 6, scale = 2)
    private BigDecimal memPercent;

    @Column(nullable = false)
    private Long memUsage;

    @Column(nullable = false)
    private Long memMaxUsage;

    // Block I/O 관련
    @Column(nullable = false)
    private Long blkRead;

    @Column(nullable = false)
    private Long blkWrite;

    @Column(nullable = false)
    private Long blkReadPerSec;

    @Column(nullable = false)
    private Long blkWritePerSec;

    // Network 관련
    @Column(nullable = false)
    private Long rxBytes;

    @Column(nullable = false)
    private Long txBytes;

    @Column(nullable = false)
    private Long rxPackets;

    @Column(nullable = false)
    private Long txPackets;

    @Column(nullable = false)
    private Long networkTotalBytes;

    @Column(nullable = false)
    private Long rxBytesPerSec;

    @Column(nullable = false)
    private Long txBytesPerSec;

    @Column(nullable = false)
    private Long rxPps;

    @Column(nullable = false)
    private Long txPps;

    @Column(precision = 6, scale = 2)
    private BigDecimal rxFailureRate;

    @Column(precision = 6, scale = 2)
    private BigDecimal txFailureRate;

    @Column(nullable = false)
    private Integer rxErrors;

    @Column(nullable = false)
    private Integer txErrors;

    @Column(nullable = false)
    private Integer rxDropped;

    @Column(nullable = false)
    private Integer txDropped;

    // 컨테이너가 실행 중 생성/수정한 데이터의 크기
    @Column(nullable = false)
    private Long sizeRw;

    // 컨테이너의 전체 파일시스템 크기 (bytes)
    @Column(nullable = false)
    private Long sizeRootFs;

    // 메트릭 수집 시간 (Agent에서 실제로 수집한 시간)
    @Id
    @Column(nullable = false)
    private LocalDateTime collectedAt;

    // DB 저장 시간 (Backend에서 INSERT한 시간)
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ContainerStatsLog of(Container container, ContainerStatsLog statsLog){
        return ContainerStatsLog.builder()
                .container(container)
                .containerHash(statsLog.getContainerHash())
                .state(statsLog.getState())
                .health(statsLog.getHealth())
                .collectedAt(statsLog.getCollectedAt())
                .cpuPercent(statsLog.getCpuPercent())
                .cpuCoreUsage(statsLog.getCpuCoreUsage())
                .hostCpuUsageTotal(statsLog.getHostCpuUsageTotal())
                .cpuUsageTotal(statsLog.getCpuUsageTotal())
                .cpuUser(statsLog.getCpuUser())
                .cpuSystem(statsLog.getCpuSystem())
                .cpuQuota(statsLog.getCpuQuota())
                .cpuPeriod(statsLog.getCpuPeriod())
                .onlineCpus(statsLog.getOnlineCpus())
                .throttlingPeriods(statsLog.getThrottlingPeriods())
                .throttledPeriods(statsLog.getThrottledPeriods())
                .throttledTime(statsLog.getThrottledTime())
                .memPercent(statsLog.getMemPercent())
                .memUsage(statsLog.getMemUsage())
                .memMaxUsage(statsLog.getMemMaxUsage())
                .blkRead(statsLog.getBlkRead())
                .blkWrite(statsLog.getBlkWrite())
                .blkReadPerSec(statsLog.getBlkReadPerSec())
                .blkWritePerSec(statsLog.getBlkWritePerSec())
                .rxBytes(statsLog.getRxBytes())
                .txBytes(statsLog.getTxBytes())
                .rxPackets(statsLog.getRxPackets())
                .txPackets(statsLog.getTxPackets())
                .networkTotalBytes(statsLog.getNetworkTotalBytes())
                .rxBytesPerSec(statsLog.getRxBytesPerSec())
                .txBytesPerSec(statsLog.getTxBytesPerSec())
                .rxPps(statsLog.getRxPps())
                .txPps(statsLog.getTxPps())
                .rxFailureRate(statsLog.getRxFailureRate())
                .txFailureRate(statsLog.getTxFailureRate())
                .rxErrors(statsLog.getRxErrors())
                .txErrors(statsLog.getTxErrors())
                .rxDropped(statsLog.getRxDropped())
                .txDropped(statsLog.getTxDropped())
                .sizeRw(statsLog.getSizeRw())
                .sizeRootFs(statsLog.getSizeRootFs())
                .build();
    }
}
