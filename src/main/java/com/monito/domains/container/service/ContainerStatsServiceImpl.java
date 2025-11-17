package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.alert.facade.AlertEvaluationFacade;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.dto.response.ContainerSummarySnapshot;
import com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO;
import com.monito.domains.dashboard.dto.response.DashboardContainerDetailDTO;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.dashboard.repository.DashboardRepository;
import com.monito.global.cache.ContainerSummaryCache;
import com.monito.global.cache.CpuMetricsBufferCache;
import com.monito.infrastructure.messaging.StompMessagingClient;
import com.monito.infrastructure.messaging.WsTopics;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.util.ContainerMetricsCalculator;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final ContainerLogRepository containerLogRepository;
    private final DashboardRepository dashboardRepository;
    private final AgentRepository agentRepository;
    private final ContainerMetricsCalculator metricsCalculator;
    private final AlertEvaluationFacade alertEvaluationFacade;
    private final SimpMessagingTemplate messagingTemplate;
    private final CpuMetricsBufferCache cpuMetricsBufferCache;
    private final StompMessagingClient messagingClient;
    private final ContainerSummaryCache containerSummaryCache;

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

            updateSpecsIfChanged(container, metricsDto);

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
            statsLog = ContainerStatsLog.of(container, statsLog);

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

            // 10. 컨테이너 리스트 브로드캐스트 (/topic/dashboard/list)
            try {
                ContainerCardResponseDTO cardDto = ContainerCardResponseDTO.of(container, statsLog);
                messagingTemplate.convertAndSend(WsTopics.DASHBOARD_STATUS, cardDto);

                log.debug("대시보드 리스트 브로드캐스트 전송 완료 - Container: {}", container.getName());
            } catch (Exception e) {
                log.error("대시보드 리스트 브로드캐스트 실패 - containerHash: {}", metricsDto.getContainerHash(), e);
            }

            // 10-1. 대시보드 컨테이너 상세 발행 (/topic/dashboard/detail/{id})
            // - logs, storage 집계 데이터 포함
            try {
                DashboardContainerDetailDTO dashboardDetail = DashboardContainerDetailDTO.forRealtimeUpdateWithMetrics(
                        container, agent, statsLog, containerLogRepository, dashboardRepository, LocalDate.now()
                );
                messagingClient.send(WsTopics.dashboardDetail(container.getId()), dashboardDetail);

                log.debug("대시보드 상세 정보 발행 완료 (집계 포함) - containerId: {}, containerName: {}",
                    container.getId(), container.getName());
            } catch (Exception e) {
                log.error("대시보드 상세 정보 발행 실패 - containerId: {}, error: {}",
                    container.getId(), e.getMessage(), e);
            }

            // 11. 컨테이너 상세 메트릭 발행 (/topic/container/{id}/metrics)
            try {
                ContainerDetailResponseDTO detailMetrics = ContainerDetailResponseDTO.forRealtimeUpdate(container, agent, statsLog);
                messagingClient.send(WsTopics.containerMetrics(container.getId()), detailMetrics);

                log.info("컨테이너 상세 메트릭 발행 완료 - containerId: {}, containerName: {}",
                    container.getId(), container.getName());
            } catch (Exception e) {
                log.error("컨테이너 상세 메트릭 발행 실패 - containerId: {}, error: {}",
                    container.getId(), e.getMessage(), e);
            }

            // 캐시에 Snapshot 업데이트
            containerSummaryCache.update(ContainerSummarySnapshot.of(container, statsLog));

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
                .isCpuUnlimited(metricsDto.getIsCpuUnlimited())
                .memLimit(metricsDto.getMemLimit())
                .isMemoryUnlimited(metricsDto.getIsMemoryUnlimited())
                .imageName(metricsDto.getImageName())
                .imageId(metricsDto.getImageId())
                .imageSize(metricsDto.getImageSize())
                .storageLimit(metricsDto.getStorageLimit())
                .isStorageUnlimited(metricsDto.getIsStorageUnlimited())
                .build();

        container = containerRepository.save(container);

        log.info("새 컨테이너 생성 - Agent: {}, ContainerHash: {}, CPU Limit: {} cores, isCpuUnlimited: {}, isMemoryUnlimited: {}, isStorageUnlimited: {}",
                agent.getAgentKey(), metricsDto.getContainerHash(), cpuLimitCores,
                metricsDto.getIsCpuUnlimited(), metricsDto.getIsMemoryUnlimited(), metricsDto.getIsStorageUnlimited());

        return container;
    }

    /**
     * 리소스 제한값이 변경되었으면 업데이트 (실시간 반영)
     * - docker update 등으로 실행 중 리소스 변경 시 자동 반영
     */
    private void updateSpecsIfChanged(Container container, ContainerMetricsRequestDTO metric) {
        // 리소스 제한값 변경 감지
        boolean changed = !container.getCpuQuota().equals(metric.getCpuQuota()) ||
                          !container.getMemLimit().equals(metric.getMemLimit()) ||
                          !container.getStorageLimit().equals(metric.getStorageLimit()) ||
                          !container.getIsCpuUnlimited().equals(metric.getIsCpuUnlimited()) ||
                          !container.getIsMemoryUnlimited().equals(metric.getIsMemoryUnlimited()) ||
                          !container.getIsStorageUnlimited().equals(metric.getIsStorageUnlimited());

        if (!changed) {
            return; // 변경 없음 → Skip
        }

        // CPU Limit Cores 계산
        BigDecimal cpuLimitCores = metricsCalculator.calculateCpuLimitCores(
                metric.getCpuQuota(),
                metric.getCpuPeriod()
        );

        // 리소스 스펙 업데이트 (이미지 정보 포함)
        container.updateSpecs(
                metric.getCpuQuota(),
                metric.getCpuPeriod(),
                cpuLimitCores,
                metric.getOnlineCpus(),
                metric.getIsCpuUnlimited(),
                metric.getMemLimit(),
                metric.getIsMemoryUnlimited(),
                metric.getStorageLimit(),
                metric.getIsStorageUnlimited(),
                metric.getImageName(),
                metric.getImageId(),
                metric.getImageSize()
        );

        log.info("리소스 제한값 변경 감지 및 업데이트 완료 - containerHash: {}, cpuQuota: {}, isCpuUnlimited: {}, memLimit: {}, isMemoryUnlimited: {}, storageLimit: {}, isStorageUnlimited: {}, imageId: {}",
                container.getContainerHash(), metric.getCpuQuota(), metric.getIsCpuUnlimited(),
                metric.getMemLimit(), metric.getIsMemoryUnlimited(), metric.getStorageLimit(), metric.getIsStorageUnlimited(),
                metric.getImageId());
    }
}