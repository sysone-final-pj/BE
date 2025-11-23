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
     * 실제 CPU 코어 사용량 계산 (코어 단위)
     * Core Usage = ΔcpuUsage / 시간차
     * 예: 0.5코어 = 0.5개의 CPU 코어를 사용 중
     *
     * 정밀도: 소수점 6자리까지 계산 (percent 계산 정확도 향상)
     */
    public BigDecimal calculateCoreUsage(
            Long currentCpuUsage,
            Long previousCpuUsage,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousCpuUsage == null || previousTime == null) {
            log.info("[CPU CALC] 이전 데이터 null -> 0 코어 반환");
            return BigDecimal.ZERO;
        }

        long deltaCpu = currentCpuUsage - previousCpuUsage;
        long timeDiffNanos = Duration.between(previousTime, currentTime).toNanos();

        if (timeDiffNanos == 0) {
            log.warn("[CPU CALC] 시간차가 0 -> 계산 불가, 0 코어 반환");
            return BigDecimal.ZERO;
        }

        // 실제 코어 사용량 = deltaCpu / timeDiff
        // 내부 계산은 고정밀도로, DB 저장/표시 시에만 반올림
        BigDecimal coreUsage = BigDecimal.valueOf(deltaCpu)
                .divide(BigDecimal.valueOf(timeDiffNanos), 10, RoundingMode.HALF_UP);

        log.debug("[CPU CALC] 실제 코어 사용량: {} cores (deltaCpu={}ns, timeDiff={}ns)",
                coreUsage, deltaCpu, timeDiffNanos);

        return coreUsage;
    }

    /**
     * CPU 사용률 계산 (제한 기준)
     * - CPU 제한 있음: (실제 사용량 / CPU 제한) × 100
     * - CPU 제한 없음: (실제 사용량 / 전체 코어) × 100
     *
     * 예시:
     * - 4코어 시스템, 제한 0.5코어, 실제 사용 0.5코어 -> 100% (제한 대비)
     * - 4코어 시스템, 제한 없음, 실제 사용 2코어 -> 50% (전체 대비)
     */
    public BigDecimal calculateCpuPercent(
            BigDecimal actualCoreUsage,
            BigDecimal cpuLimitCores,
            Integer onlineCpus,
            Boolean isCpuUnlimited
    ) {
        if (actualCoreUsage == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal percent;

        // CPU 제한이 있는 경우: 제한 대비 사용률
        if (isCpuUnlimited != null && !isCpuUnlimited && cpuLimitCores != null && cpuLimitCores.compareTo(BigDecimal.ZERO) > 0) {
            percent = actualCoreUsage
                    .divide(cpuLimitCores, 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            log.info("[CPU CALC] 제한 기준 CPU %: {} (사용={} cores, 제한={} cores)",
                    percent.setScale(2, RoundingMode.HALF_UP), actualCoreUsage, cpuLimitCores);

            // 100% 캐핑 (사용자 경험 개선)
            if (percent.compareTo(BigDecimal.valueOf(100)) > 0) {
                log.debug("[CPU CALC] CPU 사용률 100% 초과 ({}%) -> 100%로 캐핑", percent.setScale(2, RoundingMode.HALF_UP));
                percent = BigDecimal.valueOf(100);
            }
        }
        // CPU 제한이 없는 경우: 전체 코어 대비 사용률
        else {
            if (onlineCpus == null || onlineCpus == 0) {
                return BigDecimal.ZERO;
            }

            percent = actualCoreUsage
                    .divide(BigDecimal.valueOf(onlineCpus), 6, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            log.info("[CPU CALC] 전체 코어 기준 CPU %: {} (사용={} cores, 전체={} cores)",
                    percent.setScale(2, RoundingMode.HALF_UP), actualCoreUsage, onlineCpus);
        }

        return percent.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * CPU 제한 계산 (코어 단위)
     * CPU Limit = cpuQuota / cpuPeriod
     * 예: 50000 / 100000 = 0.5 코어
     */
    public BigDecimal calculateCpuLimitCores(Long cpuQuota, Long cpuPeriod) {
        if (cpuQuota == null || cpuPeriod == null || cpuPeriod == 0 || cpuQuota <= 0) {
            return null; // 무제한
        }

        return BigDecimal.valueOf(cpuQuota)
                .divide(BigDecimal.valueOf(cpuPeriod), 2, RoundingMode.HALF_UP);
    }

    /**
     * Throttling 비율 계산 (Period 기반)
     * Throttling % = (throttledPeriods / throttlingPeriods) * 100
     */
    public BigDecimal calculateThrottlingPercent(Long throttledPeriods, Long throttlingPeriods) {
        if (throttlingPeriods == null || throttlingPeriods == 0) {
            return BigDecimal.ZERO;
        }

        return BigDecimal.valueOf(throttledPeriods)
                .divide(BigDecimal.valueOf(throttlingPeriods), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Throttling 비율 계산 (Time 기반 - 더 정확)
     * Throttling % = (throttledTime / totalAvailableTime) * 100
     * totalAvailableTime = timeDiff * onlineCpus (나노초)
     */
    public BigDecimal calculateThrottlingPercentByTime(
            Long throttledTime,
            Long timeDiffNanos,
            Integer onlineCpus
    ) {
        if (throttledTime == null || timeDiffNanos == 0 || onlineCpus == null) {
            return BigDecimal.ZERO;
        }

        long totalAvailableTime = timeDiffNanos * onlineCpus;

        return BigDecimal.valueOf(throttledTime)
                .divide(BigDecimal.valueOf(totalAvailableTime), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
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
     * Memory Max Usage 계산
     * - 이전 최대값과 현재 사용량을 비교하여 더 큰 값 반환
     * - 첫 수집 시에는 현재 사용량을 반환
     */
    public Long calculateMemMaxUsage(Long currentMemUsage, Long previousMemMaxUsage) {
        if (previousMemMaxUsage == null) {
            return currentMemUsage;
        }
        return Math.max(currentMemUsage, previousMemMaxUsage);
    }

    /**
     * 범용 Throughput 계산 (Bytes per second)
     * Network, Block I/O 등 모든 bytes 기반 throughput에 사용
     * Throughput = Δbytes / Δtime
     */
    public Long calculateBytesPerSec(
            Long currentBytes,
            Long previousBytes,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousBytes == null || previousTime == null) {
            return 0L;
        }

        long deltaBytes = currentBytes - previousBytes;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        return deltaBytes / timeDiffSeconds;
    }

    /**
     * 네트워크 장애율 계산 (에러 + 드롭)
     * Failure Rate = (Δerrors + Δdropped) / Δpackets × 100
     *
     * @return 장애율 (0.00 ~ 100.00), 패킷이 없으면 0.00
     */
    public BigDecimal calculateNetworkFailureRate(
            Integer currentErrors,
            Integer previousErrors,
            Integer currentDropped,
            Integer previousDropped,
            Long currentPackets,
            Long previousPackets
    ) {
        if (previousErrors == null || previousDropped == null || previousPackets == null) {
            return BigDecimal.ZERO;
        }

        long deltaErrors = currentErrors - previousErrors;
        long deltaDropped = currentDropped - previousDropped;
        long deltaPackets = currentPackets - previousPackets;

        // 패킷이 없으면 0% (의미 없는 지표)
        if (deltaPackets == 0) {
            return BigDecimal.ZERO;
        }

        long totalFailures = deltaErrors + deltaDropped;

        return BigDecimal.valueOf(totalFailures)
                .divide(BigDecimal.valueOf(deltaPackets), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 네트워크 수신 패킷 속도 계산 (PPS - Packets Per Second)
     * 실제 패킷 수를 사용하여 정확한 PPS 계산
     */
    public Long calculateRxPps(
            Long currentRxPackets,
            Long previousRxPackets,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousRxPackets == null || previousTime == null) {
            return 0L;
        }

        long deltaPackets = currentRxPackets - previousRxPackets;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        return deltaPackets / timeDiffSeconds;
    }

    /**
     * 네트워크 송신 패킷 속도 계산 (PPS - Packets Per Second)
     * 실제 패킷 수를 사용하여 정확한 PPS 계산
     */
    public Long calculateTxPps(
            Long currentTxPackets,
            Long previousTxPackets,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousTxPackets == null || previousTime == null) {
            return 0L;
        }

        long deltaPackets = currentTxPackets - previousTxPackets;
        long timeDiffSeconds = Duration.between(previousTime, currentTime).getSeconds();

        if (timeDiffSeconds == 0) {
            return 0L;
        }

        return deltaPackets / timeDiffSeconds;
    }

    /**
     * 메트릭 계산 후 ContainerStatsLog 생성
     */
    public ContainerStatsLog calculateAndBuild(
            ContainerMetricsRequestDTO metrics,
            ContainerStatsLog previousStats,
            BigDecimal cpuLimitCores,
            Boolean isCpuUnlimited
    ) {
        // Agent에서 수집한 시간 사용
        LocalDateTime collectedAt = metrics.getCollectedAt();

        // 1. 실제 CPU 코어 사용량 계산 (코어 단위)
        BigDecimal cpuCoreUsage = calculateCoreUsage(
                metrics.getCpuUsageTotal(),
                previousStats != null ? previousStats.getCpuUsageTotal() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        // 2. CPU 사용률 계산 (제한 기준 또는 전체 코어 기준)
        BigDecimal cpuPercent = calculateCpuPercent(
                cpuCoreUsage,
                cpuLimitCores,
                metrics.getOnlineCpus(),
                isCpuUnlimited
        );

        // Memory 사용률 계산
        BigDecimal memPercent = calculateMemPercent(
                metrics.getMemUsage(),
                metrics.getMemLimit()
        );

        // Memory Max Usage 계산 (이전 최대값과 현재 사용량 비교)
        Long memMaxUsage = calculateMemMaxUsage(
                metrics.getMemUsage(),
                previousStats != null ? previousStats.getMemMaxUsage() : null
        );

        // 네트워크 속도 계산 (Bytes per second)
        Long rxBytesPerSec = calculateBytesPerSec(
                metrics.getRxBytes(),
                previousStats != null ? previousStats.getRxBytes() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        Long txBytesPerSec = calculateBytesPerSec(
                metrics.getTxBytes(),
                previousStats != null ? previousStats.getTxBytes() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        // Block I/O 속도 계산 (Bytes per second)
        Long blkReadPerSec = calculateBytesPerSec(
                metrics.getBlkRead(),
                previousStats != null ? previousStats.getBlkRead() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        Long blkWritePerSec = calculateBytesPerSec(
                metrics.getBlkWrite(),
                previousStats != null ? previousStats.getBlkWrite() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        // PPS 계산 (실제 패킷 수 사용)
        Long rxPps = calculateRxPps(
                metrics.getRxPackets(),
                previousStats != null ? previousStats.getRxPackets() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        Long txPps = calculateTxPps(
                metrics.getTxPackets(),
                previousStats != null ? previousStats.getTxPackets() : null,
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        // 네트워크 장애율 계산 (에러 + 드롭)
        BigDecimal rxFailureRate = calculateNetworkFailureRate(
                metrics.getRxErrors(),
                previousStats != null ? previousStats.getRxErrors() : null,
                metrics.getRxDropped(),
                previousStats != null ? previousStats.getRxDropped() : null,
                metrics.getRxPackets(),
                previousStats != null ? previousStats.getRxPackets() : null
        );

        BigDecimal txFailureRate = calculateNetworkFailureRate(
                metrics.getTxErrors(),
                previousStats != null ? previousStats.getTxErrors() : null,
                metrics.getTxDropped(),
                previousStats != null ? previousStats.getTxDropped() : null,
                metrics.getTxPackets(),
                previousStats != null ? previousStats.getTxPackets() : null
        );

        return ContainerStatsLog.builder()
                .containerHash(metrics.getContainerHash())
                .state(metrics.getState())
                .health(metrics.getHealth())
                .collectedAt(collectedAt)
                // CPU 계산 값
                .cpuPercent(cpuPercent)
                .cpuCoreUsage(cpuCoreUsage)
                // CPU raw 값
                .hostCpuUsageTotal(metrics.getHostCpuUsageTotal())
                .cpuUsageTotal(metrics.getCpuUsageTotal())
                .cpuUser(metrics.getCpuUser())
                .cpuSystem(metrics.getCpuSystem())
                .cpuQuota(metrics.getCpuQuota())
                .cpuPeriod(metrics.getCpuPeriod())
                .onlineCpus(metrics.getOnlineCpus())
                .throttlingPeriods(metrics.getThrottlingPeriods())
                .throttledPeriods(metrics.getThrottledPeriods())
                .throttledTime(metrics.getThrottledTime())
                // Memory 계산 값
                .memPercent(memPercent)
                // Memory raw 값
                .memUsage(metrics.getMemUsage())
                .memMaxUsage(memMaxUsage)
                // Block I/O raw 값
                .blkRead(metrics.getBlkRead())
                .blkWrite(metrics.getBlkWrite())
                // Block I/O 계산 값
                .blkReadPerSec(blkReadPerSec)
                .blkWritePerSec(blkWritePerSec)
                // Network 계산 값
                .rxBytesPerSec(rxBytesPerSec)
                .txBytesPerSec(txBytesPerSec)
                .rxPps(rxPps)
                .txPps(txPps)
                .rxFailureRate(rxFailureRate)
                .txFailureRate(txFailureRate)
                // Network raw 값
                .rxBytes(metrics.getRxBytes())
                .txBytes(metrics.getTxBytes())
                .rxPackets(metrics.getRxPackets())
                .txPackets(metrics.getTxPackets())
                .networkTotalBytes(metrics.getRxBytes() + metrics.getTxBytes())
                .rxErrors(metrics.getRxErrors())
                .txErrors(metrics.getTxErrors())
                .rxDropped(metrics.getRxDropped())
                .txDropped(metrics.getTxDropped())
                // Storage raw 값
                .sizeRw(metrics.getSizeRw())
                .sizeRootFs(metrics.getSizeRootFs())
                .build();
    }
}