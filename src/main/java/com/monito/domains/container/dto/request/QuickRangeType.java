/**
 * Quick Range 시간 옵션
 * - 사용자의 현재 시간 기준으로 과거 데이터 조회
 */
package com.monito.domains.container.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 작성자: 백승준
 */
@Getter
@RequiredArgsConstructor
public enum QuickRangeType {
    LAST_1_MINUTES(1, "최근 1분"),
    LAST_5_MINUTES(5, "최근 5분"),
    LAST_10_MINUTES(10, "최근 10분"),
    LAST_30_MINUTES(30, "최근 30분"),
    LAST_1_HOUR(60, "최근 1시간"),
    LAST_3_HOURS(180, "최근 3시간"),
    LAST_6_HOURS(360, "최근 6시간"),
    LAST_12_HOURS(720, "최근 12시간"),
    LAST_24_HOURS(1440, "최근 24시간");

    private final int minutes;
    private final String description;
}