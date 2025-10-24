package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.container.domain.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    /**
     * 활성화된 모든 규칙 조회
     */
    List<AlertRule> findByIsEnabledTrue();
    /**
     * 특정 사용자의 모든 규칙 조회
     */
    List<AlertRule> findByMemberId(Long memberId);
    /**
     * 특정 사용자의 활성화된 규칙 조회
     */
    List<AlertRule> findByMemberIdAndIsEnabledTrue(Long memberId);
    /**
     * 특정 컨테이너의 활성화된 규칙 조회
     */
    List<AlertRule> findByContainerIdAndIsEnabledTrue(Long containerId);
    /**
     * 특정 컨테이너 + 메트릭 타입의 활성화된 규칙 조회
     */
    List<AlertRule> findByContainerIdAndMetricTypeAndIsEnabledTrue(
            Long containerId, MetricType metricType);
    /**
     * 특정 사용자 + 컨테이너의 규칙 조회
     */
    List<AlertRule> findByMemberIdAndContainerId(Long memberId, Long containerId);
    /**
     * 특정 메트릭 타입의 활성화된 모든 규칙 조회
     */
    List<AlertRule> findByMetricTypeAndIsEnabledTrue(MetricType metricType);
    /**
     * 특정 사용자 + 컨테이너 + 메트릭 타입 규칙 존재 여부 확인
     */
    boolean existsByMemberIdAndContainerIdAndMetricType(Long memberId, Long containerId, MetricType metricType);
}