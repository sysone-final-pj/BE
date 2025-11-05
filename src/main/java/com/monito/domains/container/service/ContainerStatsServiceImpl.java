package com.monito.domains.container.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.alert.facade.AlertEvaluationFacade;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.global.cache.CpuMetricsBufferCache;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.util.ContainerMetricsCalculator;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 컨테이너 통계 수집 및 저장 서비스 구현체
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerStatsServiceImpl implements ContainerStatsService {

    private final ContainerStatsLogRepository statsLogRepository;
    private final ContainerRepository containerRepository;
    private final AgentRepository agentRepository;
    private final ContainerMetricsCalculator metricsCalculator;
    private final AlertEvaluationFacade alertEvaluationFacade;
    private final SimpMessagingTemplate messagingTemplate;
    private final CpuMetricsBufferCache cpuMetricsBufferCache;

    @Override
    @Transactional
    public void processMetrics(String agentKey, ContainerMetricsRequestDTO metricsDto) {
        try {
            // 1. 입력값 검증
            validateMetricsRequest(metricsDto);

            // 2. Agent 조회
            Agent agent = agentRepository.findByAgentKey(agentKey)
                    .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));

            // 3. Container 조회 또는 생성
            Container container = containerRepository
                    .findByAgentAndContainerHash(agent, metricsDto.getContainerHash())
                    .orElseGet(() -> createNewContainer(agent, metricsDto));

            initializeSpecsIfFirstMetrics(container, metricsDto);

            // 4. 이전 통계 조회 (계산용)
            ContainerStatsLog previousStats = statsLogRepository
                    .findLatestByContainerHash(metricsDto.getContainerHash())
                    .orElse(null);

            // [DEBUG] 이전 데이터 확인
            if (previousStats == null) {
                log.info("[CPU DEBUG] 이전 통계 없음 - 첫 수집 (containerHash: {})", metricsDto.getContainerHash());
            } else {
                log.info("[CPU DEBUG] 이전 통계 조회 성공 - containerHash: {}, prevCpuUsage: {}, prevHostCpuUsage: {}, createdAt: {}",
                        metricsDto.getContainerHash(),
                        previousStats.getCpuUsageTotal(),
                        previousStats.getHostCpuUsageTotal(),
                        previousStats.getCreatedAt());
            }
            log.info("[CPU DEBUG] 현재 메트릭 - cpuUsage: {}, hostCpuUsage: {}, onlineCpus: {}",
                    metricsDto.getCpuUsageTotal(),
                    metricsDto.getHostCpuUsageTotal(),
                    metricsDto.getOnlineCpus());

            // 5. 메트릭 계산 및 StatsLog 생성
            ContainerStatsLog statsLog = metricsCalculator.calculateAndBuild(
                    metricsDto,
                    previousStats
            );

            // 6. Container 연결
            statsLog = ContainerStatsLog.builder()
                    .container(container)
                    .containerHash(statsLog.getContainerHash())
                    .state(statsLog.getState())
                    .health(statsLog.getHealth())
                    .collectedAt(statsLog.getCollectedAt())
                    .cpuPercent(statsLog.getCpuPercent())
                    .cpuCoreUsage(statsLog.getCpuCoreUsage())
                    .hostCpuUsageTotal(statsLog.getHostCpuUsageTotal())
                    .cpuUsageTotal(statsLog.getCpuUsageTotal())
                    .cpuUser(statsLog.getCpuUser())
                    .cpuSystem(statsLog.getCpuSystem())
                    .cpuQuota(statsLog.getCpuQuota())
                    .cpuPeriod(statsLog.getCpuPeriod())
                    .onlineCpus(statsLog.getOnlineCpus())
                    .throttlingPeriods(statsLog.getThrottlingPeriods())
                    .throttledPeriods(statsLog.getThrottledPeriods())
                    .throttledTime(statsLog.getThrottledTime())
                    .memPercent(statsLog.getMemPercent())
                    .memUsage(statsLog.getMemUsage())
                    .memMaxUsage(statsLog.getMemMaxUsage())
                    .blkRead(statsLog.getBlkRead())
                    .blkWrite(statsLog.getBlkWrite())
                    .blkReadPerSec(statsLog.getBlkReadPerSec())
                    .blkWritePerSec(statsLog.getBlkWritePerSec())
                    .rxBytes(statsLog.getRxBytes())
                    .txBytes(statsLog.getTxBytes())
                    .rxPackets(statsLog.getRxPackets())
                    .txPackets(statsLog.getTxPackets())
                    .networkTotalBytes(statsLog.getNetworkTotalBytes())
                    .rxBytesPerSec(statsLog.getRxBytesPerSec())
                    .txBytesPerSec(statsLog.getTxBytesPerSec())
                    .rxPps(statsLog.getRxPps())
                    .txPps(statsLog.getTxPps())
                    .rxFailureRate(statsLog.getRxFailureRate())
                    .txFailureRate(statsLog.getTxFailureRate())
                    .rxErrors(statsLog.getRxErrors())
                    .txErrors(statsLog.getTxErrors())
                    .rxDropped(statsLog.getRxDropped())
                    .txDropped(statsLog.getTxDropped())
                    .sizeRw(statsLog.getSizeRw())
                    .sizeRootFs(statsLog.getSizeRootFs())
                    .build();

            // 7. INSERT (UPDATE 없음)
            statsLogRepository.save(statsLog);

            log.debug("메트릭 저장 완료 - Container: {}, CPU: {}%, Memory: {}%",
                    metricsDto.getContainerHash(),
                    statsLog.getCpuPercent(),
                    statsLog.getMemPercent()
            );

            // 8. CPU 통계치 계산을 위한 캐싱 처리
            cpuMetricsBufferCache.addCpuSample(container.getId(), statsLog.getCpuPercent());

            // 9. 알림 규칙 평가 (자동 알림 발생)
            try {
                alertEvaluationFacade.evaluateContainerStats(statsLog);
            } catch (Exception e) {
                log.error("알림 규칙 평가 중 오류 발생 - containerHash: {}, error: {}",
                        metricsDto.getContainerHash(), e.getMessage());
            }

            // 10. STOMP 메시지 브로드캐스트 (모든 상세 메트릭 포함)
            try {
                ContainerDashboardResponseDTO dashboardDto = ContainerDashboardResponseDTO.builder()
                        // 기본 정보
                        .containerId(container.getId())
                        .containerHash(container.getContainerHash())
                        .containerName(container.getName())
                        .agentId(agent.getId())
                        .agentName(agent.getAgentName())
                        .state(statsLog.getState())
                        .health(statsLog.getHealth())
                        .imageName(container.getImageName())
                        .imageSize(container.getImageSize())
                        // CPU 메트릭
                        .cpuPercent(statsLog.getCpuPercent())
                        .cpuCoreUsage(statsLog.getCpuCoreUsage())
                        .cpuUsageTotal(statsLog.getCpuUsageTotal())
                        .hostCpuUsageTotal(statsLog.getHostCpuUsageTotal())
                        .cpuUser(statsLog.getCpuUser())
                        .cpuSystem(statsLog.getCpuSystem())
                        .cpuQuota(statsLog.getCpuQuota())
                        .cpuPeriod(statsLog.getCpuPeriod())
                        .onlineCpus(statsLog.getOnlineCpus())
                        .throttlingPeriods(statsLog.getThrottlingPeriods())
                        .throttledPeriods(statsLog.getThrottledPeriods())
                        .throttledTime(statsLog.getThrottledTime())
                        // Memory 메트릭
                        .memPercent(statsLog.getMemPercent())
                        .memUsage(statsLog.getMemUsage())
                        .memLimit(container.getMemLimit())
                        .memMaxUsage(statsLog.getMemMaxUsage())
                        // Block I/O 메트릭
                        .blkRead(statsLog.getBlkRead())
                        .blkWrite(statsLog.getBlkWrite())
                        .blkReadPerSec(statsLog.getBlkReadPerSec())
                        .blkWritePerSec(statsLog.getBlkWritePerSec())
                        // Network 메트릭
                        .rxBytes(statsLog.getRxBytes())
                        .txBytes(statsLog.getTxBytes())
                        .rxPackets(statsLog.getRxPackets())
                        .txPackets(statsLog.getTxPackets())
                        .networkTotalBytes(statsLog.getNetworkTotalBytes())
                        .rxBytesPerSec(statsLog.getRxBytesPerSec())
                        .txBytesPerSec(statsLog.getTxBytesPerSec())
                        .rxPps(statsLog.getRxPps())
                        .txPps(statsLog.getTxPps())
                        .rxFailureRate(statsLog.getRxFailureRate())
                        .txFailureRate(statsLog.getTxFailureRate())
                        .rxErrors(statsLog.getRxErrors())
                        .txErrors(statsLog.getTxErrors())
                        .rxDropped(statsLog.getRxDropped())
                        .txDropped(statsLog.getTxDropped())
                        // Storage 메트릭
                        .sizeRw(statsLog.getSizeRw())
                        .sizeRootFs(statsLog.getSizeRootFs())
                        .build();

                // STOMP를 통한 브로드캐스트 (/topic/dashboard 구독자 전체에게 전송)
                messagingTemplate.convertAndSend("/topic/dashboard", dashboardDto);

                log.debug("대시보드 STOMP 브로드캐스트 전송 완료 - Container: {}", container.getName());
            } catch (Exception e) {
                log.error("대시보드 브로드캐스트 실패 - containerHash: {}", metricsDto.getContainerHash(), e);
            }

        } catch (NotFoundException | BadRequestException e) {
            log.error("메트릭 처리 실패 - containerHash: {}, error: {}",
                    metricsDto.getContainerHash(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메트릭 처리 중 예상치 못한 오류 발생 - containerHash: {}", metricsDto.getContainerHash(), e);
            throw new BadRequestException(ExceptionMessage.CONTAINER_METRICS_PROCESSING_FAILED);
        }
    }

    /**
     * 메트릭 요청 유효성 검증
     */
    private void validateMetricsRequest(ContainerMetricsRequestDTO metricsDto) {
        if (metricsDto.getContainerHash() == null || metricsDto.getContainerHash().trim().isEmpty()) {
            throw new BadRequestException(ExceptionMessage.CONTAINER_HASH_INVALID);
        }
        if (metricsDto.getContainerHash().length() < 12) {
            throw new BadRequestException(ExceptionMessage.CONTAINER_HASH_INVALID);
        }
    }

    /**
     * 새 컨테이너 생성
     */
    private Container createNewContainer(Agent agent, ContainerMetricsRequestDTO metricsDto) {
        BigDecimal cpuLimitCores = metricsCalculator.calculateCpuLimitCores(
                metricsDto.getCpuQuota(),
                metricsDto.getCpuPeriod()
        );

        Container container = Container.builder()
                .agent(agent)
                .containerHash(metricsDto.getContainerHash())
                .state(metricsDto.getState())
                .name(metricsDto.getContainerName())
                .cpuQuota(metricsDto.getCpuQuota())
                .cpuPeriod(metricsDto.getCpuPeriod())
                .cpuLimitCores(cpuLimitCores)
                .onlineCpus(metricsDto.getOnlineCpus())
                .memLimit(metricsDto.getMemLimit())
                .imageName(metricsDto.getImageName())
                .imageSize(metricsDto.getImageSize())
                .storageLimit(metricsDto.getStorageLimit())
                .build();

        container = containerRepository.save(container);

        log.info("새 컨테이너 생성 - Agent: {}, ContainerHash: {}, CPU Limit: {} cores",
                agent.getAgentKey(), metricsDto.getContainerHash(), cpuLimitCores);

        return container;
    }

    /**
     * 최초 메트릭 수신 시 컨테이너 리소스 스펙 초기화
     */
    private void initializeSpecsIfFirstMetrics(Container container, ContainerMetricsRequestDTO metric) {

        if (Boolean.TRUE.equals(container.getMetricsInitialized())) {
            return; // 이미 초기화됨 → Skip
        }

        BigDecimal cpuLimitCores = null;
        if (metric.getCpuQuota() != null && metric.getCpuPeriod() > 0) {
            cpuLimitCores = BigDecimal.valueOf(metric.getCpuQuota())
                    .divide(BigDecimal.valueOf(metric.getCpuPeriod()), 2, RoundingMode.HALF_UP);
        }

        container.updateSpecs(
                metric.getCpuQuota(),
                metric.getCpuPeriod(),
                cpuLimitCores,
                metric.getOnlineCpus(),
                metric.getMemLimit(),
                metric.getStorageLimit()
        );

        container.markMetricsInitialized();
        containerRepository.save(container);

        log.info("최초 메트릭 수신 → 컨테이너 스펙 초기화 완료: {}", container.getContainerHash());
    }
}