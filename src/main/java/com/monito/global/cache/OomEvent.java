/**
 * OOM 이벤트 정보
 * - 컨테이너별 OOM 발생 시각 저장
 */
package com.monito.global.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor
public class OomEvent {

    /**
     * 컨테이너 ID
     */
    private Long containerId;

    /**
     * 컨테이너 해시
     */
    private String containerHash;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * OOM 발생 시각
     */
    private LocalDateTime occurredAt;

    /**
     * Agent 키 (옵션)
     */
    private String agentKey;
}