package com.monito.domains.alert.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MessageType {
    CPU("cpu"),
    NETWORK("network"),
    RAM("ram");

    private final String description;
}