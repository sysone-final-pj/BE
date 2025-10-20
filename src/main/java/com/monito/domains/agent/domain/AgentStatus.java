package com.monito.domains.agent.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AgentStatus {
    ONLINE("온라인"),
    OFFLINE("오프라인"),
    ERROR("에러"),
    CONNECTING("연결중");

    private final String description;
}
