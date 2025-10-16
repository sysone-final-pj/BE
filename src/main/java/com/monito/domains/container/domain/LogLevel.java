package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LogLevel {
    DEBUG("디버그"),
    INFO("정보"),
    WARN("경고"),
    ERROR("에러"),
    FATAL("치명적");

    private final String description;
}