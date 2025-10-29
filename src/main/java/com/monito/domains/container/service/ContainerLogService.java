package com.monito.domains.container.service;

import com.monito.domains.agent.dto.request.AgentLogsRequestDTO;

/**
 * 컨테이너 로그 수집 및 저장 서비스
 */
public interface ContainerLogService {

    /**
     * WebSocket으로 수신한 로그를 처리하여 ContainerLog에 저장
     * @param agentKey Agent 키
     * @param logsDto 로그 데이터
     */
    void processLogs(String agentKey, AgentLogsRequestDTO logsDto);
}