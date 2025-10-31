package com.monito.global.config;

import com.monito.domains.alert.handler.AlertWebSocketHandler;
import com.monito.domains.agent.handler.AgentWebSocketHandler;
import com.monito.domains.container.handler.DashboardWebSocketHandler;
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
    private final AlertWebSocketHandler alertWebSocketHandler;
    private final DashboardWebSocketHandler dashboardWebSocketHandler;


    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Agent → Backend: 메트릭 수집
        registry.addHandler(agentWebSocketHandler, "/ws/agent")
                .setAllowedOrigins("*");

        // Backend → Frontend: 알림 전송
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*"); // TODO : CORS 설정 (프론트엔드 주소로 변경)

        // TODO : 대시보드 WebSocket 지표 항목 더 늘려야함
        registry.addHandler(dashboardWebSocketHandler, "/ws/dashboard")
                .setAllowedOrigins("*");
    }
}
