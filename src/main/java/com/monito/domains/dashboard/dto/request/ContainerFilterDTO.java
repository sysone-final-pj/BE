package com.monito.domains.dashboard.dto.request;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 대시보드 컨테이너 필터 DTO
 * - 검색 키워드
 * - favorite/All 선택
 * - State 다중 선택
 * - Health 다중 선택
 * - Agent 다중 선택
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerFilterDTO {

    /**
     * 검색 키워드 (컨테이너 이름, 이미지명 검색)
     */
    private String keyword;

    /**
     * 즐겨찾기만 보기 (true: 즐겨찾기만, false or null: 전체)
     */
    private Boolean favoriteOnly;

    /**
     * 컨테이너 상태 필터 (다중 선택 가능)
     * RUNNING, RESTARTING, PAUSED, CREATED, EXIT, DEAD
     */
    private List<ContainerState> states;

    /**
     * 컨테이너 헬스 필터 (다중 선택 가능)
     * HEALTHY, UNHEALTHY, STARTING, NONE, UNKNOWN
     */
    private List<ContainerHealth> healths;

    /**
     * 에이전트 ID 필터 (다중 선택 가능)
     */
    private List<Long> agentIds;
}