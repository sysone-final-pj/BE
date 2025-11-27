package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 작성자: 백승준
 */
@Getter
@RequiredArgsConstructor
public enum LogSource {
    STDOUT("표준 출력"),
    STDERR("표준 에러"),
    RAW("원시 로그");

    private final String description;
}