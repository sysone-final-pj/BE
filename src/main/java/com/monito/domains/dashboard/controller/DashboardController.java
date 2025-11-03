package com.monito.domains.dashboard.controller;

import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.dashboard.service.DashboardService;
import com.monito.global.common.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 대시보드 API Controller
 * - 대시보드 화면 전용 API
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * 대시보드용 전체 컨테이너 목록 조회
     * GET /api/dashboard/containers
     * @return 전체 컨테이너 목록
     */
    @GetMapping("/containers")
    public ApiResponse<List<ContainerDashboardResponseDTO>> getAllContainers() {
        log.info("GET /api/dashboard/containers - 대시보드용 컨테이너 목록 조회");

        List<ContainerDashboardResponseDTO> containers = dashboardService.getAllContainers();

        return ApiResponse.ok(containers, "대시보드 컨테이너 목록을 성공적으로 조회했습니다.");
    }

    /**
     * Agent별 컨테이너 개수 집계
     * GET /api/dashboard/containers/count-by-agent
     * @return Agent별 컨테이너 개수 목록 (개수 내림차순)
     */
    @GetMapping("/containers/count-by-agent")
    public ApiResponse<List<AgentContainerCountDTO>> getContainerCountByAgent() {
        log.info("GET /api/dashboard/containers/count-by-agent - Agent별 컨테이너 개수 집계");

        List<AgentContainerCountDTO> agentContainerCounts = dashboardService.getContainerCountByAgent();

        return ApiResponse.ok(agentContainerCounts, "Agent별 컨테이너 개수를 성공적으로 집계했습니다.");
    }
}
