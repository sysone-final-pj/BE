package com.monito.domains.alert.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRuleUpdateRequestDTO {

    private String ruleName;

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

    private Integer cooldownSeconds;

    private Integer checkInterval;

    private Boolean isEnabled;
}
