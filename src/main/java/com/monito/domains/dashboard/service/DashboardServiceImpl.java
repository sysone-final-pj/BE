package com.monito.domains.dashboard.service;

import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.dashboard.repository.DashboardRepository;
import java.util.List;
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
    public List<ContainerDashboardResponseDTO> getAllContainers() {
        log.info("대시보드: 전체 컨테이너 목록 조회");
        return dashboardRepository.findAllContainersForDashboard();
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
}
