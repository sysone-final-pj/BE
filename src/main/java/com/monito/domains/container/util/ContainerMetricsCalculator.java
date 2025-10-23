package com.monito.domains.container.util;

import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 컨테이너 메트릭 계산 유틸리티
 * - CPU 사용률 계산
 * - Memory 사용률 계산
 * - 네트워크 속도 계산 (Mbps, PPS)
 */
@Component
@Slf4j
public class ContainerMetricsCalculator {

    /**
     * CPU 사용률 계산
     * CPU Percent = (ΔcpuUsage / ΔhostCpuUsage) * 100 * onlineCpus
     */
    public BigDecimal calculateCpuPercent(
            Long currentCpuUsage,
            Long currentHostCpuUsage,
            Long previousCpuUsage,
            Long previousHostCpuUsage,
            Integer onlineCpus
    ) {
        if (previousCpuUsage == null || previousHostCpuUsage == null) {
            return BigDecimal.ZERO;
        }

        long deltaCpu = currentCpuUsage - previousCpuUsage;
        long deltaHost = currentHostCpuUsage - previousHostCpuUsage;

        if (deltaHost == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = BigDecimal.valueOf(deltaCpu)
                .divide(BigDecimal.valueOf(deltaHost), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .multiply(BigDecimal.valueOf(onlineCpus));

        return percent.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Memory 사용률 계산
     * Memory Percent = (memUsage / memLimit) * 100
     */
    public BigDecimal calculateMemPercent(Long memUsage, Long memLimit) {
        if (memLimit == 0) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent = BigDecimal.valueOf(memUsage)
                .divide(BigDecimal.valueOf(memLimit), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return percent.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 네트워크 수신 속도 계산 (Mbps)
     * Mbps = (ΔrxBytes * 8) / (timeDiffSeconds * 1_000_000)
     */
    public Long calculateRxMbps(
            Long currentRxBytes,
            Long previousRxBytes,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousRxBytes == null || previousTime == null) {
            return 0L;
        }

        long deltaBytes = currentRxBytes - previousRxBytes;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        // bytes to Mbps: (bytes * 8) / (seconds * 1_000_000)
        return (deltaBytes * 8) / (timeDiffSeconds * 1_000_000);
    }

    /**
     * 네트워크 송신 속도 계산 (Mbps)
     */
    public Long calculateTxMbps(
            Long currentTxBytes,
            Long previousTxBytes,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousTxBytes == null || previousTime == null) {
            return 0L;
        }

        long deltaBytes = currentTxBytes - previousTxBytes;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        return (deltaBytes * 8) / (timeDiffSeconds * 1_000_000);
    }

    /**
     * 네트워크 수신 패킷 속도 계산 (PPS)
     * 패킷 수를 직접 받지 않으므로, 평균 패킷 크기로 추정
     * 평균 패킷 크기를 1500 bytes로 가정
     */
    public Long calculateRxPps(
            Long currentRxBytes,
            Long previousRxBytes,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousRxBytes == null || previousTime == null) {
            return 0L;
        }

        long deltaBytes = currentRxBytes - previousRxBytes;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        // 평균 패킷 크기 1500 bytes로 가정
        long avgPacketSize = 1500;
        long packetsPerSecond = deltaBytes / (timeDiffSeconds * avgPacketSize);

        return packetsPerSecond;
    }

    /**
     * 네트워크 송신 패킷 속도 계산 (PPS)
     */
    public Long calculateTxPps(
            Long currentTxBytes,
            Long previousTxBytes,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousTxBytes == null || previousTime == null) {
            return 0L;
        }

        long deltaBytes = currentTxBytes - previousTxBytes;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        long avgPacketSize = 1500;
        long packetsPerSecond = deltaBytes / (timeDiffSeconds * avgPacketSize);

        return packetsPerSecond;
    }

    /**
     * 메트릭 계산 후 ContainerStatsLog 생성
     */
    public ContainerStatsLog calculateAndBuild(
            ContainerMetricsRequestDTO metrics,
            ContainerStatsLog previousStats
    ) {
        LocalDateTime now = LocalDateTime.now();

        // CPU 사용률 계산
        BigDecimal cpuPercent = calculateCpuPercent(
                metrics.getCpuUsageTotal(),
                metrics.getHostCpuUsageTotal(),
                previousStats != null ? previousStats.getCpuUsageTotal() : null,
                previousStats != null ? previousStats.getHostCpuUsageTotal() : null,
                metrics.getOnlineCpus()
        );

        // Memory 사용률 계산
        BigDecimal memPercent = calculateMemPercent(
                metrics.getMemUsage(),
                metrics.getMemLimit()
        );

        // 네트워크 속도 계산
        Long rxMbps = calculateRxMbps(
                metrics.getRxBytes(),
                previousStats != null ? previousStats.getRxBytes() : null,
                now,
                previousStats != null ? previousStats.getCreatedAt() : null
        );

        Long txMbps = calculateTxMbps(
                metrics.getTxBytes(),
                previousStats != null ? previousStats.getTxBytes() : null,
                now,
                previousStats != null ? previousStats.getCreatedAt() : null
        );

        Long rxPps = calculateRxPps(
                metrics.getRxBytes(),
                previousStats != null ? previousStats.getRxBytes() : null,
                now,
                previousStats != null ? previousStats.getCreatedAt() : null
        );

        Long txPps = calculateTxPps(
                metrics.getTxBytes(),
                previousStats != null ? previousStats.getTxBytes() : null,
                now,
                previousStats != null ? previousStats.getCreatedAt() : null
        );

        return ContainerStatsLog.builder()
                .containerHash(metrics.getContainerHash())
                .state(metrics.getState())
                // CPU 계산 값
                .cpuPercent(cpuPercent)
                // CPU raw 값
                .hostCpuUsageTotal(metrics.getHostCpuUsageTotal())
                .cpuUsageTotal(metrics.getCpuUsageTotal())
                .cpuUser(metrics.getCpuUser())
                .cpuSystem(metrics.getCpuSystem())
                .cpuQuota(metrics.getCpuQuota())
                .cpuPeriod(metrics.getCpuPeriod())
                .cpuLimit(metrics.getCpuLimit())
                .onlineCpus(metrics.getOnlineCpus())
                .throttlingPeriods(metrics.getThrottlingPeriods())
                .throttledPeriods(metrics.getThrottledPeriods())
                .throttledTime(metrics.getThrottledTime())
                .oomKills(metrics.getOomKills())
                // Memory 계산 값
                .memPercent(memPercent)
                // Memory raw 값
                .memUsage(metrics.getMemUsage())
                .memLimit(metrics.getMemLimit())
                .memMaxUsage(metrics.getMemMaxUsage())
                .memRss(metrics.getMemRss())
                .memCache(metrics.getMemCache())
                // Block I/O
                .blkRead(metrics.getBlkRead())
                .blkWrite(metrics.getBlkWrite())
                // Network 계산 값
                .rxMbps(rxMbps)
                .txMbps(txMbps)
                .rxPps(rxPps)
                .txPps(txPps)
                // Network raw 값
                .rxBytes(metrics.getRxBytes())
                .txBytes(metrics.getTxBytes())
                .rxErrors(metrics.getRxErrors())
                .txErrors(metrics.getTxErrors())
                .rxDropped(metrics.getRxDropped())
                .txDropped(metrics.getTxDropped())
                .build();
    }
}