package com.monito.global.config;

import com.monito.domains.agent.handler.AgentWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final AgentWebSocketHandler agentWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 개발용, 배포 환경에서는 특정 도메인만 접근할 수 있도록 수정
        registry.addHandler(agentWebSocketHandler, "/ws/agent")
                .setAllowedOrigins("*");
    }
}
