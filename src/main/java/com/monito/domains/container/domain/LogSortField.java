package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨테이너 로그 정렬 필드 Enum
 * - 프론트엔드에서 로그 정렬 기준으로 사용할 수 있는 필드 정의
 * - DB 컬럼명 직접 노출 방지 및 타입 안전성 보장
 */
@Getter
@RequiredArgsConstructor
public enum LogSortField {
    LOGGED_AT("loggedAt", "로그 수집 시간"),
    CONTAINER_NAME("containerName", "컨테이너 이름"),
    AGENT_NAME("agentName", "Agent 이름"),
    LOG_MESSAGE("logMessage", "로그 메시지");

    private final String fieldName;  // 필드명
    private final String description;  // 설명
}