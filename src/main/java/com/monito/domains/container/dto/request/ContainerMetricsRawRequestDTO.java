package com.monito.domains.container.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.monito.domains.container.domain.ContainerState;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

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
 *   "storage": {...}
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
    private LocalDateTime collectedAt;  // Agent 메트릭 수집 시간

    private CpuMetricsRequestDTO cpu;
    private MemoryMetricsRequestDTO memory;
    private NetworkMetricsRequestDTO network;
    @JsonProperty("blockIO")
    private BlockIOMetricsRequestDTO blockIO;
    private StorageMetricsRequestDTO storage;

    /**
     * Flat 구조의 ContainerMetricsRequestDTO로 변환
     */
    public ContainerMetricsRequestDTO toFlatDTO() {
        return ContainerMetricsRequestDTO.builder()
                .containerHash(containerHash)
                .containerName(containerName)
                .state(parseState(state))
                .collectedAt(collectedAt)  // Agent 수집 시간 전달
                // CPU
                .hostCpuUsageTotal(cpu != null ? cpu.getSystemCpuUsage() : null)
                .cpuUsageTotal(cpu != null ? cpu.getCpuUsageTotal() : null)
                .cpuUser(cpu != null ? cpu.getCpuUser() : null)
                .cpuSystem(cpu != null ? cpu.getCpuSystem() : null)
                .cpuQuota(cpu != null ? cpu.getCpuQuota() : 0L)
                .cpuPeriod(cpu != null ? cpu.getCpuPeriod() : 0L)
                .onlineCpus(cpu != null ? cpu.getOnlineCpus() : 1)
                .throttlingPeriods(cpu != null ? cpu.getThrottlingPeriods() : 0L)
                .throttledPeriods(cpu != null ? cpu.getThrottledPeriods() : 0L)
                .throttledTime(cpu != null ? cpu.getThrottledTime() : 0L)
                // Memory
                .memUsage(memory != null ? memory.getMemUsage() : null)
                .memLimit(memory != null ? memory.getMemLimit() : null)
                // Network
                .rxBytes(network != null ? network.getRxBytes() : null)
                .txBytes(network != null ? network.getTxBytes() : null)
                .rxPackets(network != null ? network.getRxPackets() : null)
                .txPackets(network != null ? network.getTxPackets() : null)
                .rxErrors(network != null ? network.getRxErrors() : null)
                .txErrors(network != null ? network.getTxErrors() : null)
                .rxDropped(network != null ? network.getRxDropped() : null)
                .txDropped(network != null ? network.getTxDropped() : null)
                // Block I/O
                .blkRead(blockIO != null ? blockIO.getBlkRead() : null)
                .blkWrite(blockIO != null ? blockIO.getBlkWrite() : null)
                // Storage
                .sizeRw(storage != null ? storage.getSizeRw() : 0L)
                .sizeRootFs(storage != null ? storage.getSizeRootFs() : 0L)
                .imageSize(storage != null ? storage.getImageSize() : null)
                .imageName(storage != null ? storage.getImageName() : null)
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
}