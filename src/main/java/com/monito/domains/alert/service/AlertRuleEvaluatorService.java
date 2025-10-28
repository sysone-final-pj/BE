package com.monito.domains.alert.service;

import com.monito.domains.container.domain.ContainerStatsLog;

/**
 * 컨테이너 메트릭과 사용자별 AlertRule을 비교하여 알림 발생 여부 판단
 * - 중복 알림 방지 (쿨다운)
 */
public interface AlertRuleEvaluatorService {

    /**
     * 컨테이너 통계 로그 평가 및 알림 생성
     * - 해당 컨테이너에 대한 모든 활성화된 AlertRule을 조회하여 평가
     */
    void evaluateContainer(ContainerStatsLog containerStats);

    /**
     * 쿨다운 캐시 정리 (옵션)
     * 주기적으로 오래된 항목 제거 (메모리 관리)
     */
    void cleanupExpiredCooldowns(int retentionHours);
}
