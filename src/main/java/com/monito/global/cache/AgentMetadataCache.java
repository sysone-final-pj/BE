package com.monito.global.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 메타데이터 인메모리 캐시
 * - Agent의 시스템 정보 (hostTotalMemory, hostCpuCores 등)를 캐싱
 * - WebSocket 연결 시 업데이트
 * - 메트릭 계산 시 조회
 * 향후 Redis로 전환 가능
 */
@Slf4j
@Component
public class AgentMetadataCache {

    private final ConcurrentHashMap<String, AgentMetadata> cache = new ConcurrentHashMap<>();

    /**
     * Agent 메타데이터 업데이트 또는 추가
     * @param agentKey Agent 식별 키
     * @param metadata Agent 메타데이터
     */
    public void updateMetadata(String agentKey, AgentMetadata metadata) {
        cache.put(agentKey, metadata);
        log.info("[CACHE] Agent 메타데이터 업데이트 - agentKey: {}, hostTotalMemory: {} bytes, hostCpuCores: {}, hostTotalDiskSpace: {} bytes",
                agentKey, metadata.getHostTotalMemory(), metadata.getHostCpuCores(), metadata.getHostTotalDiskSpace());
    }

    /**
     * Agent 메타데이터 조회
     * @param agentKey Agent 식별 키
     * @return AgentMetadata (없으면 null)
     */
    public AgentMetadata getMetadata(String agentKey) {
        return cache.get(agentKey);
    }

    /**
     * Host 전체 메모리 조회
     * @param agentKey Agent 식별 키
     * @return hostTotalMemory (없으면 null)
     */
    public Long getHostTotalMemory(String agentKey) {
        AgentMetadata metadata = cache.get(agentKey);
        return metadata != null ? metadata.getHostTotalMemory() : null;
    }

    /**
     * Host CPU 코어 수 조회
     * @param agentKey Agent 식별 키
     * @return hostCpuCores (없으면 null)
     */
    public Integer getHostCpuCores(String agentKey) {
        AgentMetadata metadata = cache.get(agentKey);
        return metadata != null ? metadata.getHostCpuCores() : null;
    }

    /**
     * Host 전체 디스크 공간 조회
     * @param agentKey Agent 식별 키
     * @return hostTotalDiskSpace (없으면 null)
     */
    public Long getHostTotalDiskSpace(String agentKey) {
        AgentMetadata metadata = cache.get(agentKey);
        return metadata != null ? metadata.getHostTotalDiskSpace() : null;
    }

    /**
     * Agent 메타데이터 삭제 (Agent 연결 해제 시)
     * @param agentKey Agent 식별 키
     */
    public void removeMetadata(String agentKey) {
        AgentMetadata removed = cache.remove(agentKey);
        if (removed != null) {
            log.info("[CACHE] Agent 메타데이터 삭제 - agentKey: {}", agentKey);
        }
    }

    /**
     * 전체 캐시 크기
     * @return 캐시에 저장된 Agent 수
     */
    public int size() {
        return cache.size();
    }

    /**
     * 캐시 존재 여부 확인
     * @param agentKey Agent 식별 키
     * @return 존재 여부
     */
    public boolean exists(String agentKey) {
        return cache.containsKey(agentKey);
    }

    /**
     * 전체 캐시 초기화 (테스트용)
     */
    public void clear() {
        cache.clear();
        log.warn("[CACHE] 전체 Agent 메타데이터 캐시 초기화");
    }
}