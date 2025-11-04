package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.temporal.ChronoUnit;

@Getter
@RequiredArgsConstructor
public enum TimeBucket {
    MINUTES(ChronoUnit.MINUTES),
    HOURS(ChronoUnit.HOURS),
    DAYS(ChronoUnit.DAYS);

    private final ChronoUnit unit;
}