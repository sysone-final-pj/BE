package com.monito.domains.dashboard.dto.response;

import com.monito.domains.agent.domain.Agent;
import com.monito.domains.container.domain.Container;
import com.monito.domains.container.domain.ContainerStatsLog;
import com.monito.domains.container.domain.LogSource;
import com.monito.domains.container.repository.ContainerLogRepository;
import com.monito.domains.dashboard.repository.DashboardRepository;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 대시보드 컨테이너 상세 정보 응답 DTO (플랫 구조)
 * - 컨테이너 카드 클릭 시 상세 정보 표시용
 * - WebSocket 실시간 업데이트용 (/topic/containers/{id}/metrics)
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "대시보드 컨테이너 상세 정보 응답 DTO")
public class DashboardContainerDetailDTO {

    // ========== 기본 정보 ==========
    @Schema(description = "Agent 이름")
    private String agentName;

    @Schema(description = "컨테이너 이름")
    private String containerName;

    @Schema(description = "컨테이너 해시")
    private String containerHash;

    // ========== CPU 메트릭 ==========
    @Schema(description = "CPU 사용률 (%)")
    private BigDecimal cpuPercent;

    @Schema(description = "CPU 사용량 (cores) = (cpuPercent / 100) * cpuLimitCores")
    private BigDecimal cpuUsage;

    @Schema(description = "CPU 제한 (cores)")
    private BigDecimal cpuLimitCores;

    // ========== Memory 메트릭 ==========
    @Schema(description = "메모리 사용량 (bytes)")
    private Long memUsage;

    @Schema(description = "메모리 제한 (bytes), isMemoryUnlimited=true면 null")
    private Long memLimit;

    // ========== State 및 Health ==========
    @Schema(description = "컨테이너 상태 (RUNNING, STOPPED 등)")
    private String state;

    @Schema(description = "컨테이너 실행 정보")
    private String status;

    @Schema(description = "헬스 상태")
    private String health;

    // ========== Network 메트릭 ==========
    @Schema(description = "네트워크 송신 속도 (bytes/sec)")
    private Long txBytesPerSec;

    @Schema(description = "네트워크 수신 속도 (bytes/sec)")
    private Long rxBytesPerSec;

    // ========== Image 정보 ==========
    @Schema(description = "이미지 레포지토리 (imageName을 ':'로 split한 [0])")
    private String repository;

    @Schema(description = "이미지 태그 (imageName을 ':'로 split한 [1])")
    private String tag;

    @Schema(description = "이미지 이름 (원본)")
    private String imageName;

    @Schema(description = "이미지 크기 (bytes)")
    private Long imageSize;

    // ========== Block I/O 메트릭 ==========
    @Schema(description = "블록 읽기 (누적, bytes)")
    private Long blkRead;

    @Schema(description = "블록 쓰기 (누적, bytes)")
    private Long blkWrite;

    // ========== Logs (당일 0시 기준 집계) ==========
    @Schema(description = "STDOUT 로그 개수 (당일)")
    private Long stdoutCount;

    @Schema(description = "STDERR 로그 개수 (당일)")
    private Long stderrCount;

    // ========== Storage ==========
    @Schema(description = "스토리지 할당량 (bytes), 0이면 무제한")
    private Long storageLimit;

    @Schema(description = "현재 스토리지 사용량 (bytes)")
    private Long storageUsed;

    /**
     * 실시간 WebSocket 발행용 (로그 카운트 제외)
     * - Container, Agent, StatsLog로부터 필드 추출
     * - stdoutCount, stderrCount, storageUsed는 null (별도 조회 필요)
     */
    public static DashboardContainerDetailDTO forRealtimeUpdate(
            Container container,
            Agent agent,
            ContainerStatsLog statsLog
    ) {
        // CPU Usage 계산: (cpuPercent / 100) * cpuLimitCores
        BigDecimal cpuUsage = null;
        if (statsLog.getCpuPercent() != null && container.getCpuLimitCores() != null) {
            cpuUsage = statsLog.getCpuPercent()
                    .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                    .multiply(container.getCpuLimitCores())
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Memory Limit: isMemoryUnlimited=true면 null 반환
        Long memLimit = container.getIsMemoryUnlimited() ? null : container.getMemLimit();

        // Image Name 파싱: repository와 tag 분리
        String repository = null;
        String tag = null;
        if (container.getImageName() != null && container.getImageName().contains(":")) {
            String[] parts = container.getImageName().split(":", 2);
            repository = parts[0];
            tag = parts.length > 1 ? parts[1] : null;
        } else {
            repository = container.getImageName();
            tag = "latest"; // 기본값
        }

        return DashboardContainerDetailDTO.builder()
                // 기본 정보
                .agentName(agent.getAgentName())
                .containerName(container.getName())
                .containerHash(container.getContainerHash())
                // CPU
                .cpuPercent(statsLog.getCpuPercent())
                .cpuUsage(cpuUsage)
                .cpuLimitCores(container.getCpuLimitCores())
                // Memory
                .memUsage(statsLog.getMemUsage())
                .memLimit(memLimit)
                // State
                .state(statsLog.getState().name())
                .health(statsLog.getHealth().name())
                // Network
                .txBytesPerSec(statsLog.getTxBytesPerSec())
                .rxBytesPerSec(statsLog.getRxBytesPerSec())
                // Image
                .repository(repository)
                .tag(tag)
                .imageName(container.getImageName())
                .imageSize(container.getImageSize())
                // Block I/O
                .blkRead(statsLog.getBlkRead())
                .blkWrite(statsLog.getBlkWrite())
                // Logs (TODO: 캐싱 또는 주기적 업데이트 필요)
                .stdoutCount(null)
                .stderrCount(null)
                // Storage (TODO: 캐싱 또는 주기적 업데이트 필요)
                .storageLimit(container.getStorageLimit())
                .storageUsed(null)
                .build();
    }

    /**
     * 로그 카운트 및 스토리지 사용량 포함 버전
     * - Repository를 통해 별도 조회 후 설정
     *
     * @param containerLogRepository 로그 카운트 조회용
     * @param dashboardRepository 스토리지 사용량 조회용
     */
    public static DashboardContainerDetailDTO forRealtimeUpdateWithMetrics(
            Container container,
            Agent agent,
            ContainerStatsLog statsLog,
            ContainerLogRepository containerLogRepository,
            DashboardRepository dashboardRepository
    ) {
        // 기본 DTO 생성
        DashboardContainerDetailDTO dto = forRealtimeUpdate(container, agent, statsLog);

        // 당일 0시 계산
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = startOfDay.plusDays(1);

        // STDOUT 로그 개수 조회
        long stdoutCount = containerLogRepository.countByContainerIdAndSourceAndLoggedAtBetween(
                container.getId(),
                LogSource.STDOUT,
                startOfDay,
                startOfNextDay
        );

        // STDERR 로그 개수 조회
        long stderrCount = containerLogRepository.countByContainerIdAndSourceAndLoggedAtBetween(
                container.getId(),
                LogSource.STDERR,
                startOfDay,
                startOfNextDay
        );

        // 스토리지 사용량 조회
        Long storageUsed = dashboardRepository.findStorageUsedByContainerId(container.getId());

        return DashboardContainerDetailDTO.builder()
                .agentName(dto.agentName)
                .containerName(dto.containerName)
                .containerHash(dto.containerHash)
                .cpuPercent(dto.cpuPercent)
                .cpuUsage(dto.cpuUsage)
                .cpuLimitCores(dto.cpuLimitCores)
                .memUsage(dto.memUsage)
                .memLimit(dto.memLimit)
                .state(dto.state)
                .health(dto.health)
                .txBytesPerSec(dto.txBytesPerSec)
                .rxBytesPerSec(dto.rxBytesPerSec)
                .repository(dto.repository)
                .tag(dto.tag)
                .imageName(dto.imageName)
                .imageSize(dto.imageSize)
                .blkRead(dto.blkRead)
                .blkWrite(dto.blkWrite)
                .stdoutCount(stdoutCount)
                .stderrCount(stderrCount)
                .storageLimit(dto.storageLimit)
                .storageUsed(storageUsed)
                .build();
    }
}