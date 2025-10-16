package com.monito.domains.alert.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertLevel {
    LOW("낮음"),
    MEDIUM("보통"),
    HIGH("높음");

    private final String description;
}
