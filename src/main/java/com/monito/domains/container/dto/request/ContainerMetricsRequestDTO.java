/**
 * Agent로부터 WebSocket으로 수신되는 컨테이너 메트릭 데이터
 */
package com.monito.domains.container.dto.request;

import com.monito.domains.container.domain.ContainerHealth;
import com.monito.domains.container.domain.ContainerState;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

/**
 작성자: 백승준
 */
@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContainerMetricsRequestDTO {
    private String containerHash;
    private String containerName;
    private ContainerState state;
    private ContainerHealth health;
    private LocalDateTime collectedAt;  // Agent에서 메트릭을 수집한 시간

    // CPU 관련
    private Long hostCpuUsageTotal;
    private Long cpuUsageTotal;
    private Long cpuUser;
    private Long cpuSystem;
    private Long cpuQuota;
    private Long cpuPeriod;
    private Integer onlineCpus;
    private Boolean isCpuUnlimited;  // CPU 제한이 없는지 여부
    private Long throttlingPeriods;
    private Long throttledPeriods;
    private Long throttledTime;

    // Memory 관련
    private Long memUsage;
    private Long memLimit;
    private Boolean isMemoryUnlimited;  // 메모리 제한이 없는지 여부

    // Block I/O 관련
    private Long blkRead;
    private Long blkWrite;

    // Network 관련
    private Long rxBytes;
    private Long txBytes;
    private Long rxPackets;
    private Long txPackets;
    private Integer rxErrors;
    private Integer txErrors;
    private Integer rxDropped;
    private Integer txDropped;

    // Storage 관련
    private Long sizeRw;         // Read-Write Layer 크기
    private Long sizeRootFs;     // 전체 파일시스템 크기 (imageSize + sizeRw)
    private Long storageLimit;   // 스토리지 할당량 (0 = 무제한)
    private Boolean isStorageUnlimited;  // 스토리지 제한이 없는지 여부
    private Long imageSize;      // 이미지 크기
    private String imageName;    // 이미지 이름
    private String imageId;      // 이미지 ID
}