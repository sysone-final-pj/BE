package com.monito.domains.alert.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 공동 작성자: 백승준, 이지민
 */
@Getter
@RequiredArgsConstructor
public enum AlertLevel {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    WARNING("WARNING"),
    INFO("INFO");

    private final String description;
}