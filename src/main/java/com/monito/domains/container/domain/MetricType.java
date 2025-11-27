package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 작성자: 백승준
 */
@Getter
@RequiredArgsConstructor
public enum MetricType {
    CPU("CPU"),
    MEMORY("MEMORY"),
    NETWORK("NETWORK");

    private final String description;
}
