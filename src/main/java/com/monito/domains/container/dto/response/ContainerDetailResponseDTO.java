package com.monito.domains.container.dto.response;

import com.monito.domains.container.dto.response.metrics.ContainerInfoDTO;
import com.monito.domains.container.dto.response.metrics.CpuMetricsDTO;
import com.monito.domains.container.dto.response.metrics.MemoryMetricsDTO;
import com.monito.domains.container.dto.response.metrics.NetworkMetricsDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 컨테이너 상세 정보 응답 DTO
 * - CPU, Memory, Network 메트릭을 한번에 반환
 * - 로그는 별도 API로 제공
 */
@Getter
@Builder
@AllArgsConstructor
public class ContainerDetailResponseDTO {
    private ContainerInfoDTO container;
    private CpuMetricsDTO cpu;
    private MemoryMetricsDTO memory;
    private NetworkMetricsDTO network;

    // 조회 시간 정보
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer dataPoints;                           // 데이터 포인트 개수
}
