package com.monito.domains.dashboard.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "네트워크 통계 데이터 포인트")
public class NetworkStatsDataPointDTO {

    @Schema(description = "수집 시간")
    private LocalDateTime timestamp;

    @Schema(description = "수신 속도 (bytes/sec)")
    private Long rxBytesPerSec;

    @Schema(description = "송신 속도 (bytes/sec)")
    private Long txBytesPerSec;
}