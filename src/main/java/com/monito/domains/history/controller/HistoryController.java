package com.monito.domains.history.controller;

import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
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

/**
 * 컨테이너 히스토리 API Controller
 * - 컨테이너의 과거 메트릭 데이터 조회
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
            @Parameter(
                    description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)",
                    required = true,
                    example = "2024-01-01T00:00:00"
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,

            @Parameter(
                    description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)",
                    required = true,
                    example = "2024-01-31T23:59:59"
            )
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,

            @Parameter(
                    description = "컨테이너 ID (선택, 특정 컨테이너만 조회)",
                    example = "123"
            )
            @RequestParam(required = false) Long containerId,

            @Parameter(
                    description = """
                            삭제 여부 필터:
                            - 0: 활성 컨테이너만 조회
                            - 1: 삭제된 컨테이너만 조회
                            - null: 전체 조회 (삭제 여부 관계없이)
                            """,
                    example = "0"
            )
            @RequestParam(required = false) Integer isDeleted,

            @Parameter(
                    description = "페이지 번호 (0부터 시작)",
                    example = "0"
            )
            @RequestParam(defaultValue = "0") Integer page,

            @Parameter(
                    description = "페이지 크기 (한 페이지당 데이터 개수)",
                    example = "20"
            )
            @RequestParam(defaultValue = "20") Integer size,

            @Parameter(
                    description = "정렬 기준 (엔티티 필드명, 예: collectedAt, cpuPercent, memUsage 등)",
                    example = "collectedAt"
            )
            @RequestParam(defaultValue = "collectedAt") String sortBy,

            @Parameter(
                    description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)",
                    example = "DESC"
            )
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("컨테이너 히스토리 조회 요청 - startTime: {}, endTime: {}, containerId: {}, isDeleted: {}, page: {}, size: {}",
                startTime, endTime, containerId, isDeleted, page, size);

        ContainerHistoryRequest request = new ContainerHistoryRequest(
                startTime,
                endTime,
                containerId,
                isDeleted,
                page,
                size,
                sortBy,
                sortDirection
        );

        ContainerHistoryPageResponse response = containerHistoryService.getContainerHistory(request);

        return ApiResponse.ok(response);
    }
}
