package com.monito.domains.alert.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Alert 정렬 타입
 */
@Getter
@RequiredArgsConstructor
public enum AlertSortType {
    ALERT_LEVEL("경고 레벨 순"),
    METRIC_TYPE("메트릭 타입 순"),
    CONTAINER_NAME("컨테이너 이름 순"),
    METRIC_VALUE("메트릭 값 순 (높은 순)"),
    COLLECTED_AT("수집 시간 순 (최신 순)");

    private final String description;
}