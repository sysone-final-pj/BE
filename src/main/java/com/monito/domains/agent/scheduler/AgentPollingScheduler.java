package com.monito.domains.agent.scheduler;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.service.ContainerCollectorService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Agent 폴링 스케줄러
 * - ONLINE 상태의 모든 Agent에서 컨테이너 데이터 수집
 * - fixedDelayString: 이전 실행이 완료된 후 대기 시간 (application.yml 설정)
 * - initialDelayString: 애플리케이션 시작 후 초기 지연 시간 (application.yml 설정)
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentPollingScheduler {

    private final AgentRepository agentRepository;
    private final ContainerCollectorService collectorService;

    @Scheduled(
        fixedDelayString = "${app.scheduler.agent-polling.fixed-delay}",
        initialDelayString = "${app.scheduler.agent-polling.initial-delay}"
    )
    public void pollAllAgents() {
        List<Agent> activeAgents = agentRepository.findByAgentStatus(AgentStatus.ONLINE);

        if (activeAgents.isEmpty()) {
            log.debug("No active agents to poll");
            return;
        }

        // 각 Agent별로 비동기 수집 실행
        activeAgents.forEach(agent -> {
            try {
                collectorService.collectContainerData(agent);
            } catch (Exception e) {
                log.error("Unexpected error while polling agent {}: {}",
                         agent.getAgentName(), e.getMessage(), e);
            }
        });
    }
}