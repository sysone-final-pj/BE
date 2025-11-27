/**
 * 컨테이너 히스토리 API Controller
 * - 컨테이너의 과거 메트릭 데이터 조회
 */
package com.monito.domains.history.controller;

import com.monito.domains.history.dto.request.ContainerChartRequest;
import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerChartResponse;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
import com.monito.domains.history.dto.response.ContainerListForHistoryDTO;
import com.monito.domains.history.service.ContainerHistoryService;
import com.monito.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
/**
 작성자: 이지민
 */
@Tag(name = "Container History", description = "컨테이너 히스토리 조회 API - 과거 메트릭 데이터 조회 및 분석")
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final ContainerHistoryService containerHistoryService;

    @Operation(
            summary = "컨테이너 히스토리 조회",
            description = """
                    컨테이너의 과거 메트릭 데이터를 기간별로 조회합니다.

                    **주요 기능:**
                    - 특정 기간 내 컨테이너 메트릭 조회 (CPU, Memory, Network, Storage 등)
                    - 삭제된 컨테이너 포함 조회 가능
                    - 페이지네이션 지원
                    - 정렬 옵션 제공

                    **날짜 형식:** yyyy-MM-dd'T'HH:mm:ss (예: 2024-01-01T00:00:00)
                    """
    )
    @GetMapping("/containers")
    public ApiResponse<ContainerHistoryPageResponse> getContainerHistory(
            @Parameter(description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(description = "컨테이너 ID (선택, 특정 컨테이너만 조회)", example = "123")
            @RequestParam(required = false) Long containerId,

            @Parameter(description = "삭제 여부 필터 (0: 활성, 1: 삭제됨, null: 전체)", example = "0")
            @RequestParam(required = false) Integer isDeleted,

            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(description = "페이지 크기 (한 페이지당 데이터 개수)", example = "20")
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(description = "정렬 기준 (엔티티 필드명)", example = "collectedAt")
            @RequestParam(defaultValue = "collectedAt") String sortBy,

            @Parameter(description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)", example = "DESC")
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("컨테이너 히스토리 조회 요청 - startTime: {}, endTime: {}, containerId: {}, isDeleted: {}, page: {}, size: {}",
                startTime, endTime, containerId, isDeleted, page, size);

        ContainerHistoryRequest request = new ContainerHistoryRequest(
                startTime, endTime, containerId, isDeleted,
                page, size, sortBy, sortDirection
        );

        ContainerHistoryPageResponse response = containerHistoryService.getContainerHistory(request);

        return ApiResponse.ok(response);
    }

    @Operation(
            summary = "히스토리 조회용 컨테이너 목록",
            description = """
                    히스토리 조회 시 필터링할 수 있도록 컨테이너 목록을 제공합니다.

                    **주요 기능:**
                    - 활성 컨테이너와 삭제된 컨테이너 목록 조회
                    - 컨테이너 ID, 이름, 해시 정보 제공
                    - 삭제 여부 필터링 지원

                    **반환 필드:**
                    - id: 컨테이너 ID
                    - containerName: 컨테이너 이름
                    - containerHash: 컨테이너 해시 (12자리)
                    - isDeleted: 삭제 여부 (0: 활성, 1: 삭제됨)
                    """
    )
    @GetMapping("/containers/list")
    public ApiResponse<List<ContainerListForHistoryDTO>> getContainerListForHistory(
            @Parameter(
                    description = """
                            삭제 여부 필터:
                            - 0: 활성 컨테이너만 조회
                            - 1: 삭제된 컨테이너만 조회
                            - null: 전체 조회 (활성 + 삭제됨)
                            """,
                    example = "0"
            )
            @RequestParam(required = false) Integer isDeleted
    ) {
        log.info("히스토리 조회용 컨테이너 목록 요청 - isDeleted: {}", isDeleted);

        List<ContainerListForHistoryDTO> containers = containerHistoryService.getContainerListForHistory(isDeleted);

        return ApiResponse.ok(containers, "컨테이너 목록을 성공적으로 조회했습니다.");
    }

    @Operation(
            summary = "컨테이너 차트 데이터 조회",
            description = """
                    컨테이너의 특정 메트릭 필드를 시계열 데이터로 조회합니다.

                    **주요 기능:**
                    - ContainerHistoryResponse의 특정 필드만 선택하여 조회
                    - 시계열 데이터 형태로 반환 (timestamp, value)
                    - 차트 렌더링에 최적화된 데이터 구조

                    **지원 메트릭 필드:**
                    - **CPU**: cpuPercent, cpuCoreUsage, cpuUsageTotal 등
                    - **Memory**: memPercent, memUsage, memMaxUsage 등
                    - **Network**: rxBytes, txBytes, rxBytesPerSec, txBytesPerSec 등
                    - **Block I/O**: blkRead, blkWrite, blkReadPerSec, blkWritePerSec 등
                    - **Storage**: sizeRw, sizeRootFs 등

                    **날짜 형식:** yyyy-MM-dd'T'HH:mm:ss (예: 2024-01-01T00:00:00)
                    """
    )
    @GetMapping("/containers/chart")
    public ApiResponse<ContainerChartResponse> getContainerChart(
            @Parameter(description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-01T00:00:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)", required = true, example = "2024-01-31T23:59:59")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(description = "컨테이너 ID", required = true, example = "123")
            @RequestParam Long containerId,

            @Parameter(description = """
                    메트릭 필드명 (ContainerHistoryResponse의 필드명)

                    예시:
                    - cpuPercent: CPU 사용률
                    - memPercent: 메모리 사용률
                    - rxBytes: 네트워크 수신 바이트
                    - txBytes: 네트워크 송신 바이트
                    - blkRead: 블록 읽기 바이트
                    - blkWrite: 블록 쓰기 바이트
                    """, required = true, example = "cpuPercent")
            @RequestParam String metricField
    ) {
        log.info("컨테이너 차트 데이터 조회 요청 - startTime: {}, endTime: {}, containerId: {}, metricField: {}",
                startTime, endTime, containerId, metricField);

        ContainerChartRequest request = new ContainerChartRequest(
                startTime, endTime, containerId, metricField
        );

        ContainerChartResponse response = containerHistoryService.getContainerChart(request);

        return ApiResponse.ok(response, "차트 데이터를 성공적으로 조회했습니다.");
    }
}
