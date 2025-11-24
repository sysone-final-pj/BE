package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.alert.facade.AlertEvaluationFacade;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.dto.response.ContainerSummarySnapshot;
import com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO;
import com.monito.domains.dashboard.dto.response.DashboardContainerDetailDTO;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.dashboard.repository.DashboardRepository;
import com.monito.global.cache.ContainerLastStatsCache;
import com.monito.global.cache.ContainerSummaryCache;
import com.monito.global.cache.CpuMetricsBufferCache;
import com.monito.infrastructure.messaging.StompMessagingClient;
import com.monito.infrastructure.messaging.WsTopics;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import com.monito.domains.container.repository.ContainerStatsLogRepository;
import com.monito.domains.container.util.ContainerMetricsCalculator;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private final ContainerLastStatsCache lastStatsCache;
    private final Executor broadcastTaskExecutor;

    @Override
    @Transactional
    public void processMetrics(String agentKey, ContainerMetricsRequestDTO metricsDto) {
        try {
            // 1. 입력값 검증
            validateMetricsRequest(metricsDto);

            // 2. running 상태가 아닌 컨테이너는 메트릭 수집 스킵
            // (created, restarting, paused, exited, dead 등은 메트릭이 null이거나 의미 없음)
            if (!isRunningState(metricsDto)) {
                log.debug("메트릭 수집 스킵 - 컨테이너 상태가 running이 아님: {}, Hash: {}",
                        metricsDto.getState(), metricsDto.getContainerHash());
                return;
            }

            // 3. running 상태여도 필수 메트릭이 null이면 스킵 (막 시작된 컨테이너)
            if (!hasRequiredMetrics(metricsDto)) {
                log.debug("메트릭 수집 스킵 - 필수 메트릭이 null (컨테이너 시작 중): {}, Hash: {}",
                        metricsDto.getState(), metricsDto.getContainerHash());
                return;
            }

            // 3. Agent 조회
            Agent agent = agentRepository.findByAgentKey(agentKey)
                    .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));

            // 3. Container 조회 또는 생성
            Container container = containerRepository
                    .findByAgentAndContainerHash(agent, metricsDto.getContainerHash())
                    .orElseGet(() -> createNewContainer(agent, metricsDto));

            updateSpecsIfChanged(container, metricsDto);

            // 4. 이전 통계 조회 (캐시 우선, DB는 폴백)
            ContainerStatsLog previousStats = lastStatsCache.get(metricsDto.getContainerHash());

            // 캐시에 없으면 DB에서 조회 (첫 수집 시)
            if (previousStats == null) {
                previousStats = statsLogRepository
                        .findLatestByContainerHash(
                                metricsDto.getContainerHash(),
                                LocalDateTime.now().minusHours(1)
                        )
                        .orElse(null);

                if (previousStats != null) {
                    log.info("[CACHE] 캐시 미스 - DB에서 이전 통계 조회 성공 (containerHash: {})", metricsDto.getContainerHash());
                }
            } else {
                log.debug("[CACHE] 캐시 히트 - 이전 통계 조회 (containerHash: {})", metricsDto.getContainerHash());
            }

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

            // 5. 메트릭 계산 및 StatsLog 생성 (CPU 제한 정보 포함)
            ContainerStatsLog statsLog = metricsCalculator.calculateAndBuild(
                    metricsDto,
                    previousStats,
                    container.getCpuLimitCores(),
                    container.getIsCpuUnlimited()
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

            // 8. 최신 통계 캐시 업데이트 (다음 메트릭 계산 시 사용)
            lastStatsCache.put(metricsDto.getContainerHash(), statsLog);

            // 9. CPU 통계치 계산을 위한 캐싱 처리
            cpuMetricsBufferCache.addCpuSample(container.getId(), statsLog.getCpuPercent());

            // 10. 알림 규칙 평가 (자동 알림 발생)
            try {
                alertEvaluationFacade.evaluateContainerStats(statsLog);
            } catch (Exception e) {
                log.error("알림 규칙 평가 중 오류 발생 - containerHash: {}, error: {}",
                        metricsDto.getContainerHash(), e.getMessage());
            }

            // 11. 컨테이너 리스트 브로드캐스트 (/topic/dashboard/list)
            try {
                ContainerCardResponseDTO cardDto = ContainerCardResponseDTO.of(container, statsLog);
                messagingTemplate.convertAndSend(WsTopics.DASHBOARD_STATUS, cardDto);

                log.debug("대시보드 리스트 브로드캐스트 전송 완료 - Container: {}", container.getName());
            } catch (Exception e) {
                log.error("대시보드 리스트 브로드캐스트 실패 - containerHash: {}", metricsDto.getContainerHash(), e);
            }

            // 12. 대시보드 컨테이너 상세 발행 (/topic/dashboard/detail/{id})
            // - 실시간 메트릭만 전송 (logs, storage는 클라이언트에서 별도 조회)
            try {
                DashboardContainerDetailDTO dashboardDetail = DashboardContainerDetailDTO.forRealtimeUpdate(
                        container, agent, statsLog
                );
                messagingClient.send(WsTopics.dashboardDetail(container.getId()), dashboardDetail);

                log.debug("대시보드 상세 정보 발행 완료 - containerId: {}, containerName: {}",
                        container.getId(), container.getName());
            } catch (Exception e) {
                log.error("대시보드 상세 정보 발행 실패 - containerId: {}, error: {}",
                        container.getId(), e.getMessage(), e);
            }

            // 13. 컨테이너 상세 메트릭 발행 (/topic/container/{id}/metrics)
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
     * running 상태인지 확인
     * - Docker의 컨테이너는 running 상태일 때만 의미있는 메트릭을 제공
     * - created, restarting, paused, exited, dead 등의 상태는 메트릭이 null이거나 의미 없음
     */
    private boolean isRunningState(ContainerMetricsRequestDTO metricsDto) {
        return ContainerState.RUNNING.equals(metricsDto.getState());
    }

    /**
     * 필수 메트릭이 존재하는지 확인
     * - running 상태여도 막 시작된 컨테이너는 메트릭이 null일 수 있음
     * - Docker가 메트릭 수집을 시작하기까지 1-2초 소요
     */
    private boolean hasRequiredMetrics(ContainerMetricsRequestDTO metricsDto) {
        return metricsDto.getOnlineCpus() != null
                && metricsDto.getMemUsage() != null
                && metricsDto.getMemLimit() != null;
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

    @Override
    public void processMetricsBatch(String agentKey, List<ContainerMetricsRequestDTO> metricsList) {
        if (metricsList == null || metricsList.isEmpty()) {
            return;
        }

        long startTime = System.currentTimeMillis();
        int totalMetrics = metricsList.size();

        try {
            // 1. Agent 조회 (읽기 전용)
            Agent agent = findAgentReadOnly(agentKey);

            List<ContainerStatsLog> statsLogList = new ArrayList<>();
            List<ContainerSummarySnapshot> snapshotList = new ArrayList<>();

            // 2. 각 메트릭 처리 (계산 및 준비) - 트랜잭션 외부에서 처리
            for (ContainerMetricsRequestDTO metricsDto : metricsList) {
                try {
                    // 검증
                    validateMetricsRequest(metricsDto);

                    // running 상태가 아니거나 필수 메트릭이 없으면 스킵
                    if (!isRunningState(metricsDto) || !hasRequiredMetrics(metricsDto)) {
                        log.debug("메트릭 수집 스킵 - containerHash: {}, state: {}",
                                metricsDto.getContainerHash(), metricsDto.getState());
                        continue;
                    }

                    // Container 조회 또는 생성 (필요시 쓰기 트랜잭션)
                    Container container = findOrCreateContainer(agent, metricsDto);

                    // 이전 통계 조회 (캐시 우선)
                    ContainerStatsLog previousStats = lastStatsCache.get(metricsDto.getContainerHash());
                    if (previousStats == null) {
                        previousStats = findLatestStatsReadOnly(metricsDto.getContainerHash());
                    }

                    // 메트릭 계산 및 StatsLog 생성
                    ContainerStatsLog statsLog = metricsCalculator.calculateAndBuild(
                            metricsDto,
                            previousStats,
                            container.getCpuLimitCores(),
                            container.getIsCpuUnlimited()
                    );

                    // Container 연결
                    statsLog = ContainerStatsLog.of(container, statsLog);
                    statsLogList.add(statsLog);

                    // 캐시 및 스냅샷 준비
                    lastStatsCache.put(metricsDto.getContainerHash(), statsLog);
                    cpuMetricsBufferCache.addCpuSample(container.getId(), statsLog.getCpuPercent());
                    snapshotList.add(ContainerSummarySnapshot.of(container, statsLog));

                } catch (Exception e) {
                    log.error("메트릭 처리 실패 (배치) - containerHash: {}, error: {}",
                            metricsDto.getContainerHash(), e.getMessage());
                }
            }

            // 3. Batch INSERT (짧은 쓰기 트랜잭션으로 분리)
            if (!statsLogList.isEmpty()) {
                saveStatsLogBatch(statsLogList);

                long elapsedTime = System.currentTimeMillis() - startTime;
                log.info("✅ 배치 메트릭 저장 완료 - agentKey: {}, 성공: {}/{}, 소요시간: {}ms",
                        agentKey, statsLogList.size(), totalMetrics, elapsedTime);

                // 4. 캐시 업데이트 (트랜잭션 외부)
                for (ContainerSummarySnapshot snapshot : snapshotList) {
                    containerSummaryCache.update(snapshot);
                }
            }

            // 5. 비동기 후처리 (트랜잭션 외부 - 별도 스레드에서 실행)
            if (!statsLogList.isEmpty()) {
                List<ContainerStatsLog> finalStatsList = new ArrayList<>(statsLogList);
                Agent finalAgent = agent;

                CompletableFuture.runAsync(() -> {
                    for (ContainerStatsLog statsLog : finalStatsList) {
                        Container container = statsLog.getContainer();

                        // 알림 규칙 평가
                        try {
                            alertEvaluationFacade.evaluateContainerStats(statsLog);
                        } catch (Exception e) {
                            log.error("알림 규칙 평가 중 오류 발생 - containerHash: {}",
                                    statsLog.getContainerHash(), e);
                        }

                        // WebSocket 브로드캐스트 (DB 조회 없이 실시간 메트릭만 전송)
                        try {
                            ContainerCardResponseDTO cardDto = ContainerCardResponseDTO.of(container, statsLog);
                            messagingTemplate.convertAndSend(WsTopics.DASHBOARD_STATUS, cardDto);

                            // DB 조회 없는 버전 사용 (비동기 스레드에서 connection leak 방지)
                            DashboardContainerDetailDTO dashboardDetail = DashboardContainerDetailDTO.forRealtimeUpdate(
                                    container, finalAgent, statsLog
                            );
                            messagingClient.send(WsTopics.dashboardDetail(container.getId()), dashboardDetail);

                            ContainerDetailResponseDTO detailMetrics = ContainerDetailResponseDTO.forRealtimeUpdate(container, finalAgent, statsLog);
                            messagingClient.send(WsTopics.containerMetrics(container.getId()), detailMetrics);
                        } catch (Exception e) {
                            log.error("WebSocket 브로드캐스트 실패 - containerId: {}", container.getId(), e);
                        }
                    }
                    log.debug("비동기 브로드캐스트 완료 - {} 건", finalStatsList.size());
                }, broadcastTaskExecutor);
            }

        } catch (NotFoundException | BadRequestException e) {
            log.error("배치 메트릭 처리 실패 - agentKey: {}, error: {}", agentKey, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("배치 메트릭 처리 중 예상치 못한 오류 발생 - agentKey: {}", agentKey, e);
            throw new BadRequestException(ExceptionMessage.CONTAINER_METRICS_PROCESSING_FAILED);
        }
    }

    @Transactional(readOnly = true)
    protected Agent findAgentReadOnly(String agentKey) {
        return agentRepository.findByAgentKey(agentKey)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    protected ContainerStatsLog findLatestStatsReadOnly(String containerHash) {
        return statsLogRepository
                .findLatestByContainerHash(containerHash, LocalDateTime.now().minusHours(1))
                .orElse(null);
    }

    @Transactional
    protected Container findOrCreateContainer(Agent agent, ContainerMetricsRequestDTO metricsDto) {
        Container container = containerRepository
                .findByAgentAndContainerHash(agent, metricsDto.getContainerHash())
                .orElseGet(() -> createNewContainer(agent, metricsDto));

        updateSpecsIfChanged(container, metricsDto);
        return container;
    }

    @Transactional
    protected void saveStatsLogBatch(List<ContainerStatsLog> statsLogList) {
        statsLogRepository.saveAll(statsLogList);
        statsLogRepository.flush();
    }
}