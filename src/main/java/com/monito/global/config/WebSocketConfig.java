package com.monito.global.config;

import com.monito.domains.alert.handler.AlertWebSocketHandler;
import com.monito.domains.agent.handler.AgentWebSocketHandler;
import com.monito.domains.dashboard.handler.DashboardWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

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
        registry.addHandler(agentWebSocketHandler, "/ws/agent/collect")
                .setAllowedOrigins("*");

        // Backend → Frontend: 알림 전송
        registry.addHandler(alertWebSocketHandler, "/ws/alerts")
                .setAllowedOrigins("*"); // TODO : CORS 설정 (프론트엔드 주소로 변경)

        // TODO : 대시보드 WebSocket 지표 항목 더 늘려야함
        registry.addHandler(dashboardWebSocketHandler, "/ws/dashboard")
                .setAllowedOrigins("*");
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
