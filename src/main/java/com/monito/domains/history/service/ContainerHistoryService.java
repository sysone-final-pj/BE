package com.monito.domains.history.service;

import com.monito.domains.history.dto.request.ContainerHistoryRequest;
import com.monito.domains.history.dto.response.ContainerHistoryPageResponse;
import com.monito.domains.history.dto.response.ContainerListForHistoryDTO;

import java.util.List;

public interface ContainerHistoryService {

    /**
     * 컨테이너 히스토리 조회 (페이지네이션)
     * @param request 조회 요청 (시간 범위, 컨테이너 ID, 삭제 여부, 페이지 정보)
     * @return 컨테이너 히스토리 페이지 응답
     */
    ContainerHistoryPageResponse getContainerHistory(ContainerHistoryRequest request);

    /**
     * 히스토리 조회용 컨테이너 목록 조회
     * @param isDeleted 삭제 여부 (0: 활성, 1: 삭제됨, null: 전체)
     * @return 컨테이너 기본 정보 리스트
     */
    List<ContainerListForHistoryDTO> getContainerListForHistory(Integer isDeleted);
}