package com.monito.domains.container.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 컨테이너 정렬 필드 Enum
 * - 프론트엔드에서 정렬 기준으로 사용할 수 있는 필드 정의
 * - DB 컬럼명 직접 노출 방지 및 타입 안전성 보장
 */
@Getter
@RequiredArgsConstructor
public enum ContainerSortField {
    // 기본 정보
    AGENT_NAME("agentName", "Agent 이름"),
    CONTAINER_HASH("containerHash", "컨테이너 해시"),
    CONTAINER_NAME("containerName", "컨테이너 이름"),

    // CPU
    CPU_PERCENT("cpuPercent", "CPU 사용률 (%)"),

    // Memory
    MEM_USAGE("memUsage", "메모리 사용량"),
    MEM_LIMIT("memLimit", "메모리 제한"),

    // Storage
    STORAGE_USAGE("sizeRootFs", "스토리지 사용량"),
    STORAGE_LIMIT("storageLimit", "스토리지 제한"),

    // Network
    RX_BYTES("rxBytesPerSec", "네트워크 수신 (bytes/sec)"),
    TX_BYTES("txBytesPerSec", "네트워크 송신 (bytes/sec)"),

    // Status
    STATE("state", "컨테이너 상태"),
    HEALTH("health", "헬스 상태");

    private final String fieldName;  // DTO 필드명
    private final String description;  // 설명
}