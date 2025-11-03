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

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            // CONNECT 메시지에서 JWT 토큰 추출
            List<String> authorizationHeaders = accessor.getNativeHeader("Authorization");

            if (authorizationHeaders != null && !authorizationHeaders.isEmpty()) {
                String authHeader = authorizationHeaders.get(0);

                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);

                    try {
                        // 토큰 유효성 검증
                        if (jwtTokenProvider.validateToken(token) && jwtTokenProvider.isAccessToken(token)) {
                            // 사용자 ID 추출
                            Long userId = jwtTokenProvider.getUserIdFromToken(token);
                            String role = jwtTokenProvider.getRoleFromToken(token);

                            // Principal 생성 (userId를 name으로 사용)
                            Principal principal = new UsernamePasswordAuthenticationToken(
                                    String.valueOf(userId),
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority(role))
                            );

                            // Principal 설정
                            accessor.setUser(principal);

                            log.info("WebSocket 인증 성공: userId={}, role={}", userId, role);
                        } else {
                            log.warn("WebSocket 인증 실패: 유효하지 않은 토큰");
                        }
                    } catch (Exception e) {
                        log.error("WebSocket 인증 중 오류 발생", e);
                    }
                } else {
                    log.warn("WebSocket 인증 실패: Authorization 헤더 형식 오류");
                }
            } else {
                log.warn("WebSocket CONNECT: Authorization 헤더 없음");
            }
        }

        return message;
    }
}