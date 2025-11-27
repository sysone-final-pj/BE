package com.monito.domains.alert.controller;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertFilterDTO;
import com.monito.domains.alert.dto.request.AlertSortType;
import com.monito.domains.alert.dto.response.AlertDetailResponseDTO;
import com.monito.domains.alert.dto.response.AlertListItemResponseDTO;
import com.monito.domains.alert.service.AlertService;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.container.dto.request.QuickRangeType;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
/**
 작성자: 이지민
 */
@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "알림 관련 API")
public class AlertController {

    private final AlertService alertService;

    /**
     * 읽지 않은 알림 개수 조회 (배지용)
     */
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 사용자의 읽지 않은 알림 개수를 조회합니다. (배지 표시용)")
    @GetMapping("/unread/count")
    public ApiResponse<Long> getUnreadAlertCount(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        long count = alertService.getUnreadAlertCount(userDetails.getId());
        return ApiResponse.ok(count, "읽지 않은 알림 개수 조회 성공");
    }

    /**
     * 읽지 않은 알림 목록 조회(개인)
     */
    @Operation(summary = "읽지 않은 알림 목록 조회", description = "현재 사용자의 읽지 않은 알림 목록을 조회합니다.")
    @GetMapping("/unread")
    public ApiResponse<List<AlertListItemResponseDTO>> getUnreadAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertListItemResponseDTO> alerts = alertService.getUnreadAlertsAsResponse(userDetails.getId());
        return ApiResponse.ok(alerts, "읽지 않은 알림 조회 성공");
    }

    /**
     * 모든 알림 목록 조회(개인)
     */
    @Operation(summary = "모든 알림 목록 조회", description = "현재 사용자의 모든 알림 목록을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlertListItemResponseDTO>> getAllAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertListItemResponseDTO> alerts = alertService.getAllAlertsAsResponse(userDetails.getId());
        return ApiResponse.ok(alerts, "알림 조회 성공");
    }

    /**
     * 알림 읽음 처리(개인)
     */
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 상태로 변경합니다. (본인 알림만 가능)")
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> markAsRead(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.markAsRead(alertId, userDetails.getId());
        return ApiResponse.ok("알림이 읽음 처리되었습니다.");

    }

    /**
     * 특정 알림 상세 조회(개인)
     */
    @Operation(summary = "특정 알림 상세 조회", description = "알림 ID로 특정 알림의 상세 정보를 조회합니다. (본인 알림만 가능)")
    @GetMapping("/{id}")
    public ApiResponse<AlertDetailResponseDTO> getAlert(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertDetailResponseDTO response = alertService.getAlert(alertId, userDetails.getId());
        return ApiResponse.ok(response, "알림 조회 성공");
    }

    /**
     * 알림 삭제(개인)
     */
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다. (본인 알림만 가능)")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAlert(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteAlert(alertId, userDetails.getId());
        return ApiResponse.ok("알림이 삭제되었습니다.");
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Operation(summary = "모든 알림 읽음 처리", description = "사용자의 모든 알림을 읽음 처리합니다.")
    @PatchMapping("/read-all")
    public ApiResponse<Void> markAllAsRead(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.markAllAsRead(userDetails.getId());
        return ApiResponse.ok("모든 알림이 읽음 처리되었습니다.");
    }

    /**
     * 모든 알림 삭제(개인)
     */
    @Operation(summary = "모든 알림 삭제", description = "사용자의 모든 알림을 삭제합니다.")
    @DeleteMapping("/all")
    public ApiResponse<Void> deleteAllAlerts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteAllAlerts(userDetails.getId());
        return ApiResponse.ok("모든 알림이 삭제되었습니다.");
    }

    /**
     * 읽은 알림 모두 삭제(개인)
     */
    @Operation(summary = "읽은 알림 모두 삭제", description = "사용자의 읽은 알림을 모두 삭제합니다.")
    @DeleteMapping("/read")
    public ApiResponse<Void> deleteReadAlerts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        alertService.deleteReadAlerts(userDetails.getId());
        return ApiResponse.ok("읽은 알림이 모두 삭제되었습니다.");
    }

    /**
     * 필터 조건에 따른 알림 조회
     */
    @Operation(
            summary = "필터 조건으로 알림 조회",
            description = """
                    알림을 다양한 조건으로 필터링하고 정렬합니다.

                    **사용 가능한 필터:**
                    - alertLevel: 경고 레벨 (CRITICAL, HIGH, WARNING, INFO)
                    - metricType: 메트릭 타입 (CPU_PERCENT, MEM_PERCENT, DISK_USAGE_GB, NETWORK_TOTAL_BYTES 등)
                    - agentName: 에이전트 이름 (부분 일치)
                    - containerName: 컨테이너 이름 (부분 일치)
                    - quickRangeType: 빠른 시간 범위 선택 (LAST_5_MINUTES, LAST_10_MINUTES 등)
                      * quickRangeType이 있으면 collectedAtFrom/To는 무시됨
                    - collectedAtFrom/To: 수집 시간 범위 (예: 2024-10-01T00:00:00) - quickRangeType이 없을 때 사용
                    - createdAtFrom/To: 생성 시간 범위 (예: 2024-10-01T00:00:00)
                    - isRead: 읽음 여부 (true/false)
                    - sortBy: 정렬 기준 (ALERT_LEVEL, METRIC_TYPE, CONTAINER_NAME, METRIC_VALUE, COLLECTED_AT, CREATED_AT)
                    """
    )
    @GetMapping("/filter")
    public ApiResponse<List<AlertListItemResponseDTO>> getAlertsWithFilter(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Parameter(description = "경고 레벨 (CRITICAL, HIGH, WARNING, INFO)")
            @RequestParam(required = false) AlertLevel alertLevel,

            //@io.swagger.v3.oas.annotations.Parameter(description = "메트릭 타입 (CPU_PERCENT, MEM_PERCENT, DISK_USAGE_GB, NETWORK_TOTAL_BYTES 등)")
            @Parameter(description = "메트릭 타입 (CPU_PERCENT, MEM_PERCENT, DISK_USAGE_GB, NETWORK_TOTAL_BYTES 등)")
            @RequestParam(required = false) MetricType metricType,

            @Parameter(description = "에이전트 이름 (부분 일치)")
            @RequestParam(required = false) String agentName,

            @Parameter(description = "컨테이너 이름 (부분 일치)")
            @RequestParam(required = false) String containerName,

            @Parameter(description = "빠른 시간 범위 선택 (LAST_5_MINUTES, LAST_10_MINUTES, LAST_30_MINUTES, LAST_1_HOUR, LAST_3_HOURS, LAST_6_HOURS, LAST_12_HOURS, LAST_24_HOURS). 이 값이 있으면 collectedAtFrom/To는 무시됨")
            @RequestParam(required = false) QuickRangeType quickRangeType,

            @Parameter(description = "수집 시작 시간 (ISO 8601 형식: 2024-10-01T00:00:00) - quickRangeType이 없을 때 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime collectedAtFrom,

            @Parameter(description = "수집 종료 시간 (ISO 8601 형식: 2024-10-31T23:59:59) - quickRangeType이 없을 때 사용")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime collectedAtTo,

            @Parameter(description = "생성 시작 시간 (ISO 8601 형식: 2024-10-01T00:00:00)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtFrom,

            @Parameter(description = "생성 종료 시간 (ISO 8601 형식: 2024-10-31T23:59:59)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAtTo,

            @Parameter(description = "읽음 여부 (true: 읽음, false: 안읽음)")
            @RequestParam(required = false) Boolean isRead,

            @Parameter(description = "정렬 기준 (ALERT_LEVEL, METRIC_TYPE, CONTAINER_NAME, METRIC_VALUE, COLLECTED_AT). 기본값: CREATED_AT")
            @RequestParam(required = false) AlertSortType sortBy
    ) {
        AlertFilterDTO filter = AlertFilterDTO.builder()
                .alertLevel(alertLevel)
                .metricType(metricType)
                .agentName(agentName)
                .containerName(containerName)
                .quickRangeType(quickRangeType)
                .collectedAtFrom(collectedAtFrom)
                .collectedAtTo(collectedAtTo)
                .createdAtFrom(createdAtFrom)
                .createdAtTo(createdAtTo)
                .isRead(isRead)
                .sortType(sortBy)
                .build();

        List<AlertListItemResponseDTO> alerts = alertService.getAlertsWithFilter(userDetails.getId(), filter);
        return ApiResponse.ok(alerts, "필터링된 알림 조회 성공");
    }
}