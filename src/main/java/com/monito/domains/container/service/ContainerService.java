package com.monito.domains.container.service;

import com.monito.domains.container.dto.response.ContainerListResponseDTO;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsRequest;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.dto.response.ContainerLogsResponseDTO;
import com.monito.domains.container.dto.response.ContainerSummaryResponseDTO;


import java.util.List;

/**
 * 컨테이너 조회 서비스
 */
public interface ContainerService {

    /**
     * 모든 컨테이너 목록 조회 (최신 통계 포함)
     * @return 전체 컨테이너 목록
     */
    List<ContainerListResponseDTO> getAllContainers();

    /**
     * 특정 Agent의 컨테이너 목록 조회 (최신 통계 포함)
     * @param agentId Agent ID
     * @return Agent에 속한 컨테이너 목록
     */
    List<ContainerListResponseDTO> getContainersByAgentId(Long agentId);

    /**
     * 컨테이너 목록 조회
     */
    List<ContainerSummaryResponseDTO> getContainerList();

    /**
     * 컨테이너 메트릭 상세 조회 (CPU, Memory, Network)
     * @param containerId 컨테이너 ID
     * @param request 시간 범위 및 필터 조건
     * @return 컨테이너 메트릭 상세 정보
     */
    ContainerDetailResponseDTO getContainerMetrics(Long containerId, ContainerMetricsRequest request);

    /**
     * 컨테이너 로그 조회 (커서 기반 무한 스크롤)
     * @param containerId 컨테이너 ID
     * @param request 커서 및 필터 조건
     * @return 로그 목록 및 다음 커서 정보
     */
    ContainerLogsResponseDTO getContainerLogs(Long containerId, ContainerLogsRequest request);
}
