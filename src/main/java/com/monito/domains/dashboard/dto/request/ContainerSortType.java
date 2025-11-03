package com.monito.domains.dashboard.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨테이너 정렬 타입
 * - 대시보드 컨테이너 목록 정렬 옵션
 */
@Getter
@RequiredArgsConstructor
public enum ContainerSortType {
    NAME("containerName", "컨테이너 이름 순"),
    CPU_PERCENT("cpuPercent", "CPU 사용률 높은 순"),
    MEM_PERCENT("memPercent", "메모리 사용률 높은 순"),
    NETWORK_TOTAL_BYTES("networkTotalBytes", "네트워크 총 사용량 많은 순");

    private final String fieldName;
    private final String description;
}