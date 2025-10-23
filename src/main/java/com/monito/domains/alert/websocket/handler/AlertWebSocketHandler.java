package com.monito.domains.alert.websocket.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AlertWebSocketHandler extends TextWebSocketHandler {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.put(userId, session);
        log.info("WebSocket 연결: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("메시지 수신: {}", payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String userId = getUserIdFromSession(session);
        sessions.remove(userId);
        log.info("WebSocket 연결 해제: userId={}, sessionId={}", userId, session.getId());
    }

    public void sendAlertToUser(String userId, String message) {
        WebSocketSession session = sessions.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
                log.info("알림 전송 성공: userId={}, message={}", userId, message);
            } catch (Exception e) {
                log.error("알림 전송 실패: userId={}", userId, e);
            }
        }
    }

    public void broadcastAlert(String message) {
        sessions.values().forEach(session -> {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(message));
                    log.info("브로드캐스트 전송 성공: sessionId={}", session.getId());
                } catch (Exception e) {
                    log.error("브로드캐스트 전송 실패: sessionId={}", session.getId(), e);
                }
            }
        });
    }

    private String getUserIdFromSession(WebSocketSession session) {
        String query = session.getUri().getQuery();
        if (query != null && query.startsWith("userId=")) {
            return query.substring(7);
        }
        return session.getId();
    }
}