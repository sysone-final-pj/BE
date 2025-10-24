package com.monito.domains.alert.facade;

import com.monito.domains.alert.service.AlertRuleEvaluatorService;
import com.monito.domains.container.domain.Container;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 알림 평가 Facade
 * - 컨테이너 메트릭 평가 프로세스 전체를 조율
 * - 외부(Controller, Scheduler, Agent)에서 호출하는 단일 진입점
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AlertEvaluationFacade {

    private final AlertRuleEvaluatorService alertRuleEvaluator;

    /**
     * 단일 컨테이너 메트릭 평가 및 알림 생성
     */
    public void evaluateContainer(Container container) {
        log.debug("컨테이너 평가 시작: containerId={}, containerName={}",
                container.getId(), container.getName());

        alertRuleEvaluator.evaluateContainer(container);

        log.debug("컨테이너 평가 완료: containerId={}", container.getId());
    }

    /**
     * 여러 컨테이너 일괄 평가
     */
    public void evaluateAllContainers(List<Container> containers) {
        log.info("전체 컨테이너 평가 시작: 총 {}개", containers.size());

        containers.forEach(this::evaluateContainer);

        log.info("전체 컨테이너 평가 완료");
    }

    /**
     * 쿨다운 캐시 정리
     * 주기적으로 호출하여 메모리 관리
     */
    public void cleanupExpiredCooldowns(int retentionHours) {
        log.info("쿨다운 캐시 정리 시작: retentionHours={}", retentionHours);

        alertRuleEvaluator.cleanupExpiredCooldowns(retentionHours);

        log.info("쿨다운 캐시 정리 완료");
    }
}
