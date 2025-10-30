package com.monito.domains.container.service;

import com.monito.domains.container.dto.response.ContainerListResponseDTO;
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
}