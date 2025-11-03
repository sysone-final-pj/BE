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
    EXITED("종료"),
    PAUSED("일시정지"),
    DELETED("삭제됨"),
    UNKNOWN("확인 불가"); // 마지막 수집 시간이 오래 되었을 때 (30초 기준)

    private final String description;
}
