package com.monito.domains.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 * 즐겨찾기 정보를 포함한 컨테이너 DTO
 * - 모든 컨테이너 목록을 즐겨찾기 우선으로 정렬할 때 사용
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerWithFavoriteDTO {

    private ContainerDashboardResponseDTO container;
    private Boolean isFavorite;

}