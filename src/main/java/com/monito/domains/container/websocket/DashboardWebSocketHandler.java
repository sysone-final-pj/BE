package com.monito.domains.container.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대시보드 실시간 메트릭 WebSocket Handler
 * - Frontend에서 /ws/dashboard로 연결
 * - 컨테이너 메트릭 업데이트를 실시간 브로드캐스트
 */
@Slf4j
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("대시보드 WebSocket 연결: sessionId={}", sessionId);

        // 연결 확인 메시지 전송
        session.sendMessage(new TextMessage("{\"type\":\"connection\",\"status\":\"connected\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.debug("대시보드 메시지 수신: sessionId={}, payload={}", session.getId(), payload);

        // Ping/Pong 처리 (keep-alive)
        if ("ping".equals(payload)) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        log.info("대시보드 WebSocket 연결 해제: sessionId={}, status={}", sessionId, status);
    }

    /**
     * 모든 연결된 클라이언트에게 메트릭 업데이트 브로드캐스트
     * @param message JSON 형식의 메트릭 데이터
     */
    public void broadcastMetrics(String message) {
        int successCount = 0;
        int failCount = 0;

        for (WebSocketSession session : sessions.values()) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    successCount++;
                } catch (Exception e) {
                    log.error("메트릭 브로드캐스트 실패: sessionId={}", session.getId(), e);
                    failCount++;
                }
            }
        }

        if (log.isDebugEnabled()) {
            log.debug("메트릭 브로드캐스트 완료 - 성공: {}, 실패: {}, 총 세션: {}",
                    successCount, failCount, sessions.size());
        }
    }

    /**
     * 특정 세션에게만 메시지 전송
     * @param sessionId 세션 ID
     * @param message 메시지
     */
    public void sendToSession(String sessionId, String message) {
        WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.debug("메시지 전송 성공: sessionId={}", sessionId);
            } catch (Exception e) {
                log.error("메시지 전송 실패: sessionId={}", sessionId, e);
            }
        } else {
            log.warn("세션을 찾을 수 없거나 닫혀 있음: sessionId={}", sessionId);
        }
    }

    /**
     * 현재 연결된 세션 수 조회
     */
    public int getActiveSessionCount() {
        return (int) sessions.values().stream()
                .filter(WebSocketSession::isOpen)
                .count();
    }
}