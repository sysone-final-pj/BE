/**
 * 알림 규칙 임계값 검증 Annotation
 * - 최소 1개 이상의 임계값 필수
 * - 설정된 임계값들의 순서 검증 (info < warning < high < critical)
 */
package com.monito.domains.alert.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 작성자: 이지민
 */
@Documented
@Constraint(validatedBy = ThresholdsValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidThresholds {

    String message() default "임계값 설정이 올바르지 않습니다.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    /**
     * Update 시 모든 임계값이 null인 것을 허용할지 여부
     * (기존 값을 유지하는 경우)
     */
    boolean allowAllNull() default false;
}