/**
 * Agent별 컨테이너 개수 집계 DTO
 * - 대시보드 통계용
 */
package com.monito.domains.dashboard.dto.response.metrics;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 작성자: 이지민
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PUBLIC)
public class AgentContainerCountDTO {

    /**
     * Agent ID
     */
    private Long agentId;

    /**
     * Agent 이름
     */
    private String agentName;

    /**
     * 해당 Agent의 컨테이너 개수
     */
    private Long containerCount;
}