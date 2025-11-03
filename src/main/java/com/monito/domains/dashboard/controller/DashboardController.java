package com.monito.domains.dashboard.controller;

import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.AgentContainerGroupDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
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
     * 대시보드용 전체 컨테이너 목록 조회
     * GET /api/dashboard/containers
     * @param sortBy 정렬 기준 (선택사항: CPU_PERCENT, MEM_PERCENT, NETWORK_TOTAL_BYTES)
     * @return 전체 컨테이너 목록
     */
    @Operation(summary = "컨테이너 목록조회(정렬 포함)",
            description = "컨테이너 카드에 사용. sortBy 파라미터로 정렬 가능 (CPU_PERCENT, MEM_PERCENT, NETWORK_TOTAL_BYTES)")
    @GetMapping("/containers")
    public ApiResponse<List<ContainerDashboardResponseDTO>> getAllContainers(
            @Parameter(description = "정렬 기준 (CPU_PERCENT, MEM_PERCENT, NETWORK_TOTAL_BYTES)")
            @RequestParam(required = false) ContainerSortType sortBy) {
        log.info("GET /api/dashboard/containers - 대시보드용 컨테이너 목록 조회 (정렬: {})", sortBy);

        List<ContainerDashboardResponseDTO> containers;
        if (sortBy != null) {
            containers = dashboardService.getAllContainersSorted(sortBy);
        } else {
            containers = dashboardService.getAllContainers();
        }

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
     * 즐겨찾기 컨테이너 목록 조회 (대시보드용)
     * GET /api/dashboard/favorites
     * @param userDetails 현재 로그인한 사용자 ID
     * @return 즐겨찾기 컨테이너 목록 (최신 통계 포함)
     */
    @Operation(summary = "즐겨찾기 컨테이너 조회 (대시보드용)",
            description = "로그인한 사용자의 즐겨찾기 컨테이너 목록 조회 (최신 통계 포함)")
    @GetMapping("/favorites")
    public ApiResponse<List<ContainerDashboardResponseDTO>> getFavoriteContainers(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("GET /api/dashboard/favorites - 즐겨찾기 컨테이너 목록 조회 - memberId: {}", userDetails.getId());

        List<ContainerDashboardResponseDTO> favorites = dashboardService.getFavoriteContainers(Long.valueOf(userDetails.getId()));

        return ApiResponse.ok(favorites, "즐겨찾기 컨테이너 목록을 성공적으로 조회했습니다.");
    }
}
