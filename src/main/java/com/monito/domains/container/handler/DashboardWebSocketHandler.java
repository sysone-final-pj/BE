package com.monito.domains.container.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 대시보드 실시간 메트릭 브로드캐스트 WebSocket Handler
 * - 모든 연결된 클라이언트에게 컨테이너 메트릭 실시간 전송
 * - 인증 없이 모든 사용자가 동일한 메트릭 수신
 */
@Slf4j
@Component
public class DashboardWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.put(session.getId(), session);
        log.info("대시보드 WebSocket 연결 성공 - sessionId: {}, 총 세션 수: {}",
                session.getId(), sessions.size());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session.getId());
        log.info("대시보드 WebSocket 연결 종료 - sessionId: {}, 총 세션 수: {}",
                session.getId(), sessions.size());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("대시보드 WebSocket 전송 오류 - sessionId: {}", session.getId(), exception);
        sessions.remove(session.getId());
    }

    /**
     * 모든 연결된 클라이언트에게 메트릭 브로드캐스트
     * @param jsonMessage ContainerListResponseDTO JSON
     */
    public void broadcastMetrics(String jsonMessage) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(jsonMessage));
                } catch (IOException e) {
                    log.error("메트릭 전송 실패 - sessionId: {}", session.getId(), e);
                }
            }
        });
    }

    /**
     * 활성 세션 수 반환
     */
    public int getActiveSessionCount() {
        return sessions.size();
    }
}