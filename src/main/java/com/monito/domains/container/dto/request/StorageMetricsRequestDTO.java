package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * Storage 메트릭 (Agent가 보내는 구조)
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class StorageMetricsRequestDTO {
    private Long sizeRw;         // Read-Write Layer 크기 (bytes)
    private Long sizeRootFs;     // 전체 파일시스템 크기 (bytes)
    private Long imageSize;      // 이미지 크기 (bytes)
    private String imageName;    // 이미지 이름
}