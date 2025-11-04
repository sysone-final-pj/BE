package com.monito.global.config;

import com.monito.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.Collections;
import java.util.List;

/**
 * WebSocket 인증 Interceptor
 * - STOMP CONNECT 시 JWT 토큰을 검증하고 사용자 Principal 설정
 * - convertAndSendToUser()가 동작하기 위해 필수
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        // CONNECT 명령이 아니면 스킵
        if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
            return message;
        }

        authenticateWebSocketConnection(accessor);
        return message;
    }

    /**
     * WebSocket 연결 인증 처리
     */
    private void authenticateWebSocketConnection(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null) {
            return;
        }

        try {
            if (!isValidToken(token)) {
                log.warn("WebSocket 인증 실패: 유효하지 않은 토큰");
                return;
            }

            setPrincipal(accessor, token);
        } catch (Exception e) {
            log.error("WebSocket 인증 중 오류 발생", e);
        }
    }

    /**
     * Authorization 헤더에서 JWT 토큰 추출
     */
    private String extractToken(StompHeaderAccessor accessor) {
        List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");

        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            log.warn("WebSocket CONNECT: Authorization 헤더 없음");
            return null;
        }

        String authHeader = authorizationHeaders.get(0);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("WebSocket 인증 실패: Authorization 헤더 형식 오류");
            return null;
        }

        return authHeader.substring(7);
    }

    /**
     * 토큰 유효성 검증
     */
    private boolean isValidToken(String token) {
        return jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token);
    }

    /**
     * 인증된 사용자의 Principal 설정
     */
    private void setPrincipal(StompHeaderAccessor accessor, String token) {
        Long userId = jwtTokenProvider.getUserIdFromToken(token);
        String role = jwtTokenProvider.getRoleFromToken(token);

        Principal principal = new UsernamePasswordAuthenticationToken(
                String.valueOf(userId),
                null,
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );

        accessor.setUser(principal);
        log.info("WebSocket 인증 성공: userId={}, role={}", userId, role);
    }
}