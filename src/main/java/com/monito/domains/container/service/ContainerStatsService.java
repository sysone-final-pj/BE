package com.monito.domains.container.service;

import com.monito.domains.container.dto.request.ContainerMetricsRequestDTO;

import java.util.List;

/**
 * 컨테이너 통계 수집 및 저장 서비스
 */
public interface ContainerStatsService {

    /**
     * WebSocket으로 수신한 메트릭을 처리하여 ContainerStatsLog에 저장
     * @param agentKey Agent 키
     * @param metricsDto 메트릭 데이터
     */
    void processMetrics(String agentKey, ContainerMetricsRequestDTO metricsDto);

    /**
     * 여러 컨테이너의 메트릭을 배치로 처리 (Batch Insert 최적화)
     * @param agentKey Agent 키
     * @param metricsList 메트릭 리스트
     */
    void processMetricsBatch(String agentKey, List<ContainerMetricsRequestDTO> metricsList);
}