package com.monito.domains.alert.dto.request;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.container.domain.MetricType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "알림 필터 조건")
public class AlertFilterDTO {

    @Schema(description = "경고 레벨", example = "ERROR")
    private AlertLevel alertLevel;

    @Schema(description = "메트릭 타입", example = "CPU")
    private MetricType metricType;

    @Schema(description = "에이전트 이름", example = "production-server-01")
    private String agentName;

    @Schema(description = "컨테이너 이름", example = "nginx-container")
    private String containerName;

    @Schema(description = "수집 시작 시간", example = "2024-10-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime collectedAtFrom;

    @Schema(description = "수집 종료 시간", example = "2024-10-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime collectedAtTo;

    @Schema(description = "생성 시작 시간", example = "2024-10-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtFrom;

    @Schema(description = "생성 종료 시간", example = "2024-10-31T23:59:59")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime createdAtTo;

    @Schema(description = "읽음 여부", example = "false")
    private Boolean isRead;
}