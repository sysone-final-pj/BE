/**
 * 대시보드 메트릭 발행 Facade
 * - 컨테이너 메트릭 수집 시 WebSocket으로 브로드캐스트
 * - 리스트 구독 및 상세 구독 분리
 */
package com.monito.domains.dashboard.facade;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.dashboard.dto.response.ContainerCardResponseDTO;
import com.monito.domains.dashboard.dto.response.DashboardContainerDetailDTO;
import com.monito.infrastructure.messaging.StompMessagingClient;
import com.monito.infrastructure.messaging.WsTopics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 작성자: 이지민
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DashboardPublishingFacade {

    private final SimpMessagingTemplate messagingTemplate;
    private final StompMessagingClient messagingClient;

    /**
     * 컨테이너 메트릭 업데이트 발행
     * - 리스트 브로드캐스트: /topic/dashboard/list (경량 카드 DTO)
     * - 상세 메트릭 발행: /topic/containers/{id}/metrics (상세 DTO)
     *
     * @param container 컨테이너 엔티티
     * @param agent Agent 엔티티
     * @param statsLog 통계 로그
     */
    public void publishMetricsUpdate(Container container, Agent agent, ContainerStatsLog statsLog) {
        // 1. 리스트 브로드캐스트 (경량 카드 리스트용)
        publishListUpdate(container, statsLog);

        // 2. 상세 메트릭 발행 (컨테이너별 상세용)
        publishDetailUpdate(container, agent, statsLog);
    }

    /**
     * 리스트 브로드캐스트 (/topic/dashboard/list)
     * - 모든 구독자에게 경량 카드 DTO 전송
     */
    private void publishListUpdate(Container container, ContainerStatsLog statsLog) {
        try {
            ContainerCardResponseDTO cardDto = ContainerCardResponseDTO.of(container, statsLog);

            // STOMP를 통한 브로드캐스트 (/topic/dashboard/list 구독자 전체에게 전송)
            messagingTemplate.convertAndSend("/topic/dashboard/list", cardDto);

            log.debug("컨테이너 카드 리스트 브로드캐스트 전송 완료 - Container: {}", container.getName());
        } catch (Exception e) {
            log.error("컨테이너 카드 리스트 브로드캐스트 실패 - containerHash: {}, error: {}",
                    container.getContainerHash(), e.getMessage(), e);
        }
    }

    /**
     * 상세 메트릭 발행 (/topic/containers/{id}/metrics)
     * - 특정 컨테이너 구독자에게 상세 DTO 전송
     */
    private void publishDetailUpdate(Container container, Agent agent, ContainerStatsLog statsLog) {
        try {
            DashboardContainerDetailDTO detailMetrics = DashboardContainerDetailDTO.forRealtimeUpdate(
                    container, agent, statsLog
            );

            // 컨테이너별 상세 메트릭 발행
            messagingClient.send(WsTopics.containerMetrics(container.getId()), detailMetrics);

            log.info("컨테이너 상세 메트릭 발행 완료 - containerId: {}, containerName: {}",
                    container.getId(), container.getName());
        } catch (Exception e) {
            log.error("컨테이너 상세 메트릭 발행 실패 - containerId: {}, error: {}",
                    container.getId(), e.getMessage(), e);
        }
    }
}