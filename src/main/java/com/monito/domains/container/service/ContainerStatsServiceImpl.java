package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.util.ContainerMetricsCalculator;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import java.math.BigDecimal;
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
                    .memLimit(statsLog.getMemLimit())
                    .memMaxUsage(statsLog.getMemMaxUsage())
                    .blkRead(statsLog.getBlkRead())
                    .blkWrite(statsLog.getBlkWrite())
                    .blkReadPerSec(statsLog.getBlkReadPerSec())
                    .blkWritePerSec(statsLog.getBlkWritePerSec())
                    .rxBytes(statsLog.getRxBytes())
                    .txBytes(statsLog.getTxBytes())
                    .rxPackets(statsLog.getRxPackets())
                    .txPackets(statsLog.getTxPackets())
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
                    .build();

            // 7. INSERT (UPDATE 없음)
            statsLogRepository.save(statsLog);

            log.debug("메트릭 저장 완료 - Container: {}, CPU: {}%, Memory: {}%",
                    metricsDto.getContainerHash(),
                    statsLog.getCpuPercent(),
                    statsLog.getMemPercent()
            );

        } catch (NotFoundException | BadRequestException e) {
            log.error("메트릭 처리 실패 - containerHash: {}, error: {}",
                    metricsDto.getContainerHash(), e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("메트릭 처리 중 예상치 못한 오류 발생 - containerHash: {}",
                    metricsDto.getContainerHash(), e);
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
        // CPU Limit Cores 계산
        BigDecimal cpuLimitCores = metricsCalculator.calculateCpuLimitCores(
                metricsDto.getCpuQuota(),
                metricsDto.getCpuPeriod()
        );

        Container container = Container.builder()
                .agent(agent)
                .containerHash(metricsDto.getContainerHash())
                .name(metricsDto.getContainerHash().substring(0, 12)) // 기본 이름 (해시 앞 12자리)
                .cpuQuota(metricsDto.getCpuQuota())
                .cpuPeriod(metricsDto.getCpuPeriod())
                .cpuLimitCores(cpuLimitCores)
                .onlineCpus(metricsDto.getOnlineCpus())
                .memLimit(metricsDto.getMemLimit())
                .build();

        container = containerRepository.save(container);

        log.info("새 컨테이너 생성 - Agent: {}, ContainerHash: {}, CPU Limit: {} cores",
                agent.getAgentKey(), metricsDto.getContainerHash(), cpuLimitCores);

        return container;
    }
}