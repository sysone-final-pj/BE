package com.monito.domains.alert.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertLevel {
    CRITICAL("심각"),
    WARNING("경고"),
    INFO("보통");

    private final String description;
}
