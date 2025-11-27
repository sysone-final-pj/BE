package com.monito.domains.history.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContainerHistoryPageResponse {

    /**
     * 히스토리 데이터 리스트
     */
    private List<ContainerHistoryResponse> content;

    /**
     * 현재 페이지 번호 (0부터 시작)
     */
    private int pageNumber;

    /**
     * 페이지 크기
     */
    private int pageSize;

    /**
     * 전체 데이터 수
     */
    private long totalElements;

    /**
     * 전체 페이지 수
     */
    private int totalPages;

    /**
     * 첫 페이지 여부
     */
    private boolean first;

    /**
     * 마지막 페이지 여부
     */
    private boolean last;

    /**
     * 다음 페이지 존재 여부
     */
    private boolean hasNext;

    /**
     * 이전 페이지 존재 여부
     */
    private boolean hasPrevious;
}