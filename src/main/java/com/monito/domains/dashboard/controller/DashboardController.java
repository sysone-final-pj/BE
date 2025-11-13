package com.monito.domains.dashboard.controller;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.request.TimeRange;
import com.monito.domains.dashboard.dto.response.*;
import com.monito.domains.dashboard.service.DashboardService;
import com.monito.global.common.response.ApiResponse;
import java.util.List;

import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API Controller
 * - 대시보드 화면 전용 API
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "대시보드 전용 API")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드용 전체 컨테이너 목록 조회 (필터 + 정렬)
     * GET /api/dashboard/containers
     * @param sortBy 정렬 기준
     * @param favoriteOnly 즐겨찾기만 보기
     * @param states 상태 필터
     * @param healths 헬스 필터
     * @param agentIds 에이전트 ID 필터
     * @param userDetails 현재 로그인한 사용자
     * @return 필터링 + 정렬된 컨테이너 목록
     */
    @Operation(summary = "컨테이너 목록조회(검색+필터+정렬)",
            description = """
                    컨테이너 목록 조회 with 검색 + 필터 + 정렬

                    **검색 옵션:**
                    - keyword: 검색 키워드 (컨테이너 이름, 이미지명 검색)

                    **필터 옵션:**
                    - favoriteOnly: 즐겨찾기만 보기 (true/false)
                    - states: 상태 필터 (RUNNING, RESTARTING, PAUSED, CREATED, EXIT, DEAD)
                    - healths: 헬스 필터 (HEALTHY, UNHEALTHY, STARTING, NONE, UNKNOWN)
                    - agentIds: 에이전트 ID 필터

                    **정렬 옵션:**
                    - sortBy: CPU_PERCENT, MEM_PERCENT, NETWORK_TOTAL_BYTES, FAVORITE
                    """)
    @GetMapping("/containers")
    public ApiResponse<List<ContainerDashboardResponseDTO>> getAllContainers(
            @Parameter(description = "검색 키워드 (컨테이너 이름, 이미지명 등)")
            @RequestParam(required = false) String keyword,

            @Parameter(description = "정렬 기준 (CPU_PERCENT, MEM_PERCENT, NETWORK_TOTAL_BYTES, FAVORITE)")
            @RequestParam(required = false) ContainerSortType sortBy,

            @Parameter(description = "즐겨찾기만 보기 (true: 즐겨찾기만, false/null: 전체)")
            @RequestParam(required = false) Boolean favoriteOnly,

            @Parameter(description = "상태 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<ContainerState> states,

            @Parameter(description = "헬스 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<ContainerHealth> healths,

            @Parameter(description = "에이전트 ID 필터 (다중 선택 가능)")
            @RequestParam(required = false) List<Long> agentIds,

            @AuthenticationPrincipal CustomUserDetails userDetails) {

        Long memberId = userDetails != null ? Long.valueOf(userDetails.getId()) : null;


        // 필터 DTO 생성
        ContainerFilterDTO filter =
                ContainerFilterDTO.builder()
                        .keyword(keyword)
                        .favoriteOnly(favoriteOnly)
                        .states(states)
                        .healths(healths)
                        .agentIds(agentIds)
                        .build();

        log.info("GET /api/dashboard/containers - 대시보드용 컨테이너 목록 조회 (정렬: {}, memberId: {}, keyword: {})", sortBy, memberId, keyword);

        List<ContainerDashboardResponseDTO> containers = dashboardService.getAllContainers(sortBy, memberId, filter);

        return ApiResponse.ok(containers, "대시보드 컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * Agent별 컨테이너 개수 집계
     * GET /api/dashboard/containers/count-by-agent
     * @return Agent별 컨테이너 개수 목록 (개수 내림차순)
     */
    @Operation(summary = "컨테이너 분포 조회",
            description = "agent별 컨테이너 수 카운트")
    @GetMapping("/containers/count-by-agent")
    public ApiResponse<List<AgentContainerCountDTO>> getContainerCountByAgent() {
        log.info("GET /api/dashboard/containers/count-by-agent - Agent별 컨테이너 개수 집계");

        List<AgentContainerCountDTO> agentContainerCounts = dashboardService.getContainerCountByAgent();

        return ApiResponse.ok(agentContainerCounts, "Agent별 컨테이너 개수를 성공적으로 집계했습니다.");
    }

    /**
     * Agent별 컨테이너 그룹핑 (컨테이너 리스트 포함)
     * GET /api/dashboard/containers/group-by-agent
     * @return Agent별 컨테이너 그룹 목록 (개수 내림차순)
     */
    @Operation(summary = "Agent별 컨테이너 그룹 조회",
            description = "agent별 컨테이너 수와 컨테이너 리스트 포함")
    @GetMapping("/containers/group-by-agent")
    public ApiResponse<List<AgentContainerGroupDTO>> getContainersGroupedByAgent() {
        log.info("GET /api/dashboard/containers/group-by-agent - Agent별 컨테이너 그룹핑 (리스트 포함)");

        List<AgentContainerGroupDTO> agentGroups = dashboardService.getContainersGroupedByAgent();

        return ApiResponse.ok(agentGroups, "Agent별 컨테이너 그룹을 성공적으로 조회했습니다.");
    }

    /**
     * 구동중인 컨테이너 목록 조회
     * GET /api/dashboard/containers/running
     * @return RUNNING 상태의 컨테이너 목록
     */
    @Operation(summary = "구동중인 컨테이너 조회",
            description = "state=RUNNING인 컨테이너만 조회")
    @GetMapping("/containers/running")
    public ApiResponse<List<ContainerDashboardResponseDTO>> getRunningContainers() {
        log.info("GET /api/dashboard/containers/running - 구동중인 컨테이너 목록 조회");

        List<ContainerDashboardResponseDTO> runningContainers = dashboardService.getRunningContainers();

        return ApiResponse.ok(runningContainers, "구동중인 컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * 특정 컨테이너 상세 정보 조회
     * GET /api/dashboard/containers/{containerId}
     * @param containerId 컨테이너 ID
     * @return 컨테이너 상세 정보 (이미지 정보 포함)
     */
    @Operation(summary = "컨테이너 상세 조회",
            description = "특정 컨테이너의 상세 정보 (imageName, imageSize 포함)")
    @GetMapping("/containers/{containerId}")
    public ApiResponse<ContainerDashboardResponseDTO> getContainerDetail(
            @PathVariable Long containerId) {
        log.info("GET /api/dashboard/containers/{} - 컨테이너 상세 정보 조회", containerId);

        ContainerDashboardResponseDTO container = dashboardService.getContainerDetail(containerId);

        return ApiResponse.ok(container, "컨테이너 상세 정보를 성공적으로 조회했습니다.");
    }

    /**
     * 모든 컨테이너 목록 조회 (즐겨찾기 우선 정렬)
     * GET /api/dashboard/favorites
     * @param userDetails 현재 로그인한 사용자 ID
     * @return 즐겨찾기가 먼저 오는 모든 컨테이너 목록 (최신 통계 포함)
     */
    @Operation(summary = "모든 컨테이너 조회 (즐겨찾기 우선)",
            description = "즐겨찾기 컨테이너가 먼저 오고, 나머지 컨테이너가 뒤따르는 전체 목록 조회")
    @GetMapping("/favorites")
    public ApiResponse<List<ContainerWithFavoriteDTO>> getAllContainersSortedByFavorite(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("GET /api/dashboard/favorites - 즐겨찾기 우선 정렬된 컨테이너 목록 조회 - memberId: {}", userDetails.getId());

        List<ContainerWithFavoriteDTO> containers = dashboardService.getAllContainersSortedByFavorite(Long.valueOf(userDetails.getId()));

        return ApiResponse.ok(containers, "즐겨찾기 우선 정렬된 컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * 당일 0시 기준 STDOUT/STDERR 로그 개수 조회
     * GET /api/dashboard/logs/daily-count
     * @return 당일 STDOUT/STDERR 로그 개수
     */
    @Operation(summary = "당일 로그 개수 조회",
            description = "당일 0시 기준으로 STDOUT과 STDERR 로그의 개수를 조회합니다.")
    @GetMapping("/logs/daily-count")
    public ApiResponse<DailyLogCountDTO> getDailyLogCount() {
        log.info("GET /api/dashboard/logs/daily-count - 당일 로그 개수 조회");

        DailyLogCountDTO dailyLogCount = dashboardService.getDailyLogCount();

        return ApiResponse.ok(dailyLogCount, "당일 로그 개수를 성공적으로 조회했습니다.");
    }

    /**
     * 전체 컨테이너의 스토리지 사용량 조회
     * GET /api/dashboard/containers/storage-usage
     * @return 전체 컨테이너의 스토리지 할당량과 사용량 목록
     */
    @Operation(summary = "컨테이너 스토리지 사용량 조회",
            description = "전체 컨테이너의 스토리지 할당량(storageLimit)과 현재 사용량(sizeRootFs)을 조회합니다.")
    @GetMapping("/containers/storage-usage")
    public ApiResponse<List<ContainerStorageUsageDTO>> getAllContainerStorageUsage() {
        log.info("GET /api/dashboard/containers/storage-usage - 전체 컨테이너 스토리지 사용량 조회");

        List<ContainerStorageUsageDTO> storageUsage = dashboardService.getAllContainerStorageUsage();

        return ApiResponse.ok(storageUsage, "컨테이너 스토리지 사용량을 성공적으로 조회했습니다.");
    }

    /**
     * 컨테이너의 네트워크 통계 시계열 데이터 조회
     * GET /api/dashboard/containers/{containerId}/network-stats
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위 (FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return 네트워크 통계 시계열 데이터
     */
    @Operation(summary = "네트워크 통계 시계열 조회",
            description = "컨테이너의 네트워크 rx/tx 속도 시계열 데이터를 조회합니다. detail=false는 대시보드용(50포인트), detail=true는 상세보기용(200포인트)")
    @GetMapping("/containers/{containerId}/network-stats")
    public ApiResponse<NetworkStatsTimeSeriesDTO> getNetworkStatsTimeSeries(
            @PathVariable Long containerId,
            @Parameter(description = "시간 범위 (FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)")
            @RequestParam(defaultValue = "THIRTY_MINUTES") TimeRange timeRange,
            @Parameter(description = "상세 여부 (false: 50포인트, true: 200포인트)")
            @RequestParam(defaultValue = "false") boolean detail) {

        log.info("GET /api/dashboard/containers/{}/network-stats - timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        NetworkStatsTimeSeriesDTO networkStats = dashboardService.getNetworkStatsTimeSeries(
                containerId, timeRange, detail
        );

        return ApiResponse.ok(networkStats, "네트워크 통계 시계열 데이터를 성공적으로 조회했습니다.");
    }

    /**
     * 컨테이너의 Block I/O 통계 시계열 데이터 조회
     * GET /api/dashboard/containers/{containerId}/blockio-stats
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위 (ONE_MINUTES, THREE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return Block I/O 통계 시계열 데이터
     */
    @Operation(summary = "Block I/O 통계 시계열 조회",
            description = "컨테이너의 블록 디바이스 읽기/쓰기 속도 시계열 데이터를 조회합니다. detail=false는 대시보드용(50포인트), detail=true는 상세보기용(200포인트)")
    @GetMapping("/containers/{containerId}/blockio-stats")
    public ApiResponse<BlockIOStatsTimeSeriesDTO> getBlockIOStatsTimeSeries(
            @PathVariable Long containerId,
            @Parameter(description = "시간 범위 (ONE_MINUTES, THREE_MINUTES, FIFTEEN_MINUTES, THIRTY_MINUTES, ONE_HOUR)")
            @RequestParam(defaultValue = "ONE_MINUTES") TimeRange timeRange,
            @Parameter(description = "상세 여부 (false: 50포인트, true: 200포인트)")
            @RequestParam(defaultValue = "false") boolean detail) {

        log.info("GET /api/dashboard/containers/{}/blockio-stats - timeRange: {}, detail: {}",
                containerId, timeRange, detail);

        BlockIOStatsTimeSeriesDTO blockIOStats = dashboardService.getBlockIOStatsTimeSeries(
                containerId, timeRange, detail
        );

        return ApiResponse.ok(blockIOStats, "Block I/O 통계 시계열 데이터를 성공적으로 조회했습니다.");
    }

    /**
     * 컨테이너 상세 메트릭 조회 (최초 로드용)
     * GET /api/dashboard/containers/{containerId}/metrics
     * @param containerId 컨테이너 ID
     * @return 컨테이너 상세 메트릭 (중첩 구조, 로그/스토리지 포함)
     */
    @Operation(summary = "컨테이너 상세 메트릭 조회",
            description = "최초 상세 패널 로드 시 사용하는 API. 중첩 구조로 구성된 컨테이너 상세 정보(로그, 스토리지 포함)를 반환합니다.")
    @GetMapping("/containers/{containerId}/metrics")
    public ApiResponse<DashboardContainerDetailDTO> getContainerDetailMetrics(
            @PathVariable Long containerId) {

        log.info("GET /api/dashboard/containers/{}/metrics - 컨테이너 상세 메트릭 조회", containerId);

        DashboardContainerDetailDTO metrics =
                dashboardService.getContainerDetailMetrics(containerId);

        return ApiResponse.ok(metrics, "컨테이너 상세 메트릭을 성공적으로 조회했습니다.");
    }
}
