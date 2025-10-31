package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsRequest;
import com.monito.domains.container.dto.response.*;
import com.monito.domains.container.dto.response.metrics.*;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.Duration;
import com.monito.domains.container.domain.ContainerState;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContainerServiceImpl implements ContainerService {

    private final ContainerRepository containerRepository;
    private final ContainerStatsLogRepository containerStatsLogRepository;
    private final ContainerLogRepository containerLogRepository;

    @Override
    public List<ContainerSummaryResponseDTO> getContainerList(){
        List<Container> containers = containerRepository.findAll();

        return containers.stream()
                .flatMap(container -> {
                    Agent agent = container.getAgent();
                    return containerStatsLogRepository.findLatestByContainerHash(container.getContainerHash())
                            .map(statsLog -> ContainerSummaryResponseDTO.of(agent, container, statsLog))
                            .stream();
                })
                .toList();
    }

    @Override
    public ContainerDetailResponseDTO getContainerMetrics(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 조회
        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.DATA_NOT_FOUND));

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();

        // 3. 해당 기간의 통계 로그 조회
        List<ContainerStatsLog> statsLogs = containerStatsLogRepository.findByContainerIdAndTimeRange(
                containerId, startTime, endTime
        );

        // 4. 컨테이너 기본 정보 생성
        ContainerStatsLog latestLog = statsLogs.isEmpty() ? null : statsLogs.get(statsLogs.size() - 1);
        ContainerInfoDTO containerInfo = buildContainerInfo(container, latestLog);

        // 5. 메트릭 DTO 생성
        CpuMetricsDTO cpu = buildCpuMetrics(statsLogs, container);
        MemoryMetricsDTO memory = buildMemoryMetrics(statsLogs, container);
        NetworkMetricsDTO network = buildNetworkMetrics(statsLogs);

        // 6. 응답 생성
        return ContainerDetailResponseDTO.builder()
                .container(containerInfo)
                .cpu(cpu)
                .memory(memory)
                .network(network)
                .startTime(startTime)
                .endTime(endTime)
                .dataPoints(statsLogs.size())
                .build();
    }

    @Override
    public ContainerLogsResponseDTO getContainerLogs(Long containerId, ContainerLogsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. size + 1개 조회 (hasMore 판단용)
        int requestSize = request.getSize();
        PageRequest pageRequest = PageRequest.of(0, requestSize + 1);

        List<ContainerLog> logs;

        // 3. 초기 로드 vs 커서 기반
        if (request.isInitialLoad()) {
            // 초기 로드
            LocalDateTime startTime = request.getCalculatedStartTime();
            LocalDateTime endTime = request.getCalculatedEndTime();

            logs = (request.getLogSource() != null)
                    ? containerLogRepository.findInitialLogsWithSource(containerId, startTime, endTime, request.getLogSource(), pageRequest)
                    : containerLogRepository.findInitialLogs(containerId, startTime, endTime, pageRequest);
        } else {
            // 커서 기반
            logs = (request.getLogSource() != null)
                    ? containerLogRepository.findLogsAfterCursorWithSource(containerId, request.getLastLogId(), request.getLastLoggedAt(), request.getLogSource(), pageRequest)
                    : containerLogRepository.findLogsAfterCursor(containerId, request.getLastLogId(), request.getLastLoggedAt(), pageRequest);
        }

        // 4. hasMore 판단 및 실제 반환할 로그 분리
        boolean hasMore = logs.size() > requestSize;
        List<ContainerLog> actualLogs = hasMore ? logs.subList(0, requestSize) : logs;

        // 5. DTO 변환
        List<ContainerLogEntryDTO> logEntries = actualLogs.stream()
                .map(ContainerLogEntryDTO::from)
                .toList();

        // 6. 다음 커서 정보
        Long lastLogId = null;
        LocalDateTime lastLoggedAt = null;
        if (!actualLogs.isEmpty()) {
            ContainerLog lastLog = actualLogs.get(actualLogs.size() - 1);
            lastLogId = lastLog.getId();
            lastLoggedAt = lastLog.getLoggedAt();
        }

        return ContainerLogsResponseDTO.builder()
                .logs(logEntries)
                .lastLogId(lastLogId)
                .lastLoggedAt(lastLoggedAt)
                .hasMore(hasMore)
                .returnedCount(actualLogs.size())
                .requestedSize(requestSize)
                .build();
    }

    // ===== Private Helper Methods =====

    private ContainerInfoDTO buildContainerInfo(Container container, ContainerStatsLog latestLog) {
        ContainerState effectiveState = null;
        if (latestLog != null) {
            boolean stale = Duration.between(
                    latestLog.getCollectedAt(),
                    LocalDateTime.now()
            ).getSeconds() > 30;
            effectiveState = stale ? ContainerState.UNKNOWN : latestLog.getState();
        }

        return ContainerInfoDTO.builder()
                .containerId(container.getId())
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .agentName(container.getAgent().getAgentName())
                .imageName(container.getImageName())
                .imageSize(container.getImageSize())
                .state(effectiveState)
                .build();
    }

    private CpuMetricsDTO buildCpuMetrics(List<ContainerStatsLog> logs, Container container) {
        List<TimeSeriesDataDTO> cpuPercent = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(log.getCpuPercent())
                        .build())
                .toList();

        List<TimeSeriesDataDTO> cpuCoreUsage = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(log.getCpuCoreUsage())
                        .build())
                .toList();

        ContainerStatsLog latest = logs.isEmpty() ? null : logs.get(logs.size() - 1);

        // Throttle Rate 계산
        BigDecimal throttleRate = null;
        if (latest != null && latest.getThrottlingPeriods() > 0) {
            throttleRate = BigDecimal.valueOf(latest.getThrottledPeriods())
                    .divide(BigDecimal.valueOf(latest.getThrottlingPeriods()), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        return CpuMetricsDTO.builder()
                .cpuPercent(cpuPercent)
                .cpuCoreUsage(cpuCoreUsage)
                .currentCpuPercent(latest != null ? latest.getCpuPercent() : null)
                .currentCpuCoreUsage(latest != null ? latest.getCpuCoreUsage() : null)
                .hostCpuUsageTotal(latest != null ? latest.getHostCpuUsageTotal() : null)
                .cpuUsageTotal(latest != null ? latest.getCpuUsageTotal() : null)
                .cpuUser(latest != null ? latest.getCpuUser() : null)
                .cpuSystem(latest != null ? latest.getCpuSystem() : null)
                .cpuQuota(latest != null ? latest.getCpuQuota() : null)
                .cpuPeriod(latest != null ? latest.getCpuPeriod() : null)
                .onlineCpus(latest != null ? latest.getOnlineCpus() : null)
                .cpuLimitCores(container.getCpuLimitCores())
                .throttlingPeriods(latest != null ? latest.getThrottlingPeriods() : null)
                .throttledPeriods(latest != null ? latest.getThrottledPeriods() : null)
                .throttledTime(latest != null ? latest.getThrottledTime() : null)
                .throttleRate(throttleRate)
                .build();
    }

    private MemoryMetricsDTO buildMemoryMetrics(List<ContainerStatsLog> logs, Container container) {
        List<TimeSeriesDataDTO> memoryUsage = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(BigDecimal.valueOf(log.getMemUsage()))
                        .build())
                .toList();

        List<TimeSeriesDataDTO> memoryPercent = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(log.getMemPercent())
                        .build())
                .toList();

        ContainerStatsLog latest = logs.isEmpty() ? null : logs.get(logs.size() - 1);

        return MemoryMetricsDTO.builder()
                .memoryUsage(memoryUsage)
                .memoryPercent(memoryPercent)
                .currentMemoryUsage(latest != null ? latest.getMemUsage() : null)
                .currentMemoryPercent(latest != null ? latest.getMemPercent() : null)
                .memLimit(container.getMemLimit())
                .memMaxUsage(latest != null ? latest.getMemMaxUsage() : null)
                .oomKills(container.getOomKills())
                .build();
    }

    private NetworkMetricsDTO buildNetworkMetrics(List<ContainerStatsLog> logs) {
        List<TimeSeriesDataDTO> rxBytesPerSec = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(BigDecimal.valueOf(log.getRxBytesPerSec()))
                        .build())
                .toList();

        List<TimeSeriesDataDTO> txBytesPerSec = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(BigDecimal.valueOf(log.getTxBytesPerSec()))
                        .build())
                .toList();

        List<TimeSeriesDataDTO> rxPacketsPerSec = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(BigDecimal.valueOf(log.getRxPps()))
                        .build())
                .toList();

        List<TimeSeriesDataDTO> txPacketsPerSec = logs.stream()
                .map(log -> TimeSeriesDataDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .value(BigDecimal.valueOf(log.getTxPps()))
                        .build())
                .toList();

        ContainerStatsLog latest = logs.isEmpty() ? null : logs.get(logs.size() - 1);

        return NetworkMetricsDTO.builder()
                .rxBytesPerSec(rxBytesPerSec)
                .txBytesPerSec(txBytesPerSec)
                .rxPacketsPerSec(rxPacketsPerSec)
                .txPacketsPerSec(txPacketsPerSec)
                .currentRxBytesPerSec(latest != null ? latest.getRxBytesPerSec() : null)
                .currentTxBytesPerSec(latest != null ? latest.getTxBytesPerSec() : null)
                .totalRxBytes(latest != null ? latest.getRxBytes() : null)
                .totalTxBytes(latest != null ? latest.getTxBytes() : null)
                .totalRxPackets(latest != null ? latest.getRxPackets() : null)
                .totalTxPackets(latest != null ? latest.getTxPackets() : null)
                .networkTotalBytes(latest != null ? latest.getNetworkTotalBytes() : null)
                .rxErrors(latest != null ? latest.getRxErrors() : null)
                .txErrors(latest != null ? latest.getTxErrors() : null)
                .rxDropped(latest != null ? latest.getRxDropped() : null)
                .txDropped(latest != null ? latest.getTxDropped() : null)
                .rxFailureRate(latest != null ? latest.getRxFailureRate() : null)
                .txFailureRate(latest != null ? latest.getTxFailureRate() : null)
                .build();
    }

}
