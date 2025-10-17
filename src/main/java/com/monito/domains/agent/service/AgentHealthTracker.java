package com.monito.domains.agent.service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Agent의 헬스 상태를 추적하는 컴포넌트
 * - 연속 실패 횟수 추적
 * - 마지막 성공 시간 기록
 */
@Component
public class AgentHealthTracker {

    private final Map<Long, Integer> failureCounts = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastSuccessTime = new ConcurrentHashMap<>();

    /**
     * Agent 폴링 성공 기록
     * - 실패 카운트를 0으로 초기화
     * - 마지막 성공 시간 업데이트
     */
    public void recordSuccess(Long agentId) {
        failureCounts.put(agentId, 0);
        lastSuccessTime.put(agentId, Instant.now());
    }

    /**
     * Agent 폴링 실패 기록
     * - 실패 카운트를 1 증가
     */
    public void recordFailure(Long agentId) {
        failureCounts.merge(agentId, 1, Integer::sum);
    }

    /**
     * Agent의 연속 실패 횟수 조회
     */
    public int getFailureCount(Long agentId) {
        return failureCounts.getOrDefault(agentId, 0);
    }

    /**
     * Agent의 마지막 성공 시간 조회
     */
    public Instant getLastSuccessTime(Long agentId) {
        return lastSuccessTime.get(agentId);
    }

    /**
     * Agent 상태 초기화 (Agent 삭제 시 호출)
     */
    public void clear(Long agentId) {
        failureCounts.remove(agentId);
        lastSuccessTime.remove(agentId);
    }
}