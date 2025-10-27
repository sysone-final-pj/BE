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
     * CPU 사용률 계산 (시간 기반 - Docker CLI 방식)
     * CPU Percent = (ΔcpuUsage / (시간차 × 10^9)) × 100 / onlineCpus
     * 참고: Docker CLI 공식 계산 방식
     * - cpuUsage는 나노초 단위 누적값
     * - 실제 경과 시간으로 나누어 정확한 % 계산
     */
    public BigDecimal calculateCpuPercent(
            Long currentCpuUsage,
            Long currentHostCpuUsage,
            Long previousCpuUsage,
            Long previousHostCpuUsage,
            Integer onlineCpus,
            LocalDateTime currentTime,
            LocalDateTime previousTime
    ) {
        if (previousCpuUsage == null || previousTime == null) {
            log.info("[CPU CALC] 이전 데이터 null -> 0% 반환");
            return BigDecimal.ZERO;
        }

        long deltaCpu = currentCpuUsage - previousCpuUsage;
        long deltaHost = currentHostCpuUsage - previousHostCpuUsage;

        // 실제 경과 시간 (나노초)
        long timeDiffNanos = Duration.between(previousTime, currentTime).toNanos();

        log.info("[CPU CALC] Delta 계산 - deltaCpu: {}, deltaHost: {}, timeDiff: {}ns ({}s)",
                deltaCpu, deltaHost, timeDiffNanos, timeDiffNanos / 1_000_000_000.0);

        if (timeDiffNanos == 0) {
            log.warn("[CPU CALC] 시간차가 0 -> 계산 불가, 0% 반환");
            return BigDecimal.ZERO;
        }

        // Docker CLI 방식: (ΔcpuUsage / timeDiff) * 100 / onlineCpus
        BigDecimal percent = BigDecimal.valueOf(deltaCpu)
                .divide(BigDecimal.valueOf(timeDiffNanos), 10, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(onlineCpus), 6, RoundingMode.HALF_UP);

        BigDecimal result = percent.setScale(2, RoundingMode.HALF_UP);
        log.info("[CPU CALC] 계산 완료 - CPU %: {} (deltaCpu={}ns, timeDiff={}ns)", result, deltaCpu, timeDiffNanos);

        return result;
    }

    /**
     * Core 사용량 계산 (코어 단위)
     * Core Usage = cpuPercent * onlineCpus / 100
     * 예: 15% × 2코어 = 0.3 코어
     */
    public BigDecimal calculateCoreUsage(BigDecimal cpuPercent, Integer onlineCpus) {
        if (cpuPercent == null || onlineCpus == null) {
            return BigDecimal.ZERO;
        }

        return cpuPercent
                .multiply(BigDecimal.valueOf(onlineCpus))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    /**
     * CPU 제한 계산 (코어 단위)
     * CPU Limit = cpuQuota / cpuPeriod
     * 예: 50000 / 100000 = 0.5 코어
     */
    // todo: responseDTO 혹은 statsLogs 테이블에 CpuLimitCore 추가 (DB테이블에 데이터 추가가 적합해 보임)
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
    // todo: responseDTO 혹은 statsLogs 테이블에 CpuLimitCore 추가 (DB테이블에 데이터 추가가 적합해 보임)
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
    // todo: responseDTO 혹은 statsLogs 테이블에 CpuLimitCore 추가 (DB테이블에 데이터 추가가 적합해 보임)
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
            ContainerStatsLog previousStats
    ) {
        // Agent에서 수집한 시간 사용
        LocalDateTime collectedAt = metrics.getCollectedAt();

        // CPU 사용률 계산 (시간 기반)
        BigDecimal cpuPercent = calculateCpuPercent(
                metrics.getCpuUsageTotal(),
                metrics.getHostCpuUsageTotal(),
                previousStats != null ? previousStats.getCpuUsageTotal() : null,
                previousStats != null ? previousStats.getHostCpuUsageTotal() : null,
                metrics.getOnlineCpus(),
                collectedAt,
                previousStats != null ? previousStats.getCollectedAt() : null
        );

        // CPU 코어 사용량 계산 (코어 단위)
        BigDecimal cpuCoreUsage = calculateCoreUsage(
                cpuPercent,
                metrics.getOnlineCpus()
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
                .memLimit(metrics.getMemLimit())
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
                .rxErrors(metrics.getRxErrors())
                .txErrors(metrics.getTxErrors())
                .rxDropped(metrics.getRxDropped())
                .txDropped(metrics.getTxDropped())
                .build();
    }
}