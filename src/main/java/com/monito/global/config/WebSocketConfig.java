package com.monito.global.config;

import com.monito.domains.agent.handler.AgentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * Raw WebSocket 설정 (Agent 전용)
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final AgentWebSocketHandler agentWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Agent → Backend: 메트릭 수집 (Raw WebSocket 유지)
        registry.addHandler(agentWebSocketHandler, "/agent")
                .setAllowedOriginPatterns("*");
    }

    /**
     * 웹소켓 전송 설정
     * - 메시지 크기 제한 설정 (64KB(기본) -> 1MB)
     * - 버퍼 크기 설정
     * - 실제 서블릿 환경에서만 동작 (테스트 환경 제외)
     */
    @Bean
    @Profile("!test")
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(1024 * 1024);  // 1MB
        container.setMaxBinaryMessageBufferSize(1024 * 1024);
        return container;
    }
}
