package com.monito.domains.alert.service;

import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import com.monito.domains.alert.dto.response.AlertRuleResponseDTO;

import java.util.List;

/**
 * 알림 규칙 서비스 인터페이스
 */
public interface AlertRuleService {

    /**
     * 알림 규칙 생성
     */
    AlertRuleResponseDTO createAlertRule(Long memberId, AlertRuleCreateRequestDTO request);

    /**
     * 특정 알림 규칙 조회 (본인 규칙만 조회 가능)
     */
    AlertRuleResponseDTO getAlertRule(Long ruleId, Long memberId);

    /**
     * 사용자의 모든 알림 규칙 조회
     */
    List<AlertRuleResponseDTO> getAllAlertRules(Long memberId);

    /**
     * 특정 컨테이너의 알림 규칙 조회
     */
    List<AlertRuleResponseDTO> getAlertRulesByContainer(Long memberId, Long containerId);

    /**
     * 알림 규칙 수정 (본인 규칙만 수정 가능)
     */
    AlertRuleResponseDTO updateAlertRule(Long ruleId, Long memberId, AlertRuleUpdateRequestDTO request);

    /**
     * 알림 규칙 삭제 (Soft Delete, 본인 규칙만 삭제 가능)
     */
    void deleteAlertRule(Long ruleId, Long memberId);

    /**
     * 알림 규칙 활성화/비활성화
     */
    AlertRuleResponseDTO toggleAlertRule(Long ruleId, Long memberId, boolean enabled);
}
