package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 개별 컨테이너 상태 스냅샷
 * Agent가 CONTAINER_STATE_CHANGE 메시지로 전송하는 컨테이너 기본 정보
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerSnapshotRequestDTO {
    private String containerHash;
    private String containerName;
    private String state;
    private String imageName;
    private Long imageSize;
}