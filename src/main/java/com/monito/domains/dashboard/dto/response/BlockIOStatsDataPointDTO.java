package com.monito.domains.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Block I/O 통계 데이터 포인트 DTO
 * 특정 시점의 읽기/쓰기 속도 데이터
 */
@Getter
@Builder
@AllArgsConstructor
public class BlockIOStatsDataPointDTO {
    /**
     * 데이터 수집 시각
     */
    private LocalDateTime timestamp;

    /**
     * 블록 읽기 속도 (bytes/sec)
     */
    private Long blkRead;

    /**
     * 블록 쓰기 속도 (bytes/sec)
     */
    private Long blkWrite;
}