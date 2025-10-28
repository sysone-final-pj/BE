package com.monito.global.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Agent 메타데이터 (캐싱용)
 * - 자주 변하지 않는 Agent의 시스템 정보
 * - 메트릭 계산 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentMetadata {
    /**
     * Agent 식별 키
     */
    private String agentKey;

    /**
     * Host 전체 메모리 (bytes)
     */
    private Long hostTotalMemory;

    /**
     * Host CPU 코어 수
     */
    private Integer hostCpuCores;

    /**
     * Host 전체 디스크 공간 (bytes)
     */
    private Long hostTotalDiskSpace;

    /**
     * 호스트명 (선택)
     */
    private String hostname;

    /**
     * OS 타입 (선택)
     */
    private String osType;

    /**
     * 마지막 업데이트 시간
     */
    private Long lastUpdatedAt;
}