/**
 * Agent로부터 WebSocket으로 수신되는 전체 메트릭 데이터
 * {
 *   "agentKey": "...",
 *   "timestamp": 1234567890,
 *   "metrics": [...]
 * }
 */
package com.monito.domains.agent.dto.request;

import java.util.List;

import com.monito.domains.container.dto.request.ContainerMetricsRawRequestDTO;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentMetricsRequestDTO {
    private String agentKey;
    private Long timestamp;
    private List<ContainerMetricsRawRequestDTO> metrics;
}