package com.monito.domains.dashboard.service;

import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.dashboard.dto.request.ContainerFilterDTO;
import com.monito.domains.dashboard.dto.request.ContainerSortType;
import com.monito.domains.dashboard.dto.request.TimeRange;
import com.monito.domains.dashboard.dto.response.*;

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
    List<ContainerDashboardResponseDTO> getAllContainers(ContainerSortType sortType, Long memberId, ContainerFilterDTO filter);

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

    /**
     * 당일 0시 기준 STDOUT/STDERR 로그 개수 조회
     * @return 당일 STDOUT/STDERR 로그 개수
     */
    DailyLogCountDTO getDailyLogCount();

    /**
     * 전체 컨테이너의 스토리지 사용량 조회
     * @return 전체 컨테이너의 스토리지 할당량과 사용량 목록
     */
    List<ContainerStorageUsageDTO> getAllContainerStorageUsage();

    /**
     * 컨테이너의 네트워크 통계 시계열 데이터 조회
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return 네트워크 통계 시계열 데이터
     */
    NetworkStatsTimeSeriesDTO getNetworkStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail);

    /**
     * 컨테이너의 Block I/O 통계 시계열 데이터 조회
     * @param containerId 컨테이너 ID
     * @param timeRange 시간 범위
     * @param detail 상세 여부 (false: 50포인트, true: 200포인트)
     * @return Block I/O 통계 시계열 데이터
     */
    BlockIOStatsTimeSeriesDTO getBlockIOStatsTimeSeries(Long containerId, TimeRange timeRange, boolean detail);

    /**
     * 컨테이너 상세 메트릭 조회 (최초 상세 패널 로드용)
     * WebSocket으로 발행되는 것과 동일한 형식의 데이터 반환
     * @param containerId 컨테이너 ID
     * @return 컨테이너 상세 메트릭 (시계열 포함)
     */
    ContainerDetailResponseDTO getContainerDetailMetrics(Long containerId);
}
