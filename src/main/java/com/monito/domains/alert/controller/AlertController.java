package com.monito.domains.alert.controller;

import com.monito.domains.alert.domain.Alert;
import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.alert.dto.request.AlertCreateRequestDTO;
import com.monito.domains.alert.dto.response.AlertResponseDTO;
import com.monito.domains.alert.service.AlertService;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert", description = "알림 관련 API")
public class AlertController {

    private final AlertService alertService;

    /**
     * 읽지 않은 알림 조회(개인)
     */
    @Operation(summary = "읽지 않은 알림 조회", description = "현재 사용자의 읽지 않은 알림을 조회합니다.")
    @GetMapping("/unread")
    public ApiResponse<List<AlertResponseDTO>> getUnreadAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertResponseDTO> alerts = alertService.getUnreadAlertsAsResponse(userDetails.getId());
        return ApiResponse.ok(alerts, "읽지 않은 알림 조회 성공");
    }

    /**
     * 모든 알림 조회(개인))
     */
    @Operation(summary = "모든 알림 조회", description = "현재 사용자의 모든 알림을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlertResponseDTO>> getAllAlerts(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertResponseDTO> alerts = alertService.getAllAlertsAsResponse(userDetails.getId());
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
     * 관리자용 브로드캐스트 알림(관리자용 알림)
     */
    @Operation(summary = "브로드캐스트 알림", description = "모든 사용자에게 알림을 전송합니다. (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/broadcast")
    public ApiResponse<Void> broadcastAlert(
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam AlertLevel alertLevel) {
        alertService.broadcastAlert(title, message, alertLevel);
        return ApiResponse.ok("브로드캐스트 알림이 전송되었습니다.");
    }

    /**
     * 알림 생성
     */
    @Operation(summary = "알림 생성", description = "새로운 알림을 생성합니다.")
    @PostMapping
    public ApiResponse<AlertResponseDTO> createAlert(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlertCreateRequestDTO request) {
        AlertResponseDTO response = alertService.createAlert(userDetails.getId(), request);
        return ApiResponse.ok(response, "알림이 생성되었습니다.");
    }

    /**
     * 특정 알림 조회(개인)
     */
    @Operation(summary = "특정 알림 조회", description = "알림 ID로 특정 알림을 조회합니다. (본인 알림만 가능)")
    @GetMapping("/{id}")
    public ApiResponse<AlertResponseDTO> getAlert(
            @PathVariable("id") Long alertId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertResponseDTO response = alertService.getAlert(alertId, userDetails.getId());
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
}