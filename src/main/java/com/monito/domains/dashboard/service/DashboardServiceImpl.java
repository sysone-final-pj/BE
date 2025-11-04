package com.monito.domains.dashboard.service;

import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.AgentContainerGroupDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.dashboard.dto.response.ContainerWithFavoriteDTO;
import com.monito.domains.dashboard.repository.DashboardRepository;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Override
    public List<ContainerDashboardResponseDTO> getAllContainers(ContainerSortType sortType, Long memberId) {
        log.info("대시보드: 전체 컨테이너 목록 조회 (정렬: {}, memberId: {})", sortType, memberId);

        List<ContainerDashboardResponseDTO> containers = dashboardRepository.findAllContainersForDashboard();

        // 정렬 타입이 없으면 기본값으로 즐겨찾기 정렬 적용
        if (sortType == null) {
            sortType = ContainerSortType.FAVORITE;
        }

        // 정렬 타입에 따라 정렬
        return sortContainers(containers, sortType, memberId);
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
}
