package com.monito.domains.history.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
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
    @Schema(
            description = "조회 시작 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)",
            example = "2024-01-01T00:00:00",
            required = true,
            type = "string",
            format = "date-time"
    )
    @NotNull(message = "시작 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime startTime;

    /**
     * 조회 종료 시간 (연월일 시분초 포함)
     */
    @Schema(
            description = "조회 종료 시간 (yyyy-MM-dd'T'HH:mm:ss 형식)",
            example = "2024-01-31T23:59:59",
            required = true,
            type = "string",
            format = "date-time"
    )
    @NotNull(message = "종료 시간은 필수입니다.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime endTime;

    /**
     * 컨테이너 ID (선택)
     */
    @Schema(description = "컨테이너 ID (선택, 특정 컨테이너만 조회)", example = "123")
    private Long containerId;

    /**
     * 삭제된 컨테이너 포함 여부
     * null: 전체 조회
     * 0: 활성 컨테이너만
     * 1: 삭제된 컨테이너만
     */
    @Schema(description = "삭제 여부 필터 (0: 활성, 1: 삭제됨, null: 전체)", example = "0")
    private Integer isDeleted;

    /**
     * 페이지 번호 (0부터 시작)
     */
    @Schema(description = "페이지 번호 (0부터 시작)", example = "0", defaultValue = "0")
    private Integer page = 0;

    /**
     * 페이지 크기
     */
    @Schema(description = "페이지 크기 (한 페이지당 데이터 개수)", example = "20", defaultValue = "20")
    private Integer size = 20;

    /**
     * 정렬 기준 (기본: collectedAt DESC)
     */
    @Schema(description = "정렬 기준 (엔티티 필드명)", example = "collectedAt", defaultValue = "collectedAt")
    private String sortBy = "collectedAt";

    /**
     * 정렬 방향 (ASC, DESC)
     */
    @Schema(description = "정렬 방향 (ASC: 오름차순, DESC: 내림차순)", example = "DESC", defaultValue = "DESC")
    private String sortDirection = "DESC";
}