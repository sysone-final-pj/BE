package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MetricType {
    CPU("CPU"),
    RAM("RAM"),
    NETWORK("NETWORK");

    private final String description;
}
