/**
 * 컨테이너 상세 정보 응답 DTO
 * - CPU, Memory, Network, OOM 메트릭을 한번에 반환
 * - 로그는 별도 API로 제공
 */
package com.monito.domains.container.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.agent.domain.AgentStatus;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.dto.response.metrics.ContainerInfoDTO;
import com.monito.domains.container.dto.response.metrics.CpuMetricsDTO;
import com.monito.domains.container.dto.response.metrics.MemoryMetricsDTO;
import com.monito.domains.container.dto.response.metrics.NetworkMetricsDTO;
import com.monito.domains.container.dto.response.metrics.OomMetricsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerDetailResponseDTO {
    private ContainerInfoDTO container;
    private CpuMetricsDTO cpu;
    private MemoryMetricsDTO memory;
    private NetworkMetricsDTO network;
    private OomMetricsDTO oom;

    // 조회 시간 정보
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer dataPoints;                           // 데이터 포인트 개수

    /**
     * 실시간 WebSocket 발행용 ContainerDetailResponseDTO 생성
     * - 각 메트릭은 단일 데이터 포인트만 포함 (dataPoints = 1)
     * - REST API 응답과 동일한 형식 유지
     */
    public static ContainerDetailResponseDTO forRealtimeUpdate(
            Container container,
            Agent agent,
            ContainerStatsLog statsLog
    ) {
        LocalDateTime timestamp = statsLog.getCollectedAt();

        // 컨테이너 기본 정보
        ContainerState effectiveState = agent.getAgentStatus() == AgentStatus.OFFLINE
                ? ContainerState.UNKNOWN : statsLog.getState();

        ContainerInfoDTO containerInfo = ContainerInfoDTO.from(container, agent, effectiveState);

        // 메트릭 DTO 생성 (실시간 업데이트용 단일 데이터 포인트)
        CpuMetricsDTO cpu = CpuMetricsDTO.forRealtimeUpdate(container, statsLog, timestamp);
        MemoryMetricsDTO memory = MemoryMetricsDTO.forRealtimeUpdate(container, statsLog, timestamp);
        NetworkMetricsDTO network = NetworkMetricsDTO.forRealtimeUpdate(statsLog, timestamp);
        OomMetricsDTO oom = OomMetricsDTO.forRealtimeUpdate(container);

        // ContainerDetailResponseDTO 생성
        return ContainerDetailResponseDTO.builder()
                .container(containerInfo)
                .cpu(cpu)
                .memory(memory)
                .network(network)
                .oom(oom)
                .startTime(timestamp)
                .endTime(timestamp)
                .dataPoints(1)  // ✅ 단일 데이터 포인트
                .build();
    }
}
