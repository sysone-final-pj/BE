package com.monito.domains.alert.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AlertLevel {
    CRITICAL("CRITICAL"),
    HIGH("HIGH"),
    WARNING("WARNING"),
    INFO("INFO");

    private final String description;
}