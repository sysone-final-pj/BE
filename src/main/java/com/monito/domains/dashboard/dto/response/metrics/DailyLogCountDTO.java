package com.monito.domains.dashboard.dto.response.metrics;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "당일 로그 카운트 응답 DTO")
public class DailyLogCountDTO {

    @Schema(description = "STDOUT 로그 개수")
    private Long stdoutCount;

    @Schema(description = "STDERR 로그 개수")
    private Long stderrCount;

    @Schema(description = "조회 기준일 (형식: YYYY-MM-DD)")
    private String date;
}