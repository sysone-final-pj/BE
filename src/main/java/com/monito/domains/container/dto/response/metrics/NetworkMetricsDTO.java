package com.monito.domains.container.dto.response.metrics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Network 메트릭 데이터
 */
@Getter
@Builder
@AllArgsConstructor
public class NetworkMetricsDTO {
    // 시계열 데이터 (차트용)
    private List<TimeSeriesDataDTO> rxBytesPerSec;        // 수신 속도 (bytes/sec)
    private List<TimeSeriesDataDTO> txBytesPerSec;        // 송신 속도 (bytes/sec)
    private List<TimeSeriesDataDTO> rxPacketsPerSec;      // 수신 패킷 속도 (packets/sec)
    private List<TimeSeriesDataDTO> txPacketsPerSec;      // 송신 패킷 속도 (packets/sec)

    // 현재 값
    private Long currentRxBytesPerSec;                    // 현재 수신 속도
    private Long currentTxBytesPerSec;                    // 현재 송신 속도

    // 누적 통계
    private Long totalRxBytes;                            // 총 수신 바이트
    private Long totalTxBytes;                            // 총 송신 바이트
    private Long totalRxPackets;                          // 총 수신 패킷
    private Long totalTxPackets;                          // 총 송신 패킷
    private Long networkTotalBytes;                       // 총 네트워크 바이트 (rx + tx)

    // 에러 및 드롭 정보
    private Integer rxErrors;                             // 수신 에러 수
    private Integer txErrors;                             // 송신 에러 수
    private Integer rxDropped;                            // 수신 드롭 수
    private Integer txDropped;                            // 송신 드롭 수
    private BigDecimal rxFailureRate;                     // 수신 실패율 (%)
    private BigDecimal txFailureRate;                     // 송신 실패율 (%)
}