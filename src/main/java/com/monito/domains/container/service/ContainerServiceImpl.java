package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.domain.*;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsRequest;
import com.monito.domains.container.dto.request.ContainerSnapshotRequestDTO;
import com.monito.domains.container.dto.response.*;
import com.monito.domains.container.dto.response.metrics.*;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.util.CpuMetricsCalculator;
import com.monito.global.cache.*;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ContainerServiceImpl implements ContainerService {

    private final ContainerRepository containerRepository;
    private final ContainerStatsLogRepository containerStatsLogRepository;
    private final ContainerLogRepository containerLogRepository;
    private final AgentMetadataCache agentMetadataCache;
    private final AgentRepository agentRepository;
    private final OomEventCache oomEventCache;
    private final CpuMetricsBufferCache cpuMetricsBufferCache;
    private final CpuMetricsCalculator cpuMetricsCalculator;
    private final ContainerSummaryCache containerSummaryCache;

    @Override
    public List<ContainerSummaryResponseDTO> getContainerList(
            String keyword,
            List<ContainerState> states,
            List<ContainerHealth> healths,
            ContainerSortField sortBy,
            Sort.Direction direction
    ) {
        // 1. keyword trim 처리 (빈 문자열은 null로 변환)
        String searchKeyword = keyword != null && !keyword.trim().isEmpty()
                ? keyword.trim()
                : null;

        // 2. Container 검색 (Repository에서 JPQL 쿼리 실행)
        List<Container> containers = containerRepository.findAllWithSearch(searchKeyword);

        // 3. Container → DTO 변환 (최신 StatsLog 포함)
        Stream<ContainerSummaryResponseDTO> dtoStream = containers.stream()
                .map(container -> {
                    Agent agent = container.getAgent();
                    Optional<ContainerStatsLog> statsLogOpt = containerStatsLogRepository.findLatestByContainerHash(container.getContainerHash());

                    // statsLog가 없어도 컨테이너는 포함 (null로 전달)
                    ContainerStatsLog statsLog = statsLogOpt.orElse(null);
                    ContainerSummaryResponseDTO dto = ContainerSummaryResponseDTO.of(container, statsLog);

                    // storageLimit가 0이면 Agent 전체 디스크 용량으로 변경
                    if (dto.getStorageLimit() == 0 && agent != null) {
                        AgentMetadata metadata = agentMetadataCache.getMetadata(agent.getAgentKey());
                        if (metadata != null && metadata.getHostTotalDiskSpace() != null) {
                            dto = dto.changeStorageLimit(metadata.getHostTotalDiskSpace());
                        }
                    }
                    return dto;
                });

        // 4. state 필터링
        if (states != null && !states.isEmpty()) {
            dtoStream = dtoStream.filter(dto -> states.contains(dto.getState()));
        }

        // 5. health 필터링
        if (healths != null && !healths.isEmpty()) {
            dtoStream = dtoStream.filter(dto -> healths.contains(dto.getHealth()));
        }

        // 6. 정렬
        if (sortBy != null) {
            Comparator<ContainerSummaryResponseDTO> comparator = getComparator(sortBy);
            if (direction == Sort.Direction.DESC) {
                comparator = comparator.reversed();
            }
            dtoStream = dtoStream.sorted(comparator);
        }

        return dtoStream.toList();
    }

    /**
     * 정렬 필드에 따른 Comparator 생성
     */
    private Comparator<ContainerSummaryResponseDTO> getComparator(ContainerSortField sortBy) {
        return switch (sortBy) {
            case AGENT_NAME -> Comparator.comparing(dto -> dto.getAgentName() != null ? dto.getAgentName() : "", String.CASE_INSENSITIVE_ORDER);
            case CONTAINER_HASH -> Comparator.comparing(dto -> dto.getContainerHash() != null ? dto.getContainerHash() : "");
            case CONTAINER_NAME -> Comparator.comparing(dto -> dto.getContainerName() != null ? dto.getContainerName() : "", String.CASE_INSENSITIVE_ORDER);
            case CPU_PERCENT -> Comparator.comparing(dto -> dto.getCpuPercent() != null ? dto.getCpuPercent() : BigDecimal.ZERO);
            case MEM_USAGE -> Comparator.comparing(dto -> dto.getMemUsage() != null ? dto.getMemUsage() : 0L);
            case MEM_LIMIT -> Comparator.comparing(dto -> dto.getMemLimit() != null ? dto.getMemLimit() : 0L);
            case STORAGE_USAGE -> Comparator.comparing(dto -> dto.getSizeRootFs() != null ? dto.getSizeRootFs() : 0L);
            case STORAGE_LIMIT -> Comparator.comparing(dto -> dto.getStorageLimit() != null ? dto.getStorageLimit() : 0L);
            case RX_BYTES -> Comparator.comparing(dto -> dto.getRxBytesPerSec() != null ? dto.getRxBytesPerSec() : 0L);
            case TX_BYTES -> Comparator.comparing(dto -> dto.getTxBytesPerSec() != null ? dto.getTxBytesPerSec() : 0L);
            case STATE -> Comparator.comparing(dto -> dto.getState() != null ? dto.getState().name() : "");
            case HEALTH -> Comparator.comparing(dto -> dto.getHealth() != null ? dto.getHealth().name() : "");
        };
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
        ContainerInfoDTO containerInfo = buildContainerInfo(container);

        // 5. 메트릭 DTO 생성
        CpuMetricsDTO cpu = buildCpuMetrics(statsLogs, container);
        MemoryMetricsDTO memory = buildMemoryMetrics(statsLogs, container);
        NetworkMetricsDTO network = buildNetworkMetrics(statsLogs);
        OomMetricsDTO oom = buildOomMetrics(container, startTime, endTime);

        // 6. 응답 생성
        return ContainerDetailResponseDTO.builder()
                .container(containerInfo)
                .cpu(cpu)
                .memory(memory)
                .network(network)
                .oom(oom)
                .startTime(startTime)
                .endTime(endTime)
                .dataPoints(statsLogs.size())
                .build();
    }

    @Override
    public ContainerLogsResponseDTO getContainerLogs(List<Long> containerIds, ContainerLogsRequest request) {
        // 1. containerIds 검증 (비어있는 리스트는 null로 처리)
        List<Long> validContainerIds = (containerIds == null || containerIds.isEmpty()) ? null : containerIds;

        // 2. 정렬 설정 (기본: LOGGED_AT DESC)
        Sort sort = createLogSort(
                request.getSortBy() != null ? request.getSortBy() : com.monito.domains.container.domain.LogSortField.LOGGED_AT,
                request.getDirection() != null ? request.getDirection() : Sort.Direction.DESC
        );

        // 3. size + 1개 조회 (hasMore 판단용)
        int requestSize = request.getSize();
        PageRequest pageRequest = PageRequest.of(0, requestSize + 1, sort);

        // 4. 초기 로드 시 시간 범위 계산
        LocalDateTime startTime = request.isInitialLoad() ? request.getCalculatedStartTime() : null;
        LocalDateTime endTime = request.isInitialLoad() ? request.getCalculatedEndTime() : null;

        // 5. 통합 메서드로 로그 조회
        List<ContainerLog> logs = containerLogRepository.findLogs(
                validContainerIds,  // null이면 모든 컨테이너, 아니면 지정된 컨테이너들
                request.getLogSource(),
                request.getAgentName(),
                request.getLastLogId(),
                request.getLastLoggedAt(),
                startTime,
                endTime,
                pageRequest
        );

        // 6. hasMore 판단 및 실제 반환할 로그 분리
        boolean hasMore = logs.size() > requestSize;
        List<ContainerLog> actualLogs = hasMore ? logs.subList(0, requestSize) : logs;

        // 7. DTO 변환
        List<ContainerLogEntryDTO> logEntries = actualLogs.stream()
                .map(ContainerLogEntryDTO::from)
                .toList();

        // 8. 다음 커서 정보
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

    /**
     * 로그 정렬 생성 (정렬 필드에 따라 JPQL 경로 매핑)
     */
    private Sort createLogSort(com.monito.domains.container.domain.LogSortField sortBy, Sort.Direction direction) {
        String sortField = switch (sortBy) {
            case LOGGED_AT -> "loggedAt";
            case CONTAINER_NAME -> "container.name";
            case AGENT_NAME -> "container.agent.agentName";
            case LOG_MESSAGE -> "logMessage";
        };

        // 동일 값일 때 id로 추가 정렬 (안정적인 페이징)
        return Sort.by(direction, sortField).and(Sort.by(direction, "id"));
    }

    // ===== Private Helper Methods =====

    private ContainerInfoDTO buildContainerInfo(Container container) {
        ContainerState effectiveState = container.getAgent().getAgentStatus() == AgentStatus.OFFLINE
                ? ContainerState.UNKNOWN : container.getState();

        String agentName = container.getAgent().getAgentName();

        return ContainerInfoDTO.builder()
                .containerId(container.getId())
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .agentName(agentName)
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

        // 통계 불러오기
        List<BigDecimal> samples = cpuMetricsBufferCache.getSamples(container.getId());

        // 요약 통계 계산 및 객체 생성
        CpuMetricsSummaryDTO summary = CpuMetricsSummaryDTO.builder()
                .current(cpuMetricsCalculator.avgLast(samples, 1))
                .avg1m(cpuMetricsCalculator.avgLast(samples, 12))        // 1분
                .avg5m(cpuMetricsCalculator.avgLast(samples, 12 * 5))    // 5분
                .avg15m(cpuMetricsCalculator.avgLast(samples, 12 * 15))  // 15분
                .p95(cpuMetricsCalculator.percentile(samples, 0.95))     // 95th percentile
                .build();

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
                .summary(summary)
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

    @Override
    @Transactional
    public void processContainerStateChange(String agentKey, ContainerSnapshotRequestDTO snapshot) {
        // 1. Agent 조회
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));

        // 2. 상태 파싱
        ContainerState state = parseState(snapshot.getState());

        // 3. 기존 Container 조회
        Container container = containerRepository
                .findByAgentAndContainerHash(agent, snapshot.getContainerHash())
                .orElse(null);

        if (container == null) {
            // 3-1. 신규 컨테이너 생성 (deleted 상태가 아닌 경우만)
            if (state != ContainerState.DELETED) {
                Container newContainer = createContainerFromSnapshot(agent, snapshot, state);
                log.info("새 컨테이너 생성 - Agent: {}, ContainerHash: {}, State: {}",
                        agentKey, snapshot.getContainerHash(), state);

                // 3-1-1. 신규 컨테이너가 처음부터 OOM 상태인 경우 (중복 체크 불필요)
                if (Boolean.TRUE.equals(snapshot.getOomKilled())) {
                    handleOomKillForNewContainer(newContainer, agentKey);
                }
                containerSummaryCache.update(ContainerSummaryResponseDTO.of(newContainer, null));
            }
        } else {
            // 3-2. 기존 컨테이너 업데이트
            if (state == ContainerState.DELETED) {
                // Soft delete 처리
                container.markAsDeleted();
                containerRepository.save(container);
                log.info("컨테이너 삭제 처리 - Agent: {}, ContainerHash: {}",
                        agentKey, snapshot.getContainerHash());
                // 컨테이너 삭제처리 될 경우 캐시 데이터에서도 삭제
                cpuMetricsBufferCache.removeContainer(container.getId());
                oomEventCache.removeContainer(container.getId());
                containerSummaryCache.remove(container.getId());
            } else if(container.getState() != state) {
                container.changeState(state);
                // 상태만 업데이트 (이름이나 이미지 변경 가능성 대응)
                log.debug("컨테이너 상태 변경 - ContainerHash: {}, State: {}",
                        snapshot.getContainerHash(), state);

                // 상태 변경 시 캐시 업데이트 (비활성 상태는 메트릭 0으로 표시)
                ContainerStatsLog latestStats = containerStatsLogRepository
                        .findLatestByContainerHash(container.getContainerHash())
                        .orElse(null);
                containerSummaryCache.update(ContainerSummaryResponseDTO.of(container, latestStats));
            }

            // 4. OOM Kill 감지 및 처리 (중복 방지)
            if (Boolean.TRUE.equals(snapshot.getOomKilled())) {
                handleOomKillIfNew(container, agentKey);
            }
        }
    }

    /**
     * 스냅샷 데이터로 컨테이너 생성 (초기값 0, metricsInitialized = false)
     * @return 생성된 컨테이너
     */
    private Container createContainerFromSnapshot(Agent agent, ContainerSnapshotRequestDTO snapshot, ContainerState state) {
        Container container = Container.builder()
                .agent(agent)
                .containerHash(snapshot.getContainerHash())
                .state(state)
                .name(snapshot.getContainerName())
                .imageName(snapshot.getImageName())
                // 초기값 (메트릭 수신 전까지 0)
                .cpuQuota(0L)
                .cpuPeriod(0L)
                .cpuLimitCores(BigDecimal.ZERO)
                .onlineCpus(1)
                .memLimit(0L)
                .oomKills(0)
                .storageLimit(0L)
                .imageSize(snapshot.getImageSize())
                .metricsInitialized(false)  // 메트릭 미수신 상태
                .build();

        return containerRepository.save(container);
    }

    /**
     * OOM Kill 이벤트 처리 (중복 방지 포함)
     * - 이전 상태가 RUNNING/PAUSED일 때만 새로운 OOM으로 간주
     * - 5초 내 중복 이벤트 방지
     */
    private void handleOomKillIfNew(Container container, String agentKey) {
        LocalDateTime now = LocalDateTime.now();

        // 중복 방지: RUNNING/PAUSED 상태였고, 아직 기록 안 됐으면 새로운 OOM
        boolean isRunningState = container.getState() == ContainerState.RUNNING
                || container.getState() == ContainerState.PAUSED;

        boolean isNotRecentlyRecorded = container.getLastOomKilledAt() == null
                || container.getLastOomKilledAt().isBefore(now.minusSeconds(5));

        if (isRunningState && isNotRecentlyRecorded) {
            // 새로운 OOM 이벤트
            handleOomKill(container, agentKey, now);
        } else {
            log.debug("[OOM] 중복 이벤트 무시 - containerId: {}, state: {}, lastOom: {}",
                    container.getId(), container.getState(), container.getLastOomKilledAt());
        }
    }

    /**
     * 신규 컨테이너 OOM Kill 처리 (중복 체크 불필요)
     * - 신규 컨테이너가 처음부터 OOM 상태로 생성된 경우
     * - 상태 검증 없이 무조건 기록
     */
    private void handleOomKillForNewContainer(Container container, String agentKey) {
        LocalDateTime now = LocalDateTime.now();
        handleOomKill(container, agentKey, now);
        log.warn("[OOM] 신규 컨테이너가 OOM 상태로 생성됨 - containerId: {}, containerName: {}, state: {}",
                container.getId(), container.getName(), container.getState());
    }

    /**
     * OOM Kill 이벤트 처리
     * - DB의 oomKills 카운터 증가 (영구 누적) - 더티 체킹으로 자동 저장
     * - 캐시에 시간대별 OOM 이벤트 기록 (최근 7일)
     * - lastOomKilledAt 타임스탬프 업데이트
     */
    private void handleOomKill(Container container, String agentKey, LocalDateTime occurredAt) {
        // 1. DB 누적 횟수 증가 (더티 체킹으로 자동 저장됨)
        container.incrementOomKills();

        // 2. 마지막 OOM 시각 업데이트
        container.updateLastOomKilledAt(occurredAt);

        // 3. 캐시에 이벤트 기록 (Histogram/Heatmap용)
        OomEvent oomEvent = OomEvent.builder()
                .containerId(container.getId())
                .containerHash(container.getContainerHash())
                .containerName(container.getName())
                .occurredAt(occurredAt)
                .agentKey(agentKey)
                .build();

        oomEventCache.recordEvent(oomEvent);

        log.warn("[OOM] 새로운 OOM Kill 발생 - containerId: {}, containerName: {}, 누적 횟수: {}, 발생 시각: {}",
                container.getId(), container.getName(), container.getOomKills(), occurredAt);
    }

    private OomMetricsDTO buildOomMetrics(Container container, LocalDateTime startTime, LocalDateTime endTime) {
        // 캐시에서 시간대별 Histogram 조회 (HOURS 고정)
        Map<LocalDateTime, Long> histogram = oomEventCache.getHistogram(
                container.getId(),
                startTime,
                endTime,
                ChronoUnit.HOURS
        );

        return OomMetricsDTO.builder()
                .timeSeries(histogram)
                .totalOomKills(container.getOomKills())
                .lastOomKilledAt(container.getLastOomKilledAt())
                .build();
    }

    /**
     * 문자열 상태를 ContainerState Enum으로 변환
     */
    private ContainerState parseState(String state) {
        if (state == null) {
            return ContainerState.UNKNOWN;
        }

        try {
            String normalized = state.trim().toUpperCase();
            return ContainerState.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 컨테이너 상태: {}", state);
            return ContainerState.UNKNOWN;
        }
    }
}
