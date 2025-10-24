package com.monito.domains.alert.controller;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.alert.facade.AlertEvaluationFacade;
import com.monito.domains.alert.repository.AlertRuleRepository;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerState;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.repository.ContainerRepository;
import com.monito.global.common.response.ApiResponse;
import com.monito.global.exception.ExceptionMessage;
import com.monito.global.exception.NotFoundException;
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
            @RequestParam(required = false) BigDecimal memPercent,
            @RequestParam(required = false) Long rxMbps,
            @RequestParam(required = false) Long txMbps) {

        Container container = containerRepository.findById(containerId)
                .orElseThrow(() -> new NotFoundException(ExceptionMessage.CONTAINER_NOT_FOUND));

        // 테스트용 ContainerStatsLog 생성 (임시 데이터)
        ContainerStatsLog testStats = ContainerStatsLog.builder()
                .container(container)
                .containerHash(container.getContainerHash())
                .state(ContainerState.RUNNING)
                .cpuPercent(cpuPercent != null ? cpuPercent : BigDecimal.ZERO)
                .memPercent(memPercent != null ? memPercent : BigDecimal.ZERO)
                .rxMbps(rxMbps != null ? rxMbps : 0L)
                .txMbps(txMbps != null ? txMbps : 0L)
                // 필수 필드들을 기본값으로 설정
                .hostCpuUsageTotal(0L)
                .cpuUsageTotal(0L)
                .cpuUser(0L)
                .cpuSystem(0L)
                .cpuQuota(container.getCpuQuota())
                .cpuPeriod(container.getCpuPeriod())
                .cpuLimit(container.getCpuLimit())
                .onlineCpus(container.getOnlineCpus())
                .throttlingPeriods(0L)
                .throttledPeriods(0L)
                .throttledTime(0L)
                .oomKills(0)
                .memUsage(0L)
                .memLimit(container.getMemLimit())
                .memMaxUsage(0L)
                .memRss(0L)
                .memCache(0L)
                .blkRead(0L)
                .blkWrite(0L)
                .rxBytes(0L)
                .txBytes(0L)
                .rxPps(0L)
                .txPps(0L)
                .rxErrors(0)
                .txErrors(0)
                .rxDropped(0)
                .txDropped(0)
                .build();

        // 알림 규칙 평가 (자동 알림 발생)
        alertEvaluationFacade.evaluateContainerStats(testStats);

        String message = String.format(
                "컨테이너 '%s' 메트릭 테스트 완료 - CPU: %s%%, Memory: %s%%, Network: %d Mbps. 알림 규칙 평가 실행됨.",
                container.getName(),
                cpuPercent,
                memPercent,
                (rxMbps != null ? rxMbps : 0L) + (txMbps != null ? txMbps : 0L)
        );

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
                        c.getContainerHash(),
                        c.getAgent().getId()
                ))
                .toList();

        return ApiResponse.ok(infos, "컨테이너 " + infos.size() + "개 조회 완료");
    }

    // DTO for container info
    public record ContainerInfo(
            Long id,
            String name,
            String containerHash,
            Long agentId
    ) {}
}