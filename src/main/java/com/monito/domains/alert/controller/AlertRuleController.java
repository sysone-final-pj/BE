package com.monito.domains.alert.controller;

import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import com.monito.domains.alert.dto.response.AlertRuleResponseDTO;
import com.monito.domains.alert.service.AlertRuleService;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.security.userdetails.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/alert-rules")
@RequiredArgsConstructor
@Tag(name = "Alert Rule", description = "알림 규칙 관리 API")
public class AlertRuleController {

    private final AlertRuleService alertRuleService;

    /**
     * 알림 규칙 생성
     */
    @Operation(summary = "알림 규칙 생성", description = "새로운 알림 규칙을 생성합니다.")
    @PostMapping
    public ApiResponse<AlertRuleResponseDTO> createAlertRule(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlertRuleCreateRequestDTO request) {
        AlertRuleResponseDTO response = alertRuleService.createAlertRule(userDetails.getId(), request);
        return ApiResponse.ok(response, "알림 규칙이 생성되었습니다.");
    }

    /**
     * 특정 알림 규칙 조회
     */
    @Operation(summary = "특정 알림 규칙 조회", description = "알림 규칙 ID로 특정 알림 규칙을 조회합니다. (본인 규칙만 가능)")
    @GetMapping("/{ruleId}")
    public ApiResponse<AlertRuleResponseDTO> getAlertRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertRuleResponseDTO response = alertRuleService.getAlertRule(ruleId, userDetails.getId());
        return ApiResponse.ok(response, "알림 규칙 조회 성공");
    }

    /**
     * 사용자의 모든 알림 규칙 조회
     */
    @Operation(summary = "모든 알림 규칙 조회", description = "현재 사용자의 모든 알림 규칙을 조회합니다.")
    @GetMapping
    public ApiResponse<List<AlertRuleResponseDTO>> getAllAlertRules(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertRuleResponseDTO> rules = alertRuleService.getAllAlertRules(userDetails.getId());
        return ApiResponse.ok(rules, "알림 규칙 목록 조회 성공");
    }

    /**
     * 특정 컨테이너의 알림 규칙 조회
     */
    @Operation(summary = "컨테이너별 알림 규칙 조회", description = "특정 컨테이너에 설정된 알림 규칙을 조회합니다.")
    @GetMapping("/container/{containerId}")
    public ApiResponse<List<AlertRuleResponseDTO>> getAlertRulesByContainer(
            @PathVariable Long containerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<AlertRuleResponseDTO> rules = alertRuleService.getAlertRulesByContainer(
                userDetails.getId(), containerId);
        return ApiResponse.ok(rules, "컨테이너별 알림 규칙 조회 성공");
    }

    /**
     * 알림 규칙 수정
     */
    @Operation(summary = "알림 규칙 수정", description = "알림 규칙을 수정합니다. (본인 규칙만 가능)")
    @PatchMapping("/{ruleId}")
    public ApiResponse<AlertRuleResponseDTO> updateAlertRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AlertRuleUpdateRequestDTO request) {
        AlertRuleResponseDTO response = alertRuleService.updateAlertRule(
                ruleId, userDetails.getId(), request);
        return ApiResponse.ok(response, "알림 규칙이 수정되었습니다.");
    }

    /**
     * 알림 규칙 활성화/비활성화
     */
    @Operation(summary = "알림 규칙 활성화/비활성화", description = "알림 규칙을 활성화하거나 비활성화합니다.")
    @PatchMapping("/{ruleId}/toggle")
    public ApiResponse<AlertRuleResponseDTO> toggleAlertRule(
            @PathVariable Long ruleId,
            @RequestParam boolean enabled,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        AlertRuleResponseDTO response = alertRuleService.toggleAlertRule(
                ruleId, userDetails.getId(), enabled);
        return ApiResponse.ok(response,
                enabled ? "알림 규칙이 활성화되었습니다." : "알림 규칙이 비활성화되었습니다.");
    }

    /**
     * 알림 규칙 삭제
     */
    @Operation(summary = "알림 규칙 삭제", description = "알림 규칙을 삭제합니다. (본인 규칙만 가능)")
    @DeleteMapping("/{ruleId}")
    public ApiResponse<Void> deleteAlertRule(
            @PathVariable Long ruleId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        alertRuleService.deleteAlertRule(ruleId, userDetails.getId());
        return ApiResponse.ok("알림 규칙이 삭제되었습니다.");
    }
}
