/**
 * 대시보드 컨테이너 상세 정보 응답 DTO (중첩 구조)
 * - 컨테이너 카드 클릭 시 상세 정보 표시용
 * - WebSocket 실시간 업데이트용 (/topic/dashboard/detail/{id})
 * - 최초 API 호출용 (/api/dashboard/containers/{id}/metrics)
 */
package com.monito.domains.dashboard.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.dashboard.dto.response.metrics.*;
import com.monito.domains.dashboard.repository.DashboardRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 컨테이너 상세 정보 응답 DTO")
public class DashboardContainerDetailDTO {

    @Schema(description = "컨테이너 기본 정보")
    private DashboardContainerInfoDTO container;

    @Schema(description = "CPU 메트릭")
    private DashboardCpuMetricsDTO cpu;

    @Schema(description = "메모리 메트릭")
    private DashboardMemoryMetricsDTO memory;

    @Schema(description = "네트워크 메트릭")
    private DashboardNetworkMetricsDTO network;

    @Schema(description = "Block I/O 메트릭")
    private DashboardBlockIOMetricsDTO blockIO;

    @Schema(description = "로그 메트릭 (당일 기준)")
    private DashboardLogsMetricsDTO logs;

    @Schema(description = "스토리지 메트릭")
    private DashboardStorageMetricsDTO storage;

    /**
     * 실시간 WebSocket 발행용 (로그 카운트 제외)
     * - Container, Agent, StatsLog로부터 필드 추출
     * - logs, storage는 null (별도 조회 필요)
     */
    public static DashboardContainerDetailDTO forRealtimeUpdate(
            Container container,
            Agent agent,
            ContainerStatsLog statsLog
    ) {
        return DashboardContainerDetailDTO.builder()
                .container(DashboardContainerInfoDTO.from(container, agent, statsLog))
                .cpu(DashboardCpuMetricsDTO.from(container, statsLog))
                .memory(DashboardMemoryMetricsDTO.from(container, statsLog))
                .network(DashboardNetworkMetricsDTO.from(statsLog))
                .blockIO(DashboardBlockIOMetricsDTO.from(statsLog))
                .logs(null)  // 별도 조회 필요
                .storage(null)  // 별도 조회 필요
                .build();
    }

    /**
     * 로그 카운트 및 스토리지 사용량 포함 버전 (최초 API 호출용)
     * - Repository를 통해 별도 조회 후 설정
     *
     * @param containerLogRepository 로그 카운트 조회용
     * @param dashboardRepository 스토리지 사용량 조회용
     * @param clientDate 클라이언트의 날짜 (해당 날짜의 0시부터 집계)
     */
    public static DashboardContainerDetailDTO forRealtimeUpdateWithMetrics(
            Container container,
            Agent agent,
            ContainerStatsLog statsLog,
            ContainerLogRepository containerLogRepository,
            DashboardRepository dashboardRepository,
            LocalDate clientDate
    ) {
        // 클라이언트 날짜의 0시 계산
        LocalDateTime startOfDay = clientDate.atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        // STDOUT 로그 개수 조회 (loggedAt 기준)
        long stdoutCount = containerLogRepository.countByContainerIdAndSourceAndLoggedAtBetween(
                container.getId(),
                LogSource.STDOUT,
                startOfDay,
                startOfNextDay
        );

        // STDERR 로그 개수 조회 (loggedAt 기준)
        long stderrCount = containerLogRepository.countByContainerIdAndSourceAndLoggedAtBetween(
                container.getId(),
                LogSource.STDERR,
                startOfDay,
                startOfNextDay
        );

        // STDOUT 로그 개수 조회 (createdAt 기준)
        long stdoutCountByCreatedAt = containerLogRepository.countByContainerIdAndSourceAndCreatedAtBetween(
                container.getId(),
                LogSource.STDOUT,
                startOfDay,
                startOfNextDay
        );

        // STDERR 로그 개수 조회 (createdAt 기준)
        long stderrCountByCreatedAt = containerLogRepository.countByContainerIdAndSourceAndCreatedAtBetween(
                container.getId(),
                LogSource.STDERR,
                startOfDay,
                startOfNextDay
        );

        // 스토리지 사용량 조회
        Long storageUsed = dashboardRepository.findStorageUsedByContainerId(container.getId());

        return DashboardContainerDetailDTO.builder()
                .container(DashboardContainerInfoDTO.from(container, agent, statsLog))
                .cpu(DashboardCpuMetricsDTO.from(container, statsLog))
                .memory(DashboardMemoryMetricsDTO.from(container, statsLog))
                .network(DashboardNetworkMetricsDTO.from(statsLog))
                .blockIO(DashboardBlockIOMetricsDTO.from(statsLog))
                .logs(DashboardLogsMetricsDTO.of(stdoutCount, stderrCount, stdoutCountByCreatedAt, stderrCountByCreatedAt))
                .storage(DashboardStorageMetricsDTO.of(container, storageUsed))
                .build();
    }
}