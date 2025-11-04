package com.monito.domains.alert.dto.request;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.container.domain.MetricType;
import com.monito.domains.container.dto.request.QuickRangeType;
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

    @Schema(description = "경고 레벨", example = "CRITICAL")
    private AlertLevel alertLevel;

    @Schema(description = "메트릭 타입", example = "CPU")
    private MetricType metricType;

    @Schema(description = "에이전트 이름", example = "production-server-01")
    private String agentName;

    @Schema(description = "컨테이너 이름", example = "nginx-container")
    private String containerName;

    @Schema(description = "빠른 시간 범위 선택 (최근 5분, 10분 등). 이 값이 있으면 collectedAtFrom/To는 무시됨", example = "LAST_10_MINUTES")
    private QuickRangeType quickRangeType;

    @Schema(description = "수집 시작 시간 (quickRangeType이 없을 때 사용)", example = "2024-10-01T00:00:00")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime collectedAtFrom;

    @Schema(description = "수집 종료 시간 (quickRangeType이 없을 때 사용)", example = "2024-10-31T23:59:59")
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

    @Schema(description = "정렬 기준 (ALERT_LEVEL, METRIC_TYPE, CONTAINER_NAME, METRIC_VALUE, COLLECTED_AT, CREATED_AT)", example = "CREATED_AT")
    private AlertSortType sortType;
}