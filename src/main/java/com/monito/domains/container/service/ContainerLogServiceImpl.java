/**
 * 컨테이너 로그 수집 및 저장 서비스 구현체
 */
package com.monito.domains.container.service;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.repository.AgentRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerLog;
import com.monito.domains.agent.dto.request.AgentLogsRequestDTO;
import com.monito.domains.container.dto.request.ContainerLogItemRequestDTO;
import com.monito.domains.container.dto.response.ContainerLogEntryDTO;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
import com.monito.infrastructure.messaging.StompMessagingClient;
import com.monito.infrastructure.messaging.WsTopics;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 작성자: 백승준
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContainerLogServiceImpl implements ContainerLogService {

    private final ContainerLogRepository containerLogRepository;
    private final ContainerRepository containerRepository;
    private final AgentRepository agentRepository;
    private final StompMessagingClient messagingClient;
    private final Executor broadcastTaskExecutor;

    @Override
    @Transactional
    public void processLogs(String agentKey, AgentLogsRequestDTO logsDto) {
        try {
            // 1. 입력값 검증
            validateLogsRequest(logsDto);

            // 2. Agent 조회
            Agent agent = agentRepository.findByAgentKey(agentKey)
                    .orElseThrow(() -> new NotFoundException(ExceptionMessage.AGENT_NOT_FOUND));

            // 3. 로그 데이터가 없으면 조기 반환
            if (logsDto.getLogs() == null || logsDto.getLogs().isEmpty()) {
                log.debug("로그 데이터가 비어있음 - agentKey: {}", agentKey);
                return;
            }

            List<ContainerLog> containerLogs = new ArrayList<>();
            int totalLogCount = 0;
            int skippedContainerCount = 0;

            // 4. 컨테이너별 로그 처리
            for (Map.Entry<String, List<ContainerLogItemRequestDTO>> entry : logsDto.getLogs().entrySet()) {
                String containerHash = entry.getKey();
                List<ContainerLogItemRequestDTO> logItems = entry.getValue();

                if (logItems == null || logItems.isEmpty()) {
                    continue;
                }

                // 5. Container 조회 (존재하지 않으면 스킵 - 메트릭이 먼저 와야 함)
                Optional<Container> containerOpt = containerRepository
                        .findByAgentAndContainerHash(agent, containerHash);

                if (containerOpt.isEmpty()) {
                    log.warn("컨테이너를 찾을 수 없음 - containerHash: {}, 로그 {}개 스킵",
                            containerHash, logItems.size());
                    skippedContainerCount++;
                    continue;
                }

                Container container = containerOpt.get();

                // 6. 각 로그 항목을 ContainerLog 엔티티로 변환
                for (ContainerLogItemRequestDTO logItem : logItems) {
                    containerLogs.add(logItem.toEntity(container));
                    totalLogCount++;
                }
            }

            // 7. 배치 저장 (트랜잭션 내)
            if (!containerLogs.isEmpty()) {
                containerLogRepository.saveAll(containerLogs);
                containerLogRepository.flush();  // 즉시 DB 반영

                log.info("로그 저장 완료 - agentKey: {}, 총 로그: {}개, 스킵된 컨테이너: {}개",
                        agentKey, totalLogCount, skippedContainerCount);

                // 8. WebSocket 브로드캐스트 (비동기, 트랜잭션 외부)
                List<ContainerLog> finalLogs = new ArrayList<>(containerLogs);
                CompletableFuture.runAsync(() -> {
                    for (ContainerLog logEntity : finalLogs) {
                        try {
                            messagingClient.send(
                                    WsTopics.containerLogs(logEntity.getContainer().getId()),
                                    ContainerLogEntryDTO.from(logEntity)
                            );
                        } catch (Exception e) {
                            log.error("로그 브로드캐스트 실패 - containerId: {}",
                                    logEntity.getContainer().getId(), e);
                        }
                    }
                    log.debug("로그 비동기 브로드캐스트 완료 - {} 건", finalLogs.size());
                }, broadcastTaskExecutor);
            } else {
                log.debug("저장할 로그가 없음 - agentKey: {}", agentKey);
            }

        } catch (NotFoundException | BadRequestException e) {
            log.error("로그 처리 실패 - agentKey: {}, error: {}", agentKey, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("로그 처리 중 예상치 못한 오류 발생 - agentKey: {}", agentKey, e);
            throw new BadRequestException(ExceptionMessage.CONTAINER_LOGS_PROCESSING_FAILED);
        }
    }

    /**
     * 로그 요청 유효성 검증
     */
    private void validateLogsRequest(AgentLogsRequestDTO logsDto) {
        if (logsDto == null) {
            throw new BadRequestException(ExceptionMessage.INVALID_INPUT_VALUE);
        }
        if (logsDto.getAgentKey() == null || logsDto.getAgentKey().trim().isEmpty()) {
            throw new BadRequestException(ExceptionMessage.AGENT_KEY_REQUIRED);
        }
    }
}