package com.monito.domains.dashboard.service;

import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import java.util.List;

/**
 * 대시보드 서비스
 * - 대시보드 화면에 필요한 데이터 제공
 */
public interface DashboardService {

    /**
     * 전체 컨테이너 목록 조회 (최신 통계 포함)
     * @return 전체 컨테이너 목록
     */
    List<ContainerDashboardResponseDTO> getAllContainers();

    /**
     * 특정 Agent의 컨테이너 목록 조회 (최신 통계 포함)
     * @param agentId Agent ID
     * @return Agent에 속한 컨테이너 목록
     */
    List<ContainerDashboardResponseDTO> getContainersByAgentId(Long agentId);
}
