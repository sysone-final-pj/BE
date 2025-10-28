package com.monito.domains.agent.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Host 시스템 정보 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInfoDetailRequestDTO {
    /**
     * 전체 메모리 (bytes)
     */
    private Long totalMemory;

    /**
     * 사용 가능한 메모리 (bytes) - 선택
     */
    private Long availableMemory;

    /**
     * 사용 중인 메모리 (bytes) - 선택
     */
    private Long usedMemory;

    /**
     * CPU 코어 수 - 선택
     */
    private Integer cpuCores;

    /**
     * 호스트명 - 선택
     */
    private String hostname;

    /**
     * OS 타입 - 선택
     */
    private String osType;

    /**
     * 전체 디스크 공간 (bytes) - 선택
     */
    private Long totalDisk;
}