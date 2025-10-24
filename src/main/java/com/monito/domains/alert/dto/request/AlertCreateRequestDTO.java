package com.monito.domains.alert.dto.request;

import com.monito.domains.alert.domain.AlertLevel;
import com.monito.domains.container.domain.MetricType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertCreateRequestDTO {

    @NotNull(message = "알림 규칙 ID는 필수입니다.")
    private Long ruleId;

    @NotNull(message = "컨테이너 ID는 필수입니다.")
    private Long containerId;

    @NotBlank(message = "알림 메시지는 필수입니다.")
    private String message;

    @NotNull(message = "메트릭 타입은 필수입니다.")
    private MetricType metricType;

    @NotNull(message = "메트릭 값은 필수입니다.")
    private BigDecimal metricValue;

    private AlertLevel alertLevel;
}