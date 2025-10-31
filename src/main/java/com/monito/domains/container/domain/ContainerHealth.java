package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContainerHealth {
    HEALTHY("정상"),
    UNHEALTHY("비정상"),
    STARTING("HEALTH CHECK 시작"),
    NONE("HEALTH CHECK 설정 X"),
    UNKNOWN("정보를 불러올 수 없음");

    private final String description;
}
