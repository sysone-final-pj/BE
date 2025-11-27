/**
 * 알림 규칙 생성 요청 DTO
 * - 컨테이너별 메트릭 임계값 및 알림 규칙 설정 시 사용
 * - 임계값은 일부만 입력 가능 (최소 1개 이상 필수)
 */
package com.monito.domains.alert.dto.request;

import com.monito.domains.alert.validator.ValidThresholds;
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
 작성자: 이지민
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidThresholds
public class AlertRuleCreateRequestDTO {

    @NotBlank(message = "규칙 이름은 필수입니다.")
    private String ruleName;

    @NotNull(message = "메트릭 타입은 필수입니다.")
    private MetricType metricType;

    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal infoThreshold;

    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal warningThreshold;

    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal highThreshold;

    @DecimalMin(value = "0.00", message = "임계값은 0 이상이어야 합니다.")
    @DecimalMax(value = "100.00", message = "임계값은 100 이하여야 합니다.")
    private BigDecimal criticalThreshold;

    @NotNull(message = "쿨다운 시간은 필수입니다.")
    private Integer cooldownSeconds;
}
