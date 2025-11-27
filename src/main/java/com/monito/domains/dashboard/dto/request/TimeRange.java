package com.monito.domains.dashboard.dto.request;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 작성자: 이지민
 */
@Getter
@RequiredArgsConstructor
public enum TimeRange {
    ONE_MINUTES(1, "1분"),
    THREE_MINUTES(3, "3분"),
    FIFTEEN_MINUTES(15, "15분"),
    THIRTY_MINUTES(30, "30분"),
    ONE_HOUR(60, "1시간");

    private final int minutes;
    private final String description;
}