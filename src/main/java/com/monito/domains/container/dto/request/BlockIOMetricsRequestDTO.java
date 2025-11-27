/**
 * Block I/O 메트릭 (Agent가 보내는 구조)
 */
package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockIOMetricsRequestDTO {
    private Long blkRead;
    private Long blkWrite;
}