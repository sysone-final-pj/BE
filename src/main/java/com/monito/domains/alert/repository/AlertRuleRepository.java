package com.monito.domains.alert.repository;

import com.monito.domains.alert.domain.AlertRule;
import com.monito.domains.container.domain.MetricType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRuleRepository extends JpaRepository<AlertRule, Long> {

    // 활성화된 모든 규칙 조회
    List<AlertRule> findByIsEnabledTrueAndIsDeletedFalse();

    // 특정 사용자의 모든 규칙 조회
    List<AlertRule> findByMemberIdAndIsDeletedFalse(Long memberId);

    // 특정 사용자의 활성화된 규칙 조회
    List<AlertRule> findByMemberIdAndIsEnabledTrueAndIsDeletedFalse(Long memberId);

    // 특정 컨테이너의 활성화된 규칙 조회
    List<AlertRule> findByContainerIdAndIsEnabledTrueAndIsDeletedFalse(Long containerId);

    // 특정 컨테이너 + 메트릭 타입의 활성화된 규칙 조회
    List<AlertRule> findByContainerIdAndMetricTypeAndIsEnabledTrueAndIsDeletedFalse(
            Long containerId, MetricType metricType);

    // 특정 사용자 + 컨테이너의 규칙 조회
    List<AlertRule> findByMemberIdAndContainerIdAndIsDeletedFalse(
            Long memberId, Long containerId);

    // 특정 메트릭 타입의 활성화된 모든 규칙 조회
    List<AlertRule> findByMetricTypeAndIsEnabledTrueAndIsDeletedFalse(MetricType metricType);

    // 특정 사용자 + 컨테이너 + 메트릭 타입 규칙 존재 여부 확인
    boolean existsByMemberIdAndContainerIdAndMetricTypeAndIsDeletedFalse(Long memberId, Long containerId, MetricType metricType);
}