package com.monito.domains.container.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.monito.domains.container.domain.ContainerState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Agent가 보내는 개별 컨테이너 메트릭 (중첩 구조 그대로)
 * {
 *   "containerHash": "...",
 *   "containerName": "...",
 *   "state": "running",
 *   "cpu": {...},
 *   "memory": {...},
 *   "network": {...},
 *   "blockIO": {...}
 * }
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerMetricsRawRequestDTO {
    private String containerHash;
    private String containerName;
    private String status;
    private String state;

    private CpuMetricsRequestDTO cpu;
    private MemoryMetricsRequestDTO memory;
    private NetworkMetricsRequestDTO network;
    @JsonProperty("blockIO")
    private BlockIOMetricsRequestDTO blockIO;

    /**
     * Flat 구조의 ContainerMetricsRequestDTO로 변환
     */
    public ContainerMetricsRequestDTO toFlatDTO() {
        return ContainerMetricsRequestDTO.builder()
                .containerHash(containerHash)
                .containerName(containerName)
                .state(parseState(state))
                // CPU
                .hostCpuUsageTotal(cpu != null ? cpu.getSystemCpuUsage() : null)
                .cpuUsageTotal(cpu != null ? cpu.getCpuUsageTotal() : null)
                .cpuUser(cpu != null ? cpu.getCpuUser() : null)
                .cpuSystem(cpu != null ? cpu.getCpuSystem() : null)
                .cpuQuota(cpu != null ? cpu.getCpuQuota() : 0L)
                .cpuPeriod(cpu != null ? cpu.getCpuPeriod() : 0L)
                .cpuLimit(cpu != null ? calculateCpuLimit(cpu.getCpuQuota(), cpu.getCpuPeriod()) : 0L)
                .onlineCpus(cpu != null ? cpu.getOnlineCpus() : 1)
                .throttlingPeriods(cpu != null ? cpu.getThrottlingPeriods() : 0L)
                .throttledPeriods(cpu != null ? cpu.getThrottledPeriods() : 0L)
                .throttledTime(cpu != null ? cpu.getThrottledTime() : 0L)
                .oomKills(0) // Agent에서 제공하지 않음
                // Memory
                .memUsage(memory != null ? memory.getMemUsage() : null)
                .memLimit(memory != null ? memory.getMemLimit() : null)
                .memMaxUsage(memory != null ? memory.getMemMaxUsage() : null)
                .memRss(0L) // Agent에서 제공하지 않음
                .memCache(0L) // Agent에서 제공하지 않음
                // Network
                .rxBytes(network != null ? network.getRxBytes() : null)
                .txBytes(network != null ? network.getTxBytes() : null)
                .rxErrors(network != null ? network.getRxErrors() : null)
                .txErrors(network != null ? network.getTxErrors() : null)
                .rxDropped(network != null ? network.getRxDropped() : null)
                .txDropped(network != null ? network.getTxDropped() : null)
                // Block I/O
                .blkRead(blockIO != null ? blockIO.getBlkRead() : null)
                .blkWrite(blockIO != null ? blockIO.getBlkWrite() : null)
                .build();
    }

    private ContainerState parseState(String state) {
        if (state == null) return ContainerState.DEAD;

        try {
            return ContainerState.valueOf(state.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ContainerState.DEAD;
        }
    }

    private Long calculateCpuLimit(Long cpuQuota, Long cpuPeriod) {
        if (cpuQuota == null || cpuPeriod == null || cpuQuota <= 0 || cpuPeriod <= 0) {
            return 0L;
        }
        return cpuQuota;
    }
}