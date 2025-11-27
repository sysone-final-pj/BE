/**
 * 컨테이너 차트 데이터 조회 요청 DTO
 * - ContainerHistoryResponse의 특정 필드를 시계열 데이터로 조회
 */
package com.monito.domains.history.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
/**
 작성자: 이지민
 */
@Schema(description = "컨테이너 차트 데이터 조회 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ContainerChartRequest {

    @Schema(description = "조회 시작 시간", example = "2024-01-01T00:00:00", required = true)
    private LocalDateTime startTime;

    @Schema(description = "조회 종료 시간", example = "2024-01-31T23:59:59", required = true)
    private LocalDateTime endTime;

    @Schema(description = "컨테이너 ID", example = "123", required = true)
    private Long containerId;

    @Schema(description = """
            조회할 메트릭 필드명 (ContainerHistoryResponse의 필드명)

            **CPU 메트릭:**
            - cpuPercent: CPU 사용률
            - cpuCoreUsage: CPU 코어 사용량
            - cpuUsageTotal: 전체 CPU 사용 시간

            **Memory 메트릭:**
            - memPercent: 메모리 사용률
            - memUsage: 메모리 사용량 (bytes)
            - memMaxUsage: 최대 메모리 사용량 (bytes)

            **Network 메트릭:**
            - rxBytes: 네트워크 수신 바이트
            - txBytes: 네트워크 송신 바이트
            - rxBytesPerSec: 초당 수신 바이트
            - txBytesPerSec: 초당 송신 바이트
            - networkTotalBytes: 전체 네트워크 사용량

            **Block I/O 메트릭:**
            - blkRead: 블록 읽기 바이트
            - blkWrite: 블록 쓰기 바이트
            - blkReadPerSec: 초당 블록 읽기
            - blkWritePerSec: 초당 블록 쓰기

            **Storage 메트릭:**
            - sizeRw: R/W 레이어 크기
            - sizeRootFs: 루트 파일시스템 크기
            """,
            example = "cpuPercent", required = true)
    private String metricField;
}