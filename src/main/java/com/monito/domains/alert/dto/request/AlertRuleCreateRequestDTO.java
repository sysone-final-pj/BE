package com.monito.domains.alert.dto.request;

import com.monito.domains.container.domain.MetricType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 알림 규칙 생성 요청 DTO
 * - 컨테이너별 메트릭 임계값 및 알림 규칙 설정 시 사용
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleCreateRequestDTO {

    @NotNull(message = "컨테이너 ID는 필수입니다.")
    private Long containerId;

    @NotBlank(message = "규칙 이름은 필수입니다.")
    private String ruleName;

    @NotNull(message = "메트릭 타입은 필수입니다.")
    private MetricType metricType;

    @NotNull(message = "INFO 임계값은 필수입니다.")
    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal infoThreshold;

    @NotNull(message = "WARNING 임계값은 필수입니다.")
    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal warningThreshold;

    @NotNull(message = "HIGH 임계값은 필수입니다.")
    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal highThreshold;

    @NotNull(message = "CRITICAL 임계값은 필수입니다.")
    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal criticalThreshold;

    @NotNull(message = "쿨다운 시간은 필수입니다.")
    private Integer cooldownSeconds;
}
