/**
 * 컨테이너 정렬 타입
 * - 대시보드 컨테이너 목록 정렬 옵션
 */
package com.monito.domains.dashboard.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 작성자: 이지민
 */
@Getter
@RequiredArgsConstructor
public enum ContainerSortType {
    NAME("containerName", "컨테이너 이름 순"),
    CPU_PERCENT("cpuPercent", "CPU 사용률 높은 순"),
    MEM_PERCENT("memPercent", "메모리 사용률 높은 순"),
    FAVORITE("favorite", "즐겨찾기 우선 순");

    private final String fieldName;
    private final String description;
}