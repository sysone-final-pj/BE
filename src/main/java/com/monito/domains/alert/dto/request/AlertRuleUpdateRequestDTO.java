/**
 * 알림 규칙 수정 요청 DTO
 * - 기존 알림 규칙의 임계값, 쿨다운, 활성화 상태 등을 수정할 때 사용
 * - 임계값은 일부만 입력 가능
 */
package com.monito.domains.alert.dto.request;

import com.monito.domains.alert.validator.ValidThresholds;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
@ValidThresholds(allowAllNull = true)
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

    @DecimalMin(value = "5.00", message = "쿨다운은 5 이상이어야 합니다.")
    private Integer cooldownSeconds;

    private Boolean isEnabled;
}
