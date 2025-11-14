package com.monito.domains.history.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerHistoryRequest {

    /**
     * 조회 시작 시간 (연월일 시분초 포함)
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 조회 종료 시간 (연월일 시분초 포함)
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 컨테이너 ID (선택)
     */
    private Long containerId;

    /**
     * 삭제된 컨테이너 포함 여부
     * null: 전체 조회
     * 0: 활성 컨테이너만
     * 1: 삭제된 컨테이너만
     */
    private Integer isDeleted;

    /**
     * 페이지 번호 (0부터 시작)
     */
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    private Integer size = 20;

    /**
     * 정렬 기준 (기본: collectedAt DESC)
     */
    private String sortBy = "collectedAt";

    /**
     * 정렬 방향 (ASC, DESC)
     */
    private String sortDirection = "DESC";
}