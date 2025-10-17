package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.agent.service.AgentHealthTracker;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.response.ContainerLogResponseDto;
import com.monito.domains.container.dto.response.ContainerResponseDto;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

/**
 * 컨테이너 데이터 수집 서비스
 * - Agent API를 호출하여 컨테이너 메트릭 및 로그 수집
 * - 비동기 처리로 여러 Agent를 동시에 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerCollectorService {

    private final WebClient webClient;
    private final ContainerRepository containerRepository;
    private final ContainerStatsLogRepository statsLogRepository;
    private final ContainerLogRepository logRepository;
    private final AgentRepository agentRepository;
    private final AgentHealthTracker healthTracker;

    private static final int MAX_FAILURE_COUNT = 3;
    private static final int REQUEST_TIMEOUT_SECONDS = 5;

    /**
     * Agent로부터 컨테이너 데이터 수집 (비동기)
     * @param agent 데이터를 수집할 Agent
     */
    @Async("agentPollingExecutor") // SchedulingConfig의 @Bean(name = "agentPollingExecutor")의 정보를 통해 진행
    public void collectContainerData(Agent agent) {
        String baseUrl = buildBaseUrl(agent);
        log.debug("Collecting data from agent: {} ({})", agent.getAgentName(), baseUrl);

        try {
            // 1. 컨테이너 목록 조회
            List<ContainerResponseDto> containers = fetchContainers(baseUrl, agent.getApiToken());
            log.debug("Fetched {} containers from agent {}", containers.size(), agent.getAgentName());

            // 2. 각 컨테이너 처리
            containers.forEach(dto -> processContainer(agent, dto));

            // 3. 컨테이너 로그 수집
//             containers.forEach(dto -> collectContainerLogs(agent, dto.getContainerHash()));

            // 4. Agent 상태 업데이트 (성공)
            healthTracker.recordSuccess(agent.getId());
            updateAgentStatusIfNeeded(agent, AgentStatus.ONLINE);

        } catch (WebClientRequestException e) {
            // 네트워크 연결 실패 (타임아웃, 연결 거부 등)
            log.error("Connection failed to agent {} ({}): {}",
                     agent.getAgentName(), baseUrl, e.getMessage());
            handleAgentFailure(agent);

        } catch (WebClientResponseException e) {
            // HTTP 응답 에러 (4xx, 5xx)
            log.error("HTTP error from agent {} ({}): {} - {}",
                     agent.getAgentName(), baseUrl, e.getStatusCode(), e.getMessage());
            handleAgentFailure(agent);

        } catch (Exception e) {
            // 기타 예외
            log.error("Unexpected error collecting data from agent {}: {}",
                     agent.getAgentName(), e.getMessage(), e);
            healthTracker.recordFailure(agent.getId());
        }
    }

    /**
     * Agent API에서 컨테이너 목록 조회
     */
    private List<ContainerResponseDto> fetchContainers(String baseUrl, String apiToken) {
        return webClient.get()
                .uri(baseUrl + "/api/containers")  // Agent API 엔드포인트
                .header("Authorization", "Bearer " + apiToken)
                .retrieve()
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                         response -> Mono.error(new WebClientResponseException(
                                 response.statusCode().value(),
                                 "Agent API error",
                                 null, null, null)))
                .bodyToFlux(ContainerResponseDto.class)
                .collectList()
                .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                .block();
    }

    /**
     * 컨테이너 데이터 처리 및 저장
     */
    @Transactional
    protected void processContainer(Agent agent, ContainerResponseDto dto) {
        try {
            // 기존 컨테이너 찾기 또는 생성
            Container container = containerRepository
                    .findByAgentAndContainerHash(agent, dto.getContainerHash())
                    .orElseGet(() -> createNewContainer(agent, dto));

            // 메트릭 업데이트
            // todo: 컨테이너에서 받아오는 정보 제대로 정의되면 이전 로그 토대로 percent 계산 및 필요한 데이터 정제 작업 진행
            container.updateStats(
                    dto.getStatus(),
                    dto.getCpuPercent(),
                    dto.getMemPercent(),
                    dto.getCpuUsageTotal(),
                    dto.getMemUsage(),
                    dto.getRxBytes(),
                    dto.getTxBytes()
            );

            containerRepository.save(container);

            // 시계열 로그 저장 (히스토리 추적용)
            saveStatsLog(container, dto);

            log.debug("Container data 처리 및 저장 완료: {} ({})", dto.getName(), dto.getContainerHash());

        } catch (Exception e) {
            log.error("Container {} 처리 중 오류 발생: {}", dto.getContainerHash(), e.getMessage(), e);
        }
    }

    /**
     * 새 컨테이너 엔티티 생성
     */
    private Container createNewContainer(Agent agent, ContainerResponseDto dto) {
        return Container.builder()
                .agent(agent)
                .containerHash(dto.getContainerHash())
                .name(dto.getName())
                .status(dto.getStatus())
                .cpuPercent(dto.getCpuPercent())
                .hostCpuUsageTotal(dto.getHostCpuUsageTotal())
                .cpuUsageTotal(dto.getCpuUsageTotal())
                .cpuUser(dto.getCpuUser())
                .cpuSystem(dto.getCpuSystem())
                .cpuQuota(dto.getCpuQuota())
                .cpuPeriod(dto.getCpuPeriod())
                .cpuLimit(dto.getCpuLimit())
                .onlineCpus(dto.getOnlineCpus())
                .throttlingPeriods(dto.getThrottlingPeriods())
                .throttledPeriods(dto.getThrottledPeriods())
                .throttledTime(dto.getThrottledTime())
                .oomKills(dto.getOomKills())
                .memPercent(dto.getMemPercent())
                .memUsage(dto.getMemUsage())
                .memLimit(dto.getMemLimit())
                .memMaxUsage(dto.getMemMaxUsage())
                .memRss(dto.getMemRss())
                .memCache(dto.getMemCache())
                .blkRead(dto.getBlkRead())
                .blkWrite(dto.getBlkWrite())
                .rxBytes(dto.getRxBytes())
                .txBytes(dto.getTxBytes())
                .rxMbps(dto.getRxMbps())
                .txMbps(dto.getTxMbps())
                .rxPps(dto.getRxPps())
                .txPps(dto.getTxPps())
                .rxErrors(dto.getRxErrors())
                .txErrors(dto.getTxErrors())
                .rxDropped(dto.getRxDropped())
                .txDropped(dto.getTxDropped())
                .build();
    }

    /**
     * 컨테이너 통계 로그 저장 (시계열 데이터)
     */
    private void saveStatsLog(Container container, ContainerResponseDto dto) {
        // todo: 컨테이너에서 받아오는 정보 제대로 정의되면 percent 계산 및 필요한 데이터 정제 작업 진행
        try {
            ContainerStatsLog log = ContainerStatsLog.builder()
                    .container(container)
                    .cpuPercent(dto.getCpuPercent())
                    .memPercent(dto.getMemPercent())
                    .memUsage(dto.getMemUsage())
                    .rxBytes(dto.getRxBytes())
                    .txBytes(dto.getTxBytes())
                    .build();

            statsLogRepository.save(log);
        } catch (Exception e) {
            log.error("Container Status 저장 실패 -> container {}: {}", container.getId(), e.getMessage());
        }
    }

    /**
     * 컨테이너 로그 수집
     */
    private void collectContainerLogs(Agent agent, String containerHash) {
        try {
            String baseUrl = buildBaseUrl(agent);
            List<ContainerLogResponseDto> logs = webClient.get()
                    .uri(baseUrl + "/api/containers/" + containerHash + "/logs")
                    .header("Authorization", "Bearer " + agent.getApiToken())
                    .retrieve()
                    .bodyToFlux(ContainerLogResponseDto.class)
                    .collectList()
                    .timeout(Duration.ofSeconds(REQUEST_TIMEOUT_SECONDS))
                    .block();

            // 로그 저장 로직
            // TODO: 실제 로그 저장 구현

        } catch (Exception e) {
            log.debug("Container log 수집 실패 container {}: {}", containerHash, e.getMessage());
        }
    }

    /**
     * Agent 실패 처리
     */
    @Transactional
    protected void handleAgentFailure(Agent agent) {
        healthTracker.recordFailure(agent.getId());
        int failureCount = healthTracker.getFailureCount(agent.getId());

        if (failureCount >= MAX_FAILURE_COUNT) {
            log.warn("에이전트 {}가 {}회 연속 실패로 오프라인 상태로 변경됨",
                    agent.getAgentName(), failureCount);
            updateAgentStatusIfNeeded(agent, AgentStatus.OFFLINE);

            // TODO: Alert 생성 로직 추가
        } else {
            log.debug("Agent {} 실패 횟수: {}/{}", agent.getAgentName(), failureCount, MAX_FAILURE_COUNT);
        }
    }

    /**
     * Agent 상태 업데이트 (변경이 필요한 경우만)
     */
    @Transactional
    protected void updateAgentStatusIfNeeded(Agent agent, AgentStatus newStatus) {
        if (agent.getAgentStatus() != newStatus) {
            agent.updateStatus(newStatus);
            agentRepository.save(agent);
            log.info("Agent {} 상태 변경: {} -> {}", agent.getAgentName(), agent.getAgentStatus(), newStatus);
        }
    }

    /**
     * Agent의 base URL 생성
     */
    private String buildBaseUrl(Agent agent) {
        return String.format("http://%s:%d", agent.getHostIp(), agent.getHostPort());
    }
}