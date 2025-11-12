package com.monito.domains.dashboard.service;

import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.request.TimeRange;
import com.monito.domains.dashboard.dto.response.*;
import com.monito.domains.dashboard.repository.DashboardRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private final ContainerLogRepository containerLogRepository;
    private final ContainerStatsLogRepository containerStatsLogRepository;
    private final ContainerRepository containerRepository;

    @Override
    public List<ContainerDashboardResponseDTO> getAllContainers(ContainerSortType sortType, Long memberId, ContainerFilterDTO filter) {
        log.info("대시보드: 전체 컨테이너 목록 조회 (정렬: {}, memberId: {}, 필터: {})", sortType, memberId, filter != null);

        List<ContainerDashboardResponseDTO> containers;

        // 필터가 있으면 필터링 적용
        if (hasActiveFilter(filter)) {
            containers = dashboardRepository.findContainersWithFilters(
                    filter.getKeyword(),
                    filter.getKeyword() == null || filter.getKeyword().trim().isEmpty(),
                    filter.getFavoriteOnly() != null && filter.getFavoriteOnly(),
                    filter.getStates(),
                    filter.getStates() == null || filter.getStates().isEmpty(),
                    filter.getHealths(),
                    filter.getHealths() == null || filter.getHealths().isEmpty(),
                    filter.getAgentIds(),
                    filter.getAgentIds() == null || filter.getAgentIds().isEmpty(),
                    memberId
            );
        } else {
            containers = dashboardRepository.findAllContainersForDashboard();
        }

        // 정렬 타입이 없으면 기본값으로 즐겨찾기 정렬 적용
        if (sortType == null) {
            sortType = ContainerSortType.FAVORITE;
        }

        // 정렬 타입에 따라 정렬
        return sortContainers(containers, sortType, memberId);
    }

    /**
     * 필터가 활성화되어 있는지 확인
     */
    private boolean hasActiveFilter(ContainerFilterDTO filter) {
        if (filter == null) {
            return false;
        }
        return (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) ||
                (filter.getFavoriteOnly() != null && filter.getFavoriteOnly()) ||
                (filter.getStates() != null && !filter.getStates().isEmpty()) ||
                (filter.getHealths() != null && !filter.getHealths().isEmpty()) ||
                (filter.getAgentIds() != null && !filter.getAgentIds().isEmpty());
    }

    /**
     * 컨테이너 목록 정렬
     */
    private List<ContainerDashboardResponseDTO> sortContainers(
            List<ContainerDashboardResponseDTO> containers,
            ContainerSortType sortType,
            Long memberId) {

        return switch (sortType) {
            case NAME -> containers.stream()
                    .sorted(Comparator.comparing(ContainerDashboardResponseDTO::getContainerName,
                            Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            case CPU_PERCENT -> containers.stream()
                    .sorted(Comparator.comparing(ContainerDashboardResponseDTO::getCpuPercent,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            case MEM_PERCENT -> containers.stream()
                    .sorted(Comparator.comparing(ContainerDashboardResponseDTO::getMemPercent,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            case NETWORK_TOTAL_BYTES -> containers.stream()
                    .sorted(Comparator.comparing(ContainerDashboardResponseDTO::getNetworkTotalBytes,
                            Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());

            case FAVORITE -> {
                if (memberId == null) {
                    log.warn("FAVORITE 정렬 시 memberId가 필요하지만 null입니다. 정렬하지 않고 반환합니다.");
                    yield containers;
                }

                // 즐겨찾기 컨테이너 ID 목록 조회
                List<Long> favoriteIds = dashboardRepository.findFavoriteContainerIdsByMemberId(memberId);
                Set<Long> favoriteIdSet = new HashSet<>(favoriteIds);

                // 즐겨찾기 우선 정렬 (즐겨찾기가 먼저 오도록)
                yield containers.stream()
                        .sorted((c1, c2) -> {
                            boolean isFav1 = favoriteIdSet.contains(c1.getContainerId());
                            boolean isFav2 = favoriteIdSet.contains(c2.getContainerId());
                            // 즐겨찾기가 먼저 오도록: true > false
                            return Boolean.compare(isFav2, isFav1);
                        })
                        .collect(Collectors.toList());
            }
        };
    }

    @Override
    public List<ContainerDashboardResponseDTO> getContainersByAgentId(Long agentId) {
        log.info("대시보드: Agent별 컨테이너 목록 조회 - agentId: {}", agentId);
        return dashboardRepository.findContainersByAgentId(agentId);
    }

    @Override
    public List<AgentContainerCountDTO> getContainerCountByAgent() {
        log.info("대시보드: Agent별 컨테이너 개수 집계");
        return dashboardRepository.countContainersByAgent();
    }

    @Override
    public List<AgentContainerGroupDTO> getContainersGroupedByAgent() {
        log.info("대시보드: Agent별 컨테이너 그룹핑 (리스트 포함)");

        // 전체 컨테이너 조회
        List<ContainerDashboardResponseDTO> allContainers = dashboardRepository.findAllContainersForDashboard();

        // Agent별로 그룹핑 (agentId 기준)
        Map<Long, List<ContainerDashboardResponseDTO>> groupedByAgent = allContainers.stream()
                .collect(Collectors.groupingBy(ContainerDashboardResponseDTO::getAgentId));

        // AgentContainerGroupDTO 리스트로 변환
        return groupedByAgent.entrySet().stream()
                .map(entry -> {
                    Long agentId = entry.getKey();
                    List<ContainerDashboardResponseDTO> containers = entry.getValue();

                    // Agent 이름은 첫 번째 컨테이너에서 추출 (모든 컨테이너가 같은 Agent에 속함)
                    String agentName = containers.isEmpty() ? "Unknown" : containers.get(0).getAgentName();

                    return AgentContainerGroupDTO.builder()
                            .agentId(agentId)
                            .agentName(agentName)
                            .containerCount((long) containers.size())
                            .containers(containers)
                            .build();
                })
                .sorted((a, b) -> b.getContainerCount().compareTo(a.getContainerCount())) // 개수 내림차순
                .collect(Collectors.toList());
    }

    @Override
    public List<ContainerDashboardResponseDTO> getRunningContainers() {
        log.info("대시보드: 구동중인 컨테이너 목록 조회 (state=RUNNING)");
        return dashboardRepository.findRunningContainers();
    }

    @Override
    public ContainerDashboardResponseDTO getContainerDetail(Long containerId) {
        log.info("대시보드: 컨테이너 상세 정보 조회 - containerId: {}", containerId);
        return dashboardRepository.findContainerDetailById(containerId);
    }

    @Override
    public List<ContainerWithFavoriteDTO> getAllContainersSortedByFavorite(Long memberId) {
        log.info("대시보드: 즐겨찾기 우선 정렬된 전체 컨테이너 목록 조회 - memberId: {}", memberId);

        // 1. 모든 컨테이너 조회
        List<ContainerDashboardResponseDTO> allContainers = dashboardRepository.findAllContainersForDashboard();

        // 2. 즐겨찾기 컨테이너 ID 목록 조회
        List<Long> favoriteContainerIds = dashboardRepository.findFavoriteContainerIdsByMemberId(memberId);
        Set<Long> favoriteIdSet = new HashSet<>(favoriteContainerIds);

        // 3. 각 컨테이너에 즐겨찾기 여부 표시
        List<ContainerWithFavoriteDTO> containersWithFavorite = allContainers.stream()
                .map(container -> ContainerWithFavoriteDTO.builder()
                        .container(container)
                        .isFavorite(favoriteIdSet.contains(container.getContainerId()))
                        .build())
                .collect(Collectors.toList());

        // 4. 즐겨찾기 우선 정렬 (즐겨찾기=true가 먼저)
        return containersWithFavorite.stream()
                .sorted(Comparator.comparing(ContainerWithFavoriteDTO::getIsFavorite).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public DailyLogCountDTO getDailyLogCount() {
        log.info("대시보드: 당일 0시 기준 STDOUT/STDERR 로그 개수 조회");

        // 당일 0시 계산
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        // 다음날 0시 계산
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        // STDOUT 로그 개수 조회
        long stdoutCount = containerLogRepository.countBySourceAndLoggedAtBetween(
                LogSource.STDOUT,
                startOfDay,
                startOfNextDay
        );

        // STDERR 로그 개수 조회
        long stderrCount = containerLogRepository.countBySourceAndLoggedAtBetween(
                LogSource.STDERR,
                startOfDay,
                startOfNextDay
        );

        // 조회 기준일 (YYYY-MM-DD 형식)
        String date = startOfDay.format(DateTimeFormatter.ISO_LOCAL_DATE);

        log.info("당일 로그 개수 - STDOUT: {}, STDERR: {}, 기준일: {}", stdoutCount, stderrCount, date);

        return DailyLogCountDTO.builder()
                .stdoutCount(stdoutCount)
                .stderrCount(stderrCount)
                .date(date)
                .build();
    }

    @Override
    public List<ContainerStorageUsageDTO> getAllContainerStorageUsage() {
        log.info("대시보드: 전체 컨테이너 스토리지 사용량 조회");
        return dashboardRepository.findAllContainerStorageUsage();
    }

    @Override
    public NetworkStatsTimeSeriesDTO getNetworkStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail) {
        log.info("대시보드: 네트워크 통계 시계열 데이터 조회 - containerId: {}, timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // 컨테이너 정보 조회
        var container = containerRepository.findById(containerId)
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다: " + containerId));

        // 시간 범위 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(timeRange.getMinutes());

        // 통계 데이터 조회
        List<ContainerStatsLog> statsLogs = containerStatsLogRepository.findByContainerIdAndTimeRange(
                containerId, startTime, now
        );

        // 샘플링 처리
        int targetPoints = detail ? 200 : 50;
        List<NetworkStatsDataPointDTO> dataPoints = sampleNetworkStats(statsLogs, targetPoints);

        return NetworkStatsTimeSeriesDTO.builder()
                .containerId(containerId)
                .containerName(container.getName())
                .timeRange(timeRange)
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * 네트워크 통계 데이터 샘플링
     * @param statsLogs 원본 통계 로그
     * @param targetPoints 목표 포인트 개수
     * @return 샘플링된 데이터 포인트
     */
    private List<NetworkStatsDataPointDTO> sampleNetworkStats(List<ContainerStatsLog> statsLogs, int targetPoints) {
        if (statsLogs.isEmpty()) {
            return List.of();
        }

        // 원본 데이터가 목표보다 적으면 그대로 반환
        if (statsLogs.size() <= targetPoints) {
            return statsLogs.stream()
                    .map(log -> NetworkStatsDataPointDTO.builder()
                            .timestamp(log.getCollectedAt())
                            .rxBytesPerSec(log.getRxBytesPerSec())
                            .txBytesPerSec(log.getTxBytesPerSec())
                            .build())
                    .collect(Collectors.toList());
        }

        // 샘플링 간격 계산
        double step = (double) statsLogs.size() / targetPoints;
        List<NetworkStatsDataPointDTO> sampledData = new ArrayList<>();

        for (int i = 0; i < targetPoints; i++) {
            int index = (int) (i * step);
            if (index < statsLogs.size()) {
                ContainerStatsLog log = statsLogs.get(index);
                sampledData.add(NetworkStatsDataPointDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .rxBytesPerSec(log.getRxBytesPerSec())
                        .txBytesPerSec(log.getTxBytesPerSec())
                        .build());
            }
        }

        return sampledData;
    }

    @Override
    public BlockIOStatsTimeSeriesDTO getBlockIOStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail) {
        log.info("대시보드: Block I/O 통계 시계열 데이터 조회 - containerId: {}, timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        // 컨테이너 정보 조회
        var container = containerRepository.findById(containerId)
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다: " + containerId));

        // 시간 범위 계산
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = now.minusMinutes(timeRange.getMinutes());

        // 통계 데이터 조회
        List<ContainerStatsLog> statsLogs = containerStatsLogRepository.findByContainerIdAndTimeRange(
                containerId, startTime, now
        );

        // 샘플링 처리
        int targetPoints = detail ? 200 : 50;
        List<BlockIOStatsDataPointDTO> dataPoints = sampleBlockIOStats(statsLogs, targetPoints);

        return BlockIOStatsTimeSeriesDTO.builder()
                .containerId(containerId)
                .containerName(container.getName())
                .timeRange(timeRange)
                .dataPointCount(dataPoints.size())
                .dataPoints(dataPoints)
                .build();
    }

    /**
     * Block I/O 통계 데이터 샘플링
     * @param statsLogs 원본 통계 로그
     * @param targetPoints 목표 포인트 개수
     * @return 샘플링된 데이터 포인트
     */
    private List<BlockIOStatsDataPointDTO> sampleBlockIOStats(List<ContainerStatsLog> statsLogs, int targetPoints) {
        if (statsLogs.isEmpty()) {
            return List.of();
        }

        // 원본 데이터가 목표보다 적으면 그대로 반환
        if (statsLogs.size() <= targetPoints) {
            return statsLogs.stream()
                    .map(log -> BlockIOStatsDataPointDTO.builder()
                            .timestamp(log.getCollectedAt())
                            .blkRead(log.getBlkRead())
                            .blkWrite(log.getBlkWrite())
                            .build())
                    .collect(Collectors.toList());
        }

        // 샘플링 간격 계산
        double step = (double) statsLogs.size() / targetPoints;
        List<BlockIOStatsDataPointDTO> sampledData = new ArrayList<>();

        for (int i = 0; i < targetPoints; i++) {
            int index = (int) (i * step);
            if (index < statsLogs.size()) {
                ContainerStatsLog log = statsLogs.get(index);
                sampledData.add(BlockIOStatsDataPointDTO.builder()
                        .timestamp(log.getCollectedAt())
                        .blkRead(log.getBlkRead())
                        .blkWrite(log.getBlkWrite())
                        .build());
            }
        }

        return sampledData;
    }

    @Override
    public ContainerDetailResponseDTO getContainerDetailMetrics(Long containerId) {
        log.info("컨테이너 상세 메트릭 조회 - containerId: {}", containerId);

        // 컨테이너 조회
        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException(
                        ExceptionMessage.CONTAINER_NOT_FOUND));

        // 최신 StatsLog 조회
        ContainerStatsLog statsLog = containerStatsLogRepository.findTopByContainerIdOrderByCollectedAtDesc(containerId)
                .orElseThrow(() -> new NotFoundException(
                        ExceptionMessage.CONTAINER_STATS_LOG_NOT_FOUND));

        // ContainerDetailResponseDTO 생성 (WebSocket과 동일한 형식)
        return ContainerDetailResponseDTO.forRealtimeUpdate(
                container,
                container.getAgent(),
                statsLog
        );
    }
}
