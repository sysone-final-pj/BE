/**
 * 컨테이너 API Controller
 * - 컨테이너 목록 조회, 메트릭 조회, 로그 조회
 */
package com.monito.domains.container.controller;

import com.monito.domains.container.domain.*;
import com.monito.domains.container.dto.request.ContainerLogsRequest;
import com.monito.domains.container.dto.request.ContainerMetricsPageRequestDTO;
import com.monito.domains.container.dto.request.QuickRangeType;
import com.monito.domains.container.dto.response.ContainerDetailResponseDTO;
import com.monito.domains.container.dto.response.ContainerLogsResponseDTO;
import com.monito.domains.container.dto.response.ContainerSummaryResponseDTO;
import com.monito.domains.container.dto.response.DeletedContainerResponseDTO;
import com.monito.domains.container.dto.response.timeseries.TimeSeriesResponse;
import com.monito.domains.container.service.ContainerService;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
/**
 작성자: 백승준
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ContainerController {

    private final ContainerService containerService;

    /**
     * 컨테이너 목록 조회 (검색/필터/정렬 지원)
     * GET /api/containers
     *
     * Query Parameters:
     * - keyword: 검색어 (agent name, container hash, container name)
     * - states: 상태 필터 (다중 선택 가능) - RUNNING, PAUSED, DEAD, etc.
     * - healths: 헬스 필터 (다중 선택 가능) - HEALTHY, UNHEALTHY, NONE, etc.
     * - sortBy: 정렬 필드 - AGENT_NAME, CONTAINER_NAME, CPU_PERCENT, MEM_USAGE, etc.
     * - direction: 정렬 방향 - ASC, DESC (기본: DESC)
     *
     * 예시:
     * - 검색: GET /api/containers?keyword=nginx
     * - 필터: GET /api/containers?states=RUNNING&healths=HEALTHY
     * - 정렬: GET /api/containers?sortBy=CPU_PERCENT&direction=DESC
     * - 복합: GET /api/containers?keyword=nginx&states=RUNNING&sortBy=MEM_USAGE&direction=DESC
     */
    @GetMapping("/containers")
    public ApiResponse<List<ContainerSummaryResponseDTO>> getContainerList(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) List<ContainerState> states,
            @RequestParam(required = false) List<ContainerHealth> healths,
            @RequestParam(required = false) ContainerSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        log.info("컨테이너 목록 조회 요청 - memberId: {}, keyword: {}, states: {}, healths: {}, sortBy: {}, direction: {}",
                userDetails.getId(), keyword, states, healths, sortBy, direction);

        List<ContainerSummaryResponseDTO> containers = containerService.getContainerList(
                userDetails.getId(), keyword, states, healths, sortBy, direction
        );
        return ApiResponse.ok(containers, "컨테이너 목록 조회 성공");
    }

    /**
     * 컨테이너 메트릭 상세 조회 (CPU, Memory, Network)
     * GET /api/containers/{id}/metrics
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     */
    @GetMapping("/containers/{id}/metrics")
    public ApiResponse<ContainerDetailResponseDTO> getContainerMetrics(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("컨테이너 메트릭 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        ContainerDetailResponseDTO response = containerService.getContainerMetrics(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "컨테이너 메트릭 조회 성공");
    }

    /**
     * 컨테이너 로그 조회 (커서 기반 무한 스크롤 + 다중 컨테이너 지원 + 정렬)
     * GET /api/logs
     *
     * Query Parameters:
     * - containerIds: 컨테이너 ID 리스트 (쉼표로 구분, 없으면 전체 컨테이너)
     * - lastLogId: 커서 - 마지막 로그 ID (무한 스크롤용)
     * - lastLoggedAt: 커서 - 마지막 로그 시간 (무한 스크롤용)
     * - size: 한번에 가져올 개수 (기본: 50)
     * - quickRange: Quick Range (초기 로드 시)
     * - startTime: Custom range 시작 시간 (초기 로드 시)
     * - endTime: Custom range 종료 시간 (초기 로드 시)
     * - logSource: 로그 소스 필터 (STDOUT, STDERR, RAW)
     * - agentName: Agent 이름 필터
     * - sortBy: 정렬 필드 (LOGGED_AT, CONTAINER_NAME, AGENT_NAME, LOG_MESSAGE) - 기본: LOGGED_AT
     * - direction: 정렬 방향 (ASC, DESC) - 기본: DESC
     *
     * 예시:
     * - 전체 컨테이너: GET /api/logs?size=50
     * - 단일 컨테이너: GET /api/logs?containerIds=1&size=50
     * - 다중 컨테이너: GET /api/logs?containerIds=1,2,3&size=50
     * - 무한 스크롤: GET /api/logs?containerIds=1,2&lastLogId=1000&lastLoggedAt=2025-10-30T10:30:00&size=50
     * - 필터링: GET /api/logs?containerIds=1&logSource=STDERR&size=50
     * - 정렬: GET /api/logs?containerIds=1,2&sortBy=CONTAINER_NAME&direction=ASC
     */
    @GetMapping("/logs")
    public ApiResponse<ContainerLogsResponseDTO> getContainerLogs(
            @RequestParam(required = false) List<Long> containerIds,
            @RequestParam(required = false) Long lastLogId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastLoggedAt,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(required = false) LogSource logSource,
            @RequestParam(required = false) String agentName,
            @RequestParam(required = false) LogSortField sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction
    ) {
        log.info("컨테이너 로그 조회 요청 - containerIds: {}, lastLogId: {}, lastLoggedAt: {}, size: {}, sortBy: {}, direction: {}",
                containerIds, lastLogId, lastLoggedAt, size, sortBy, direction);

        ContainerLogsResponseDTO response = containerService.getContainerLogs(
                containerIds,
                ContainerLogsRequest.of(lastLogId, lastLoggedAt, size, quickRange, startTime, endTime, logSource, agentName, sortBy, direction)
        );
        return ApiResponse.ok(response, "컨테이너 로그 조회 성공");
    }

    /**
     * 삭제된 컨테이너 목록 조회 (24시간 이내)
     * GET /api/containers/deleted
     *
     * 예시:
     * - GET /api/containers/deleted
     */
    @GetMapping("/containers/deleted")
    public ApiResponse<List<DeletedContainerResponseDTO>> getDeletedContainers() {
        log.info("삭제된 컨테이너 목록 조회 요청");

        List<DeletedContainerResponseDTO> deletedContainers = containerService.getDeletedContainers();
        return ApiResponse.ok(deletedContainers, "삭제된 컨테이너 목록 조회 성공");
    }

    // ==================== 시계열 데이터 전용 엔드포인트 ====================

    /**
     * CPU 사용률(%) 시계열 데이터 조회
     * GET /api/containers/{id}/metrics/cpu-usage
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics/cpu-usage?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics/cpu-usage?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     *
     * 응답: TimeSeriesResponse (자동 다운샘플링 적용, 메타데이터 포함)
     */
    @GetMapping("/containers/{id}/metrics/cpu-usage")
    public ApiResponse<TimeSeriesResponse> getCpuUsageTimeSeries(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("CPU 사용률 시계열 데이터 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        TimeSeriesResponse response = containerService.getCpuUsageTimeSeries(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "CPU 사용률 시계열 데이터 조회 성공");
    }

    /**
     * 메모리 사용률(%) 시계열 데이터 조회
     * GET /api/containers/{id}/metrics/memory-usage
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics/memory-usage?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics/memory-usage?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     *
     * 응답: TimeSeriesResponse (자동 다운샘플링 적용, 메타데이터 포함)
     */
    @GetMapping("/containers/{id}/metrics/memory-usage")
    public ApiResponse<TimeSeriesResponse> getMemoryUsageTimeSeries(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("메모리 사용률 시계열 데이터 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        TimeSeriesResponse response = containerService.getMemoryUsageTimeSeries(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "메모리 사용률 시계열 데이터 조회 성공");
    }

    /**
     * 네트워크 수신(RX) 속도 시계열 데이터 조회
     * GET /api/containers/{id}/metrics/network-rx
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics/network-rx?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics/network-rx?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     *
     * 응답: TimeSeriesResponse (자동 다운샘플링 적용, 메타데이터 포함, 단위: bytes/sec)
     */
    @GetMapping("/containers/{id}/metrics/network-rx")
    public ApiResponse<TimeSeriesResponse> getNetworkRxTimeSeries(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("네트워크 수신 속도 시계열 데이터 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        TimeSeriesResponse response = containerService.getNetworkRxTimeSeries(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "네트워크 수신 속도 시계열 데이터 조회 성공");
    }

    /**
     * 네트워크 송신(TX) 속도 시계열 데이터 조회
     * GET /api/containers/{id}/metrics/network-tx
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics/network-tx?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics/network-tx?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     *
     * 응답: TimeSeriesResponse (자동 다운샘플링 적용, 메타데이터 포함, 단위: bytes/sec)
     */
    @GetMapping("/containers/{id}/metrics/network-tx")
    public ApiResponse<TimeSeriesResponse> getNetworkTxTimeSeries(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("네트워크 송신 속도 시계열 데이터 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        TimeSeriesResponse response = containerService.getNetworkTxTimeSeries(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "네트워크 송신 속도 시계열 데이터 조회 성공");
    }

    /**
     * 네트워크 패킷 레이트 시계열 데이터 조회
     * GET /api/containers/{id}/metrics/network-packets
     *
     * Query Parameters:
     * - quickRange: LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR,
     *               LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS
     * - startTime: Custom range 시작 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     * - endTime: Custom range 종료 시간 (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)
     *
     * 예시:
     * - Quick Range: GET /api/containers/1/metrics/network-packets?quickRange=LAST_1_HOUR
     * - Custom Range: GET /api/containers/1/metrics/network-packets?startTime=2025-10-30T08:00:00&endTime=2025-10-30T10:00:00
     *
     * 응답: TimeSeriesResponse (자동 다운샘플링 적용, 메타데이터 포함, 단위: packets/sec, RX+TX 합계)
     */
    @GetMapping("/containers/{id}/metrics/network-packets")
    public ApiResponse<TimeSeriesResponse> getNetworkPacketsTimeSeries(
            @PathVariable("id") Long containerId,
            @RequestParam(required = false) QuickRangeType quickRange,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime
    ) {
        log.info("네트워크 패킷 레이트 시계열 데이터 조회 요청 - containerId: {}, quickRange: {}, startTime: {}, endTime: {}",
                containerId, quickRange, startTime, endTime);

        TimeSeriesResponse response = containerService.getNetworkPacketsTimeSeries(
                containerId,
                ContainerMetricsPageRequestDTO.of(quickRange, startTime, endTime)
        );
        return ApiResponse.ok(response, "네트워크 패킷 레이트 시계열 데이터 조회 성공");
    }

}