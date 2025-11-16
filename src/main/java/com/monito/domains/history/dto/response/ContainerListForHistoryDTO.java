package com.monito.domains.history.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 히스토리 조회용 컨테이너 목록 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerListForHistoryDTO {

    /**
     * 컨테이너 ID
     */
    private Long id;

    /**
     * 컨테이너 이름
     */
    private String containerName;

    /**
     * 컨테이너 해시
     */
    private String containerHash;

    /**
     * 삭제 여부 (0: 활성, 1: 삭제됨)
     */
    private Integer isDeleted;
}