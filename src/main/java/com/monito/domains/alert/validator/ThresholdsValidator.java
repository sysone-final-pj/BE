package com.monito.domains.alert.validator;

import com.monito.domains.alert.dto.request.AlertRuleCreateRequestDTO;
import com.monito.domains.alert.dto.request.AlertRuleUpdateRequestDTO;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 알림 규칙 임계값 검증 로직
 *
 * 검증 규칙:
 * 1. 최소 1개 이상의 임계값 필수 (Create 시)
 * 2. 모든 임계값은 0보다 커야 함
 * 3. 설정된 임계값들 간의 순서: 0 < info < warning < high < critical
 * 4. 설정되지 않은 임계값(null)은 무시
 */
public class ThresholdsValidator implements ConstraintValidator<ValidThresholds, Object> {

    private boolean allowAllNull;

    @Override
    public void initialize(ValidThresholds constraintAnnotation) {
        this.allowAllNull = constraintAnnotation.allowAllNull();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        BigDecimal info = null;
        BigDecimal warning = null;
        BigDecimal high = null;
        BigDecimal critical = null;

        // DTO 타입에 따라 임계값 추출
        if (value instanceof AlertRuleCreateRequestDTO dto) {
            info = dto.getInfoThreshold();
            warning = dto.getWarningThreshold();
            high = dto.getHighThreshold();
            critical = dto.getCriticalThreshold();
        } else if (value instanceof AlertRuleUpdateRequestDTO dto) {
            info = dto.getInfoThreshold();
            warning = dto.getWarningThreshold();
            high = dto.getHighThreshold();
            critical = dto.getCriticalThreshold();
        } else {
            return true; // 알 수 없는 타입은 통과
        }

        // 1. 최소 1개 이상 필수 검증 (allowAllNull이 false인 경우)
        if (!allowAllNull && info == null && warning == null && high == null && critical == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("최소 1개 이상의 임계값을 설정해야 합니다.")
                    .addConstraintViolation();
            return false;
        }

        // 2. 범위 검증: 모든 임계값은 0보다 커야 함
        List<String> errors = new ArrayList<>();

        if (info != null && info.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(String.format("INFO 임계값(%s)은 0보다 커야 합니다.", info));
        }
        if (warning != null && warning.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(String.format("WARNING 임계값(%s)은 0보다 커야 합니다.", warning));
        }
        if (high != null && high.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(String.format("HIGH 임계값(%s)은 0보다 커야 합니다.", high));
        }
        if (critical != null && critical.compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(String.format("CRITICAL 임계값(%s)은 0보다 커야 합니다.", critical));
        }

        // 3. 순서 검증: 설정된 임계값들 간의 순서가 올바른지 확인
        // info와 warning 비교
        if (info != null && warning != null && info.compareTo(warning) >= 0) {
            errors.add(String.format("INFO 임계값(%s)은 WARNING 임계값(%s)보다 작아야 합니다.", info, warning));
        }

        // warning과 high 비교
        if (warning != null && high != null && warning.compareTo(high) >= 0) {
            errors.add(String.format("WARNING 임계값(%s)은 HIGH 임계값(%s)보다 작아야 합니다.", warning, high));
        }

        // high와 critical 비교
        if (high != null && critical != null && high.compareTo(critical) >= 0) {
            errors.add(String.format("HIGH 임계값(%s)은 CRITICAL 임계값(%s)보다 작아야 합니다.", high, critical));
        }

        // info와 high 비교 (warning이 없는 경우)
        if (info != null && high != null && warning == null && info.compareTo(high) >= 0) {
            errors.add(String.format("INFO 임계값(%s)은 HIGH 임계값(%s)보다 작아야 합니다.", info, high));
        }

        // info와 critical 비교 (warning, high가 없는 경우)
        if (info != null && critical != null && warning == null && high == null && info.compareTo(critical) >= 0) {
            errors.add(String.format("INFO 임계값(%s)은 CRITICAL 임계값(%s)보다 작아야 합니다.", info, critical));
        }

        // warning과 critical 비교 (high가 없는 경우)
        if (warning != null && critical != null && high == null && warning.compareTo(critical) >= 0) {
            errors.add(String.format("WARNING 임계값(%s)은 CRITICAL 임계값(%s)보다 작아야 합니다.", warning, critical));
        }

        // 에러가 있으면 false 반환
        if (!errors.isEmpty()) {
            context.disableDefaultConstraintViolation();
            for (String error : errors) {
                context.buildConstraintViolationWithTemplate(error)
                        .addConstraintViolation();
            }
            return false;
        }

        return true;
    }
}