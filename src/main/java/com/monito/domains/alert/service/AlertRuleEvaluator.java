package com.monito.domains.alert.service;

import com.monito.domains.container.domain.Container;

/**
 * 알림 규칙 평가 서비스 인터페이스
 * - 컨테이너 메트릭과 사용자별 AlertRule을 비교하여 알림 발생 여부 판단
 * - 중복 알림 방지 (쿨다운 기간)
 */
public interface AlertRuleEvaluator {

    /**
     * 컨테이너 메트릭 평가 및 알림 생성
     * 해당 컨테이너에 대한 모든 활성화된 AlertRule을 조회하여 평가
     */
    void evaluateContainer(Container container);

    /**
     * 쿨다운 캐시 정리 (옵션)
     * 주기적으로 오래된 항목 제거 (메모리 관리)
     */
    void cleanupExpiredCooldowns(int retentionHours);
}
