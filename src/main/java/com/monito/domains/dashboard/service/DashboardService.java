package com.monito.domains.dashboard.service;

import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.response.AgentContainerCountDTO;
import com.monito.domains.dashboard.dto.response.AgentContainerGroupDTO;
import com.monito.domains.dashboard.dto.response.ContainerDashboardResponseDTO;
import com.monito.domains.dashboard.dto.response.ContainerWithFavoriteDTO;
import java.util.List;

/**
 * 대시보드 서비스
 * - 대시보드 화면에 필요한 데이터 제공
 */
public interface DashboardService {

    /**
     * 전체 컨테이너 목록 조회 (최신 통계 포함)
     * @param sortType 정렬 타입 (null이면 정렬하지 않음)
     * @param memberId 회원 ID (FAVORITE 정렬 시 필수)
     * @param filter 필터 조건
     * @return 컨테이너 목록
     */
    List<ContainerDashboardResponseDTO> getAllContainers(ContainerSortType sortType, Long memberId, com.monito.domains.dashboard.dto.request.ContainerFilterDTO filter);

    /**
     * 특정 Agent의 컨테이너 목록 조회 (최신 통계 포함)
     * @param agentId Agent ID
     * @return Agent에 속한 컨테이너 목록
     */
    List<ContainerDashboardResponseDTO> getContainersByAgentId(Long agentId);

    /**
     * Agent별 컨테이너 개수 집계
     * @return Agent별 컨테이너 개수 목록 (개수 내림차순)
     */
    List<AgentContainerCountDTO> getContainerCountByAgent();

    /**
     * Agent별 컨테이너 그룹핑 (컨테이너 리스트 포함)
     * @return Agent별 컨테이너 그룹 목록 (개수 내림차순)
     */
    List<AgentContainerGroupDTO> getContainersGroupedByAgent();

    /**
     * 구동중인 컨테이너 목록 조회 (state = RUNNING)
     * @return RUNNING 상태의 컨테이너 목록
     */
    List<ContainerDashboardResponseDTO> getRunningContainers();

    /**
     * 특정 컨테이너 상세 정보 조회
     * @param containerId 컨테이너 ID
     * @return 컨테이너 상세 정보
     */
    ContainerDashboardResponseDTO getContainerDetail(Long containerId);

    /**
     * 모든 컨테이너 목록 조회 (즐겨찾기 우선 정렬)
     * @param memberId Member ID
     * @return 즐겨찾기가 먼저 오는 모든 컨테이너 목록
     */
    List<ContainerWithFavoriteDTO> getAllContainersSortedByFavorite(Long memberId);
}
