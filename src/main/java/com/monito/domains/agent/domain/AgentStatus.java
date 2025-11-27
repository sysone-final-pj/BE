package com.monito.domains.agent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 작성자: 백승준
 */
@Getter
@RequiredArgsConstructor
public enum AgentStatus {
    REGISTERED("등록됨"),
    CONNECTING("연결중"),
    AUTHENTICATING("인증중"),
    ONLINE("온라인"),
    OFFLINE("오프라인"),
    ERROR("에러");

    private final String description;
}
