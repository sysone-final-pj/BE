package com.monito.domains.container.service;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerSortField;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsRequest;
import com.monito.domains.container.dto.request.ContainerSnapshotRequestDTO;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.dto.response.ContainerLogsResponseDTO;
import com.monito.domains.container.dto.response.ContainerSummaryResponseDTO;
import com.monito.domains.container.dto.response.OomTimeSeriesResponseDTO;
import org.springframework.data.domain.Sort.Direction;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 컨테이너 조회 서비스
 */
public interface ContainerService {

    /**
     * 컨테이너 목록 조회 (검색/필터/정렬 지원)
     * @param keyword 검색어 (agent name, container hash, container name) - null 가능
     * @param states 상태 필터 (다중 선택) - null이면 전체
     * @param healths 헬스 필터 (다중 선택) - null이면 전체
     * @param sortBy 정렬 필드 - null이면 기본 정렬
     * @param direction 정렬 방향 (ASC/DESC) - null이면 DESC
     * @return 컨테이너 목록
     */
    List<ContainerSummaryResponseDTO> getContainerList(
            String keyword,
            List<ContainerState> states,
            List<ContainerHealth> healths,
            ContainerSortField sortBy,
            Direction direction
    );

    /**
     * 컨테이너 메트릭 상세 조회 (CPU, Memory, Network)
     * @param containerId 컨테이너 ID
     * @param request 시간 범위 및 필터 조건
     * @return 컨테이너 메트릭 상세 정보
     */
    ContainerDetailResponseDTO getContainerMetrics(Long containerId, ContainerMetricsRequest request);

    /**
     * 컨테이너 로그 조회 (커서 기반 무한 스크롤 + 다중 컨테이너 지원)
     * @param containerIds 컨테이너 ID 리스트 (null이면 모든 컨테이너, 단일/다중 모두 지원)
     * @param request 커서 및 필터 조건
     * @return 로그 목록 및 다음 커서 정보
     */
    ContainerLogsResponseDTO getContainerLogs(List<Long> containerIds, ContainerLogsRequest request);

    /**
     * 컨테이너 상태 변경 처리 (Agent의 CONTAINER_STATE_CHANGE 메시지)
     * - 신규 컨테이너: 초기값(0)으로 생성, metricsInitialized = false
     * - 기존 컨테이너: 상태 업데이트
     * - deleted 상태: soft delete 처리
     * @param agentKey Agent 식별 키
     * @param snapshot 컨테이너 상태 스냅샷
     */
    void processContainerStateChange(String agentKey, ContainerSnapshotRequestDTO snapshot);

    /**
     * OOM 시계열 데이터 조회 (Histogram/Heatmap용)
     * - 캐시에서 최근 7일 이내 OOM 이벤트 조회
     * - 시간대별 버킷으로 그룹핑하여 반환
     * @param containerId 컨테이너 ID
     * @param startTime 조회 시작 시간 (null이면 7일 전)
     * @param endTime 조회 종료 시간 (null이면 현재)
     * @param bucketSize 버킷 크기 (HOURS, DAYS 등)
     * @return OOM 시계열 데이터
     */
    OomTimeSeriesResponseDTO getOomTimeSeries(
            Long containerId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ChronoUnit bucketSize
    );
}
