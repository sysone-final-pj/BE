package com.monito.domains.container.dto.request;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * Agent가 보내는 CONTAINER_STATE_CHANGE 메시지
 * 컨테이너 상태 변경(생성/종료/삭제) 시 전송
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerStateChangeRequestDTO {
    private List<ContainerSnapshotRequestDTO> containers;
}