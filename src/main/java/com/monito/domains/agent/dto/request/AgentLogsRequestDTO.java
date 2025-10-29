package com.monito.domains.agent.dto.request;

import com.monito.domains.container.dto.request.ContainerLogItemRequestDTO;
import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * Agent로부터 받는 로그 데이터 DTO
 * WebSocket LOGS 메시지의 data 필드에 해당
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentLogsRequestDTO {

    /**
     * Agent 식별 키
     */
    private String agentKey;

    /**
     * 로그 수집 타임스탬프
     */
    private Long timestamp;

    /**
     * 컨테이너별 로그 데이터
     * Key: containerHash, Value: 로그 항목 리스트
     */
    private Map<String, List<ContainerLogItemRequestDTO>> logs;
}