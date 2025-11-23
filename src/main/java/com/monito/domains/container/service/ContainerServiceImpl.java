package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.domain.*;
import com.monito.domains.container.dto.projection.ContainerLogProjection;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsRequest;
import com.monito.domains.container.dto.request.ContainerSnapshotRequestDTO;
import com.monito.domains.container.dto.response.*;
import com.monito.domains.container.dto.response.metrics.*;
import com.monito.domains.container.dto.response.timeseries.TimeSeriesResponse;
import java.util.Set;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.repository.projection.TimeSeriesDataPoint;
import com.monito.domains.container.util.CpuMetricsCalculator;
import com.monito.domains.container.util.TimeSeriesDownSampler;
import com.monito.domains.favorite.repository.FavoriteRepository;
import com.monito.global.cache.*;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;

import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
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
    private final AgentRepository agentRepository;
    private final OomEventCache oomEventCache;
    private final CpuMetricsBufferCache cpuMetricsBufferCache;
    private final CpuMetricsCalculator cpuMetricsCalculator;
    private final ContainerSummaryCache containerSummaryCache;
    private final ContainerLastStatsCache lastStatsCache;
    private final FavoriteCache favoriteCache;
    private final FavoriteRepository favoriteRepository;

    @Override
    public List<ContainerSummaryResponseDTO> getContainerList(
            Long memberId,
            String keyword,
            List<ContainerState> states,
            List<ContainerHealth> healths,
            ContainerSortField sortBy,
            Sort.Direction direction
    ) {
        // 1. 캐시에서 모든 스냅샷 조회
        List<ContainerSummarySnapshot> snapshots = containerSummaryCache.getAllSnapshots();

        // 2. 사용자의 즐겨찾기 목록 조회 (FavoriteCache에서)
        Set<Long> favoriteContainerIds = memberId != null
                ? favoriteCache.getFavoriteSet(memberId)
                : Set.of();

        // 3. Snapshot → ResponseDTO 변환 (isFavorite 포함)
        Stream<ContainerSummaryResponseDTO> dtoStream = snapshots.stream()
                .map(snapshot -> {
                    boolean isFavorite = favoriteContainerIds.contains(snapshot.getId());
                    return ContainerSummaryResponseDTO.from(snapshot, isFavorite);
                });

        // 4. keyword 필터링 (검색)
        if (keyword != null && !keyword.trim().isEmpty()) {
            String searchKeyword = keyword.trim().toLowerCase();
            dtoStream = dtoStream.filter(dto ->
                    (dto.getAgentName() != null && dto.getAgentName().toLowerCase().contains(searchKeyword)) ||
                    (dto.getContainerHash() != null && dto.getContainerHash().toLowerCase().contains(searchKeyword)) ||
                    (dto.getContainerName() != null && dto.getContainerName().toLowerCase().contains(searchKeyword))
            );
        }

        // 5. state 필터링
        if (states != null && !states.isEmpty()) {
            dtoStream = dtoStream.filter(dto -> states.contains(dto.getState()));
        }

        // 6. health 필터링
        if (healths != null && !healths.isEmpty()) {
            dtoStream = dtoStream.filter(dto -> healths.contains(dto.getHealth()));
        }

        // 7. 정렬
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

        // 5. 통합 메서드로 로그 조회 (Native Query - CLOB 제외, 500자 미리보기)
        List<Object[]> rawLogs = containerLogRepository.findLogsOptimizedNative(
                validContainerIds,  // null이면 모든 컨테이너, 아니면 지정된 컨테이너들
                request.getLogSource() != null ? request.getLogSource().name() : null,
                request.getAgentName(),
                request.getLastLogId(),
                request.getLastLoggedAt(),
                startTime,
                endTime,
                pageRequest
        );

        // 6. Object[] → ContainerLogProjection 변환
        List<ContainerLogProjection> logs = rawLogs.stream()
                .map(row -> new ContainerLogProjection(
                        ((Number) row[0]).longValue(),  // id
                        ((Number) row[1]).longValue(),  // containerId
                        (String) row[2],                // containerHash
                        (String) row[3],                // containerName
                        ((Number) row[4]).longValue(),  // agentId
                        (String) row[5],                // agentName
                        (String) row[6],                // logMessagePreview
                        LogSource.valueOf((String) row[7]),  // source
                        ((java.sql.Timestamp) row[8]).toLocalDateTime(),  // loggedAt
                        ((java.sql.Timestamp) row[9]).toLocalDateTime()   // createdAt
                ))
                .toList();

        // 7. hasMore 판단 및 실제 반환할 로그 분리
        boolean hasMore = logs.size() > requestSize;
        List<ContainerLogProjection> actualLogs =
                hasMore ? logs.subList(0, requestSize) : logs;

        // 8. DTO 변환 (Projection에서 변환)
        List<ContainerLogEntryDTO> logEntries = actualLogs.stream()
                .map(ContainerLogEntryDTO::from)
                .toList();

        // 9. 다음 커서 정보
        Long lastLogId = null;
        LocalDateTime lastLoggedAt = null;
        if (!actualLogs.isEmpty()) {
            ContainerLogProjection lastLog =
                    actualLogs.get(actualLogs.size() - 1);
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
    private Sort createLogSort(LogSortField sortBy, Sort.Direction direction) {
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

                // 3-1-2. 캐시에 Snapshot 저장
                containerSummaryCache.update(ContainerSummarySnapshot.of(newContainer, null));
            }
        } else {
            // 3-2. 기존 컨테이너 업데이트
            if (state == ContainerState.DELETED) {
                // Soft delete 처리
                container.markAsDeleted();
                log.info("컨테이너 삭제 처리 - Agent: {}, ContainerHash: {}",
                        agentKey, snapshot.getContainerHash());

                // 컨테이너 삭제 시 관련된 모든 데이터 삭제
                // 1. 메트릭 캐시 삭제
                cpuMetricsBufferCache.removeContainer(container.getId());
                oomEventCache.removeContainer(container.getId());
                containerSummaryCache.remove(container.getId());
                lastStatsCache.remove(container.getContainerHash());

                // 2. 즐겨찾기 데이터 삭제 (DB + 캐시)
                favoriteRepository.deleteByContainerId(container.getId());  // DB 삭제
                favoriteCache.removeContainerFromAll(container.getId());    // 캐시 삭제
            } else {
                // state 또는 status가 변경된 경우 업데이트
                boolean stateChanged = container.getState() != state;
                boolean statusChanged = snapshot.getStatus() != null
                        && !snapshot.getStatus().equals(container.getStatus());

                if(stateChanged || statusChanged) {
                    container.changeStateWithStatus(state, snapshot.getStatus());
                    log.debug("컨테이너 상태 변경 - ContainerHash: {}, State: {}, Status: {}",
                            snapshot.getContainerHash(), state, snapshot.getStatus());
                }

                // 이미지 정보 업데이트 (Agent가 보낸 경우만)
                boolean imageInfoUpdated = false;
                if (snapshot.getImageId() != null && !snapshot.getImageId().equals(container.getImageId())) {
                    container.updateImageId(snapshot.getImageId());
                    imageInfoUpdated = true;
                }
                if (snapshot.getImageName() != null && !snapshot.getImageName().equals(container.getImageName())) {
                    container.updateImageName(snapshot.getImageName());
                    imageInfoUpdated = true;
                }
                if (snapshot.getImageSize() != null && !snapshot.getImageSize().equals(container.getImageSize())) {
                    container.updateImageSize(snapshot.getImageSize());
                    imageInfoUpdated = true;
                }

                if (imageInfoUpdated) {
                    log.info("컨테이너 이미지 정보 업데이트 - ContainerHash: {}, ImageId: {}, ImageName: {}, ImageSize: {}",
                            snapshot.getContainerHash(), snapshot.getImageId(), snapshot.getImageName(), snapshot.getImageSize());
                }

                // 캐시에 Snapshot 업데이트 - 파티션 프루닝을 위해 1시간 전부터 조회
                ContainerStatsLog latestStats = containerStatsLogRepository
                        .findLatestByContainerHash(
                                container.getContainerHash(),
                                LocalDateTime.now().minusHours(1)
                        )
                        .orElse(null);
                containerSummaryCache.update(ContainerSummarySnapshot.of(container, latestStats));
            }

            // 4. OOM Kill 감지 및 처리 (중복 방지)
            if (Boolean.TRUE.equals(snapshot.getOomKilled())) {
                handleOomKillIfNew(container, agentKey);
            }
        }
    }

    /**
     * 스냅샷 데이터로 컨테이너 생성 (초기값 0)
     * @return 생성된 컨테이너
     */
    private Container createContainerFromSnapshot(Agent agent, ContainerSnapshotRequestDTO snapshot, ContainerState state) {
        Container container = Container.builder()
                .agent(agent)
                .containerHash(snapshot.getContainerHash())
                .state(state)
                .status(snapshot.getStatus())
                .name(snapshot.getContainerName())
                .imageName(snapshot.getImageName())
                .imageId(snapshot.getImageId())
                .cpuQuota(0L)
                .cpuPeriod(0L)
                .cpuLimitCores(BigDecimal.ZERO)
                .onlineCpus(1)
                .isCpuUnlimited(false)
                .memLimit(0L)
                .isMemoryUnlimited(false)
                .oomKills(0)
                .storageLimit(0L)
                .isStorageUnlimited(false)
                .imageSize(snapshot.getImageSize())
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

    @Override
    public List<DeletedContainerResponseDTO> getDeletedContainers() {
        // 24시간 이내 삭제된 컨테이너 조회 (현재 시간 - 24시간)
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<Container> deletedContainers = containerRepository.findAllDeletedWithin24Hours(since);

        // N+1 방지: Agent를 명시적으로 로드 (Lazy Loading 강제 초기화)
        deletedContainers.forEach(container -> {
            if (container.getAgent() != null) {
                container.getAgent().getAgentName(); // Agent 프록시 초기화
            }
        });

        return deletedContainers.stream()
                .map(DeletedContainerResponseDTO::from)
                .toList();
    }

    @Override
    @Transactional
    public void syncAgentContainers(String agentKey, Set<String> agentContainerHashes) {
        // 1. Agent 조회
        Agent agent = agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));

        // 2. DB에서 해당 Agent의 활성 컨테이너 조회 (is_deleted = 0)
        List<Container> dbContainers = containerRepository.findAllByAgent_Id(agent.getId());

        log.info("🔄 Agent 컨테이너 동기화 - Agent: {}, DB 활성 컨테이너: {}개, Agent 보고: {}개",
                agentKey, dbContainers.size(), agentContainerHashes.size());

        // 3. DB에는 있지만 Agent가 보내지 않은 컨테이너 = 삭제된 것
        List<Container> missingContainers = dbContainers.stream()
                .filter(container -> !agentContainerHashes.contains(container.getContainerHash()))
                .toList();

        // 4. 삭제 처리
        if (!missingContainers.isEmpty()) {
            log.warn("⚠️  삭제된 컨테이너 감지: {}개", missingContainers.size());

            for (Container container : missingContainers) {
                container.markAsDeleted();
                log.warn("   🗑️  컨테이너 삭제 처리 - ID: {}, Hash: {}, Name: {}",
                        container.getId(), container.getContainerHash(), container.getName());

                // 캐시 및 관련 데이터 삭제
                cpuMetricsBufferCache.removeContainer(container.getId());
                oomEventCache.removeContainer(container.getId());
                containerSummaryCache.remove(container.getId());
                favoriteRepository.deleteByContainerId(container.getId());
                favoriteCache.removeContainerFromAll(container.getId());
            }

            log.info("✅ 컨테이너 동기화 완료 - {}개 삭제 처리됨", missingContainers.size());
        } else {
            log.info("✅ 컨테이너 동기화 완료 - 삭제할 컨테이너 없음 (DB와 Agent 일치)");
        }
    }

    // ==================== 시계열 데이터 전용 메서드 구현 ====================

    @Override
    public TimeSeriesResponse getCpuUsageTimeSeries(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Projection을 통한 최적화된 데이터 조회 (Index-Only Scan)
        List<TimeSeriesDataPoint> projections = containerStatsLogRepository.findCpuUsageTimeSeries(
                containerId, startTime, endTime
        );

        // 4. TimeSeriesDataDTO로 변환
        List<TimeSeriesDataDTO> rawData = projections.stream()
                .map(projection -> TimeSeriesDataDTO.builder()
                        .timestamp(projection.getCollectedAt())
                        .value(projection.getValue())
                        .build())
                .toList();

        // 5. 자동 다운샘플링 적용
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(rawData, totalMinutes);

        // 6. 메타데이터와 함께 응답
        return TimeSeriesResponse.of(startTime, endTime, sampledData);
    }

    @Override
    public TimeSeriesResponse getMemoryUsageTimeSeries(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Projection을 통한 최적화된 데이터 조회 (Index-Only Scan)
        List<TimeSeriesDataPoint> projections = containerStatsLogRepository.findMemoryUsageTimeSeries(
                containerId, startTime, endTime
        );

        // 4. TimeSeriesDataDTO로 변환
        List<TimeSeriesDataDTO> rawData = projections.stream()
                .map(projection -> TimeSeriesDataDTO.builder()
                        .timestamp(projection.getCollectedAt())
                        .value(projection.getValue())
                        .build())
                .toList();

        // 5. 자동 다운샘플링 적용
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(rawData, totalMinutes);

        // 6. 메타데이터와 함께 응답
        return TimeSeriesResponse.of(startTime, endTime, sampledData);
    }

    @Override
    public TimeSeriesResponse getNetworkRxTimeSeries(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Projection을 통한 최적화된 데이터 조회 (Index-Only Scan)
        List<TimeSeriesDataPoint> projections = containerStatsLogRepository.findNetworkRxTimeSeries(
                containerId, startTime, endTime
        );

        // 4. TimeSeriesDataDTO로 변환 (bytes/sec)
        List<TimeSeriesDataDTO> rawData = projections.stream()
                .map(projection -> TimeSeriesDataDTO.builder()
                        .timestamp(projection.getCollectedAt())
                        .value(projection.getValue())
                        .build())
                .toList();

        // 5. 자동 다운샘플링 적용
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(rawData, totalMinutes);

        // 6. 메타데이터와 함께 응답
        return TimeSeriesResponse.of(startTime, endTime, sampledData);
    }

    @Override
    public TimeSeriesResponse getNetworkTxTimeSeries(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Projection을 통한 최적화된 데이터 조회 (Index-Only Scan)
        List<TimeSeriesDataPoint> projections = containerStatsLogRepository.findNetworkTxTimeSeries(
                containerId, startTime, endTime
        );

        // 4. TimeSeriesDataDTO로 변환 (bytes/sec)
        List<TimeSeriesDataDTO> rawData = projections.stream()
                .map(projection -> TimeSeriesDataDTO.builder()
                        .timestamp(projection.getCollectedAt())
                        .value(projection.getValue())
                        .build())
                .toList();

        // 5. 자동 다운샘플링 적용
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(rawData, totalMinutes);

        // 6. 메타데이터와 함께 응답
        return TimeSeriesResponse.of(startTime, endTime, sampledData);
    }

    @Override
    public TimeSeriesResponse getNetworkPacketsTimeSeries(Long containerId, ContainerMetricsRequest request) {
        // 1. 컨테이너 존재 확인
        if (!containerRepository.existsById(containerId)) {
            throw new NotFoundException(ExceptionMessage.DATA_NOT_FOUND);
        }

        // 2. 시간 범위 계산
        LocalDateTime startTime = request.getCalculatedStartTime();
        LocalDateTime endTime = request.getCalculatedEndTime();
        long totalMinutes = java.time.Duration.between(startTime, endTime).toMinutes();

        // 3. Projection을 통한 최적화된 데이터 조회 (Index-Only Scan)
        List<TimeSeriesDataPoint> projections = containerStatsLogRepository.findNetworkPacketsTimeSeries(
                containerId, startTime, endTime
        );

        // 4. TimeSeriesDataDTO로 변환 (RX + TX packets/sec)
        List<TimeSeriesDataDTO> rawData = projections.stream()
                .map(projection -> TimeSeriesDataDTO.builder()
                        .timestamp(projection.getCollectedAt())
                        .value(projection.getValue())
                        .build())
                .toList();

        // 5. 자동 다운샘플링 적용
        List<TimeSeriesDataDTO> sampledData = TimeSeriesDownSampler.autoDownSample(rawData, totalMinutes);

        // 6. 메타데이터와 함께 응답
        return TimeSeriesResponse.of(startTime, endTime, sampledData);
    }
}
