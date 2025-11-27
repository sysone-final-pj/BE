package com.monito.domains.member.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
/**
 작성자: 백승준
 */
@Getter
@RequiredArgsConstructor
public enum Role {
    USER("일반 사용자"),
    ADMIN("관리자");

    private final String description;
}
