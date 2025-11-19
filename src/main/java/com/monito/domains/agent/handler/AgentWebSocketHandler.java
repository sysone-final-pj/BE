package com.monito.domains.agent.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.agent.dto.request.AgentInfoRequestDTO;
import com.monito.domains.agent.service.AgentService;
import com.monito.domains.agent.dto.request.AgentLogsRequestDTO;
import com.monito.domains.agent.dto.request.AgentMetricsRequestDTO;
import com.monito.domains.container.dto.request.ContainerMetricsRawRequestDTO;
import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;
import com.monito.domains.container.dto.request.ContainerSnapshotRequestDTO;
import com.monito.domains.container.dto.request.ContainerStateChangeRequestDTO;
import com.monito.domains.container.service.ContainerLogService;
import com.monito.domains.container.service.ContainerService;
import com.monito.domains.container.service.ContainerStatsService;
import com.monito.global.cache.AgentMetadata;
import com.monito.global.cache.AgentMetadataCache;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgentWebSocketHandler extends TextWebSocketHandler {
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;
    private final AgentService agentService;
    private final ContainerStatsService containerStatsService;
    private final ContainerLogService containerLogService;
    private final ContainerService containerService;
    private final AgentMetadataCache metadataCache;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> authenticatedAgents = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);

        log.info("═══════════════════════════════════════");
        log.info("새 연결 수립");
        log.info("   Session ID: {}", sessionId);
        log.info("   현재 연결 수: {}", sessions.size());
        log.info("   시각: {}", getCurrentTime());
        log.info("═══════════════════════════════════════");

        sendMessage(session, Map.of(
                "type", "SYSTEM",
                "message", "Connected to Backend. Please authenticate",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        String sessionId = session.getId();

        log.info("메시지 수신 [{}]: {}", sessionId, payload);

        try {
            Map<String, Object> data = objectMapper.readValue(payload, Map.class);
            String type = String.valueOf(data.get("type"));

            switch (type) {
                case "AUTH":
                    handleAuth(session, data);
                    break;
                case "AGENT_INFO":
                    handleAgentInfo(session, data);
                    break;
                case "CONTAINER_SYNC":
                    handleContainerSync(session, data);
                    break;
                case "CONTAINER_STATE_CHANGE":
                    handleContainerStateChange(session, data);
                    break;
                case "METRICS":
                    handleMetrics(session, data);
                    break;
                case "LOGS":
                    handleLogs(session, data);
                    break;
                case "PING":
                    handlePing(session);
                    break;
                default:
                    log.warn("알 수 없는 메시지 타입: {}", type);
                    sendMessage(session, Map.of(
                            "type", "ERROR",
                            "message", "Unknown message type: " + type
                    ));
            }
        } catch (Exception e) {
            log.error("메시지 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "error",
                    "message", "Invalid message format: " + e.getMessage()
            ));
        }
    }

    private void handleAuth(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = String.valueOf(data.get("agentKey"));

        log.info("═══════════════════════════════════════");
        log.info("🔐 인증 시도");
        log.info("   Session ID: {}", sessionId);
        log.info("   Agent Key: {}", agentKey);
        log.info("═══════════════════════════════════════");

        // 중복 연결 체크
        if (authenticatedAgents.containsValue(agentKey)) {
            sendMessage(session, Map.of(
                    "type", "AUTH_FAILED",
                    "message", "Agent already connected from another session",
                    "timestamp", System.currentTimeMillis()
            ));

            log.warn("인증 실패: 이미 연결된 Agent - agentKey: {}", agentKey);
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // Agent 인증
        try {
            Agent agent = agentService.authenticateAgent(agentKey);

            // 인증 성공 처리
            authenticatedAgents.put(sessionId, agentKey);
            agentService.updateAgentStatus(agentKey, AgentStatus.ONLINE);

            sendMessage(session, Map.of(
                    "type", "AUTH_SUCCESS",
                    "message", "Authentication Successful",
                    "agentKey", agentKey,
                    "agentName", agent.getAgentName(),
                    "timestamp", System.currentTimeMillis()
            ));

            log.info("인증 성공");
            log.info("      Agent Key: {}", agentKey);
            log.info("      Agent Name: {}", agent.getAgentName());
            log.info("      현재 인증된 Agent 수: {}", authenticatedAgents.size());

        } catch (Exception e) {
            sendMessage(session, Map.of(
                    "type", "AUTH_FAILED",
                    "message", e.getMessage(),
                    "timestamp", System.currentTimeMillis()
            ));

            log.error("인증 실패");
            log.error("      Agent Key: {}", agentKey);
            log.error("      Reason: {}", e.getMessage());

            session.close(CloseStatus.NOT_ACCEPTABLE);
        }
    }

    // todo: 이러한 구조(응답 상태 확인)에서 네트워크 연결 비용 고려해보기
    private void handleMetrics(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        if (agentKey == null) {
            log.warn("인증되지 않은 세션에서 메트릭 전송 시도: {}", sessionId);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Not authenticated. Please authenticate first."
            ));
            return;
        }

        try {
            // JSON 데이터를 DTO로 변환
            Map<String, Object> metricsData = (Map<String, Object>) data.get("data");
            AgentMetricsRequestDTO agentMetrics = objectMapper.convertValue(
                    metricsData,
                    AgentMetricsRequestDTO.class
            );

            log.info("═══════════════════════════════════════");
            log.info("메트릭 수신");
            log.info("   Agent Key: {}", agentKey);
            log.info("   컨테이너 개수: {}", agentMetrics.getMetrics() != null ? agentMetrics.getMetrics().size() : 0);
            log.info("   시각: {}", getCurrentTime());
            log.info("═══════════════════════════════════════");

            // 각 컨테이너 메트릭 처리
            int successCount = 0;
            int failCount = 0;

            if (agentMetrics.getMetrics() != null) {
                for (ContainerMetricsRawRequestDTO rawMetric : agentMetrics.getMetrics()) {
                    try {
                        // Flat DTO로 변환
                        ContainerMetricsRequestDTO flatMetric = rawMetric.toFlatDTO();

                        // DB 저장 (계산 포함)
                        containerStatsService.processMetrics(agentKey, flatMetric);
                        successCount++;

                        log.debug("컨테이너 메트릭 저장 성공 - Hash: {}, Name: {}",
                                flatMetric.getContainerHash(),
                                flatMetric.getContainerName());
                    } catch (Exception e) {
                        failCount++;
                        log.error("컨테이너 메트릭 처리 실패 - Hash: {}",
                                rawMetric.getContainerHash(), e);
                    }
                }
            }

            log.info("메트릭 처리 완료 - 성공: {}, 실패: {}", successCount, failCount);

            sendMessage(session, Map.of(
                    "type", "ACK",
                    "message", String.format("Metrics processed: %d success, %d failed", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("메트릭 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to process metrics: " + e.getMessage()
            ));
        }
    }

    /**
     * 컨테이너 로그 처리 (LOGS)
     * - Agent로부터 수집된 컨테이너 로그를 처리하여 DB에 저장
     */
    private void handleLogs(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        if (agentKey == null) {
            log.warn("인증되지 않은 세션에서 로그 전송 시도: {}", sessionId);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Not authenticated. Please authenticate first."
            ));
            return;
        }

        try {
            // JSON 데이터를 DTO로 변환
            Map<String, Object> logsData = (Map<String, Object>) data.get("data");
            AgentLogsRequestDTO agentLogs = objectMapper.convertValue(
                    logsData,
                    AgentLogsRequestDTO.class
            );

            int totalContainers = agentLogs.getLogs() != null ? agentLogs.getLogs().size() : 0;
            int totalLogs = 0;
            if (agentLogs.getLogs() != null) {
                totalLogs = agentLogs.getLogs().values().stream()
                        .mapToInt(list -> list != null ? list.size() : 0)
                        .sum();
            }

            log.info("═══════════════════════════════════════");
            log.info("📝 로그 수신");
            log.info("   Agent Key: {}", agentKey);
            log.info("   컨테이너 개수: {}", totalContainers);
            log.info("   총 로그 개수: {}", totalLogs);
            log.info("   시각: {}", getCurrentTime());
            log.info("═══════════════════════════════════════");

            // 로그 처리 (DB 저장)
            containerLogService.processLogs(agentKey, agentLogs);

            log.info("로그 처리 완료 - 총 로그: {}개", totalLogs);

            sendMessage(session, Map.of(
                    "type", "LOGS_ACK",
                    "message", String.format("Logs processed: %d logs from %d containers", totalLogs, totalContainers),
                    "totalContainers", totalContainers,
                    "totalLogs", totalLogs,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("로그 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to process logs: " + e.getMessage()
            ));
        }
    }

    /**
     * 컨테이너 전체 동기화 (CONTAINER_SYNC)
     * - Agent 연결 직후 1회만 실행
     * - Agent가 보유한 모든 컨테이너 목록을 받아 DB와 동기화
     * - DB에는 있지만 Agent에 없는 컨테이너 = 삭제된 것으로 간주하여 DELETED 처리
     */
    private void handleContainerSync(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        if (agentKey == null) {
            log.warn("인증되지 않은 세션에서 컨테이너 동기화 시도: {}", sessionId);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Not authenticated. Please authenticate first."
            ));
            return;
        }

        try {
            // JSON 데이터를 DTO로 변환
            Map<String, Object> syncData = (Map<String, Object>) data.get("data");
            ContainerStateChangeRequestDTO sync = objectMapper.convertValue(
                    syncData,
                    ContainerStateChangeRequestDTO.class
            );

            int totalContainers = sync.getContainers() != null ? sync.getContainers().size() : 0;

            log.info("═══════════════════════════════════════");
            log.info("🔄 컨테이너 전체 동기화 시작");
            log.info("   Agent Key: {}", agentKey);
            log.info("   Agent 컨테이너 개수: {}", totalContainers);
            log.info("   시각: {}", getCurrentTime());
            log.info("═══════════════════════════════════════");

            // 1. Agent가 보낸 컨테이너들을 생성/업데이트
            int successCount = 0;
            int failCount = 0;
            Set<String> agentContainerHashes = new HashSet<>();

            if (sync.getContainers() != null) {
                for (ContainerSnapshotRequestDTO snapshot : sync.getContainers()) {
                    try {
                        containerService.processContainerStateChange(agentKey, snapshot);
                        agentContainerHashes.add(snapshot.getContainerHash());
                        successCount++;

                        log.debug("컨테이너 동기화 성공 - Hash: {}, Name: {}, State: {}",
                                snapshot.getContainerHash(),
                                snapshot.getContainerName(),
                                snapshot.getState());
                    } catch (Exception e) {
                        failCount++;
                        log.error("컨테이너 동기화 실패 - Hash: {}",
                                snapshot.getContainerHash(), e);
                    }
                }
            }

            // 2. DB에는 있지만 Agent가 보내지 않은 컨테이너 삭제 처리
            // - Agent가 빈 배열(Set)을 보낸 경우: Agent에 컨테이너가 실제로 0개 → 모든 DB 컨테이너 삭제
            // - Agent가 일부 컨테이너를 보낸 경우: 나머지 DB 컨테이너만 삭제
            try {
                containerService.syncAgentContainers(agentKey, agentContainerHashes);
            } catch (Exception e) {
                log.error("컨테이너 삭제 동기화 실패 - agentKey: {}", agentKey, e);
            }

            if (totalContainers == 0) {
                log.info("✅ 컨테이너 전체 동기화 완료 - Agent가 컨테이너 0개 보고 (모든 DB 컨테이너 삭제 처리됨)");
            } else {
                log.info("✅ 컨테이너 전체 동기화 완료 - 성공: {}, 실패: {}", successCount, failCount);
            }

            sendMessage(session, Map.of(
                    "type", "CONTAINER_SYNC_ACK",
                    "message", String.format("Container sync completed: %d success, %d failed", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("컨테이너 동기화 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to process container sync: " + e.getMessage()
            ));
        }
    }

    /**
     * 컨테이너 상태 변경 처리 (CONTAINER_STATE_CHANGE)
     * - Agent가 감지한 컨테이너 생성/종료/삭제 이벤트 처리
     * - 신규 컨테이너: 초기값(0)으로 생성
     * - 삭제된 컨테이너: soft delete 처리
     */
    private void handleContainerStateChange(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        if (agentKey == null) {
            log.warn("인증되지 않은 세션에서 컨테이너 상태 변경 전송 시도: {}", sessionId);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Not authenticated. Please authenticate first."
            ));
            return;
        }

        try {
            // JSON 데이터를 DTO로 변환
            Map<String, Object> stateChangeData = (Map<String, Object>) data.get("data");
            ContainerStateChangeRequestDTO stateChange = objectMapper.convertValue(
                    stateChangeData,
                    ContainerStateChangeRequestDTO.class
            );

            int totalContainers = stateChange.getContainers() != null ? stateChange.getContainers().size() : 0;

            log.info("═══════════════════════════════════════");
            log.info("📦 컨테이너 상태 변경 수신");
            log.info("   Agent Key: {}", agentKey);
            log.info("   컨테이너 개수: {}", totalContainers);
            log.info("   시각: {}", getCurrentTime());
            log.info("═══════════════════════════════════════");

            // 각 컨테이너 상태 처리
            int successCount = 0;
            int failCount = 0;

            if (stateChange.getContainers() != null) {
                for (ContainerSnapshotRequestDTO snapshot : stateChange.getContainers()) {
                    try {
                        containerService.processContainerStateChange(agentKey, snapshot);
                        successCount++;

                        log.debug("컨테이너 상태 변경 처리 성공 - Hash: {}, Name: {}, State: {}",
                                snapshot.getContainerHash(),
                                snapshot.getContainerName(),
                                snapshot.getState());
                    } catch (Exception e) {
                        failCount++;
                        log.error("컨테이너 상태 변경 처리 실패 - Hash: {}",
                                snapshot.getContainerHash(), e);
                    }
                }
            }

            log.info("컨테이너 상태 변경 처리 완료 - 성공: {}, 실패: {}", successCount, failCount);

            sendMessage(session, Map.of(
                    "type", "CONTAINER_STATE_CHANGE_ACK",
                    "message", String.format("Container state changes processed: %d success, %d failed", successCount, failCount),
                    "successCount", successCount,
                    "failCount", failCount,
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("컨테이너 상태 변경 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to process container state changes: " + e.getMessage()
            ));
        }
    }

    /**
     * Agent 메타데이터 처리 (AGENT_INFO)
     * - Agent의 시스템 정보 (hostTotalMemory, cpuCores 등)를 캐시에 저장
     * - 인증 후 1회 전송 또는 시스템 변경 시 전송
     */
    private void handleAgentInfo(WebSocketSession session, Map<String, Object> data) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        if (agentKey == null) {
            log.warn("인증되지 않은 세션에서 Agent 정보 전송 시도: {}", sessionId);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Not authenticated. Please authenticate first."
            ));
            return;
        }

        try {
            // JSON 데이터를 DTO로 변환
            Map<String, Object> agentInfoData = (Map<String, Object>) data.get("data");
            AgentInfoRequestDTO agentInfo = objectMapper.convertValue(
                    agentInfoData,
                    AgentInfoRequestDTO.class
            );

            // 캐시에 저장
            AgentMetadata metadata = AgentMetadata.builder()
                    .agentKey(agentKey)
                    .hostTotalMemory(agentInfo.getHost().getTotalMemory())
                    .hostCpuCores(agentInfo.getHost().getCpuCores())
                    .hostTotalDiskSpace(agentInfo.getHost().getTotalDisk())
                    .hostname(agentInfo.getHost().getHostname())
                    .osType(agentInfo.getHost().getOsType())
                    .lastUpdatedAt(System.currentTimeMillis())
                    .build();

            metadataCache.updateMetadata(agentKey, metadata);

            log.info("═══════════════════════════════════════");
            log.info("📊 Agent 메타데이터 수신");
            log.info("   Agent Key: {}", agentKey);
            log.info("   Host Total Memory: {} bytes ({} GB)",
                    metadata.getHostTotalMemory(),
                    metadata.getHostTotalMemory() / (1024.0 * 1024.0 * 1024.0));
            log.info("   Host CPU Cores: {}", metadata.getHostCpuCores());
            log.info("   Host Total Disk: {} bytes ({} GB)",
                    metadata.getHostTotalDiskSpace(),
                    metadata.getHostTotalDiskSpace() / (1024.0 * 1024.0 * 1024.0));
            log.info("   시각: {}", getCurrentTime());
            log.info("═══════════════════════════════════════");

            sendMessage(session, Map.of(
                    "type", "AGENT_INFO_ACK",
                    "message", "Agent metadata received and cached",
                    "timestamp", System.currentTimeMillis()
            ));

        } catch (Exception e) {
            log.error("Agent 메타데이터 처리 실패", e);
            sendMessage(session, Map.of(
                    "type", "ERROR",
                    "message", "Failed to process agent info: " + e.getMessage()
            ));
        }
    }

    private void handlePing(WebSocketSession session) throws Exception {
        String agentKey = authenticatedAgents.get(session.getId());

        log.debug("PING from Agent: {}", agentKey);

        sendMessage(session, Map.of(
                "type", "PONG",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        // Map에서 제거 (세션은 이미 닫힌 상태)
        sessions.remove(sessionId);
        authenticatedAgents.remove(sessionId);

        // Agent 상태를 OFFLINE으로 변경 및 캐시 제거
        if (agentKey != null) {
            agentService.updateAgentStatus(agentKey, AgentStatus.OFFLINE);
            metadataCache.removeMetadata(agentKey);
        }

        log.info("═══════════════════════════════════════");
        log.info("🔌 연결 종료");
        log.info("   Agent Key: {}", agentKey);
        log.info("   Session ID: {}", sessionId);
        log.info("   Status: {}", status);
        log.info("   남은 연결 수: {}", sessions.size());
        log.info("   시각: {}", getCurrentTime());
        log.info("═══════════════════════════════════════");
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        String agentKey = authenticatedAgents.get(sessionId);

        // Agent 상태를 ERROR로 변경
        if (agentKey != null) {
            agentService.updateAgentStatus(agentKey, AgentStatus.ERROR);
        }

        log.error("═══════════════════════════════════════");
        log.error("WebSocket 전송 에러");
        log.error("   Agent Key: {}", agentKey);
        log.error("   Session ID: {}", sessionId);
        log.error("   Error: ", exception);
        log.error("═══════════════════════════════════════");
    }

    private void sendMessage(WebSocketSession session, Map<String, Object> data) throws Exception {
        if (session.isOpen()) {
            String json = objectMapper.writeValueAsString(data);
            session.sendMessage(new TextMessage(json));
            log.debug("메시지 전송: {}", json);
        }
    }

    private String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }
}
