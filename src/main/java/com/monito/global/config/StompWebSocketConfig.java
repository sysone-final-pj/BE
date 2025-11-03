package com.monito.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP WebSocket 설정 (Dashboard, Alert 전용)
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class StompWebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    /**
     * STOMP 메시지 브로커 설정
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 메시지 브로커 활성화
        registry.enableSimpleBroker("/topic", "/queue");

        // 클라이언트가 메시지를 보낼 때 사용할 prefix
        registry.setApplicationDestinationPrefixes("/app");

        // 사용자별 메시지 prefix (알림용)
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * STOMP 엔드포인트 등록
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // STOMP 엔드포인트: /ws
        // SockJS 폴백 지원
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /**
     * 인바운드 채널 설정 (클라이언트 → 서버)
     * - WebSocket 인증 Interceptor 등록
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}