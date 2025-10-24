package com.monito.domains.alert.controller;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.facade.AlertEvaluationFacade;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * 자동 알림 시스템 테스트용 컨트롤러
 * - 개발/테스트 환경에서만 사용
 */
@Slf4j
@RestController
@RequestMapping("/api/test/alerts")
@RequiredArgsConstructor
@Tag(name = "Alert Test (Dev Only)", description = "알림 시스템 테스트 API - 개발 전용, 프로덕션 환경에서는 비활성화")
@org.springframework.context.annotation.Profile({"dev", "local", "test"})
public class AlertTestController {

    private final AlertEvaluationFacade alertEvaluationFacade;
    private final ContainerRepository containerRepository;
    private final AlertRuleRepository alertRuleRepository;

    /**
     * 특정 컨테이너의 메트릭을 임의로 설정하여 알림 테스트
     */
    @Operation(summary = "컨테이너 메트릭 설정 및 알림 테스트",
            description = "컨테이너의 CPU/Memory 사용률을 강제로 설정하여 자동 알림 발생을 테스트합니다.")
    @PostMapping("/containers/{containerId}/metrics")
    public ApiResponse<String> setContainerMetrics(
            @PathVariable Long containerId,
            @RequestParam(required = false) BigDecimal cpuPercent,
            @RequestParam(required = false) BigDecimal memPercent) {

        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new IllegalArgumentException("컨테이너를 찾을 수 없습니다: " + containerId));

        // 메트릭 강제 설정
        if (cpuPercent != null) {
            container.updateCpuPercent(cpuPercent);
        }
        if (memPercent != null) {
            container.updateMemPercent(memPercent);
        }

        containerRepository.save(container);

        // 알림 규칙 평가 (자동 알림 발생)
        alertEvaluationFacade.evaluateContainer(container);

        String message = String.format(
                "컨테이너 '%s' 메트릭 설정 완료 - CPU: %s%%, Memory: %s%%. 알림 규칙 평가 실행됨.",
                container.getName(),
                container.getCpuPercent(),
                container.getMemPercent()
        );

        log.info(message);
        return ApiResponse.ok(message);
    }

    /**
     * 모든 컨테이너에 대해 알림 규칙 재평가
     */
    @Operation(summary = "모든 컨테이너 알림 재평가",
            description = "현재 등록된 모든 컨테이너에 대해 알림 규칙을 재평가합니다.")
    @PostMapping("/evaluate-all")
    public ApiResponse<String> evaluateAllContainers() {
        List<Container> containers = containerRepository.findAll();

        // Facade를 통한 일괄 평가
        alertEvaluationFacade.evaluateAllContainers(containers);

        String message = String.format("%d개 컨테이너에 대해 알림 규칙 평가 완료", containers.size());
        log.info(message);
        return ApiResponse.ok(message);
    }

    /**
     * 활성화된 AlertRule 조회
     */
    @Operation(summary = "활성화된 알림 규칙 조회",
            description = "현재 활성화된 모든 알림 규칙을 조회합니다.")
    @GetMapping("/rules")
    public ApiResponse<List<AlertRule>> getActiveRules() {
        List<AlertRule> rules = alertRuleRepository.findByIsEnabledTrue();
        return ApiResponse.ok(rules, "활성화된 알림 규칙 " + rules.size() + "개 조회 완료");
    }

    /**
     * 컨테이너 목록 조회 (테스트용)
     */
    @Operation(summary = "컨테이너 목록 조회",
            description = "등록된 모든 컨테이너를 조회합니다.")
    @GetMapping("/containers")
    public ApiResponse<List<ContainerInfo>> getContainers() {
        List<Container> containers = containerRepository.findAll();
        List<ContainerInfo> infos = containers.stream()
                .map(c -> new ContainerInfo(
                        c.getId(),
                        c.getName(),
                        c.getCpuPercent(),
                        c.getMemPercent(),
                        c.getAgent().getId()
                ))
                .toList();

        return ApiResponse.ok(infos, "컨테이너 " + infos.size() + "개 조회 완료");
    }

    // DTO for container info
    public record ContainerInfo(
            Long id,
            String name,
            BigDecimal cpuPercent,
            BigDecimal memPercent,
            Long agentId
    ) {}
}