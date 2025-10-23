package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ContainerState {
    RUNNING("실행중"),
    RESTARTING("재실행"),
    DEAD("DEAD"),
    CREATED("생성됨"),
    EXIT("종료"),
    PAUSED("일시정지");

    private final String description;
}
