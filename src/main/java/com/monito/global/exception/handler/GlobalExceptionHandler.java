package com.monito.global.exception.handler;

import com.monito.global.exception.AuthenticationException;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ConflictException;
import com.monito.global.exception.ForbiddenException;
import com.monito.global.exception.NotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Client Error 4xx - Custom Exception 요청에 문제가 있는 경우
     */

    /**
     * @Valid 검증 실패 시 발생 (DTO 필드 검증)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleMethodArgumentNotValidException(final MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        String errorMessage = errors.entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                errorMessage
        );
        problemDetail.setTitle("입력값 검증 실패");
        problemDetail.setProperty("errors", errors);

        log.warn("Validation failed: {}", errors);
        return problemDetail;
    }

    /**
     * Custom Validator 검증 실패 시 발생 (@ValidThresholds 등)
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraintViolationException(final ConstraintViolationException e) {
        Map<String, String> errors = new HashMap<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        }

        String errorMessage = errors.values().stream()
                .collect(Collectors.joining(", "));

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                errorMessage
        );
        problemDetail.setTitle("입력값 검증 실패");
        problemDetail.setProperty("errors", errors);

        log.warn("Constraint violation: {}", errors);
        return problemDetail;
    }

    @ExceptionHandler(BadRequestException.class)
    ProblemDetail handleBadRequestException(final BadRequestException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problemDetail.setTitle("잘못된 요청입니다");

        return problemDetail;
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail handleAuthenticationException(final AuthenticationException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
        problemDetail.setTitle("인증 실패");

        return problemDetail;
    }

    @ExceptionHandler(ForbiddenException.class)
    ProblemDetail handleForbiddenException(final ForbiddenException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, e.getMessage());

        problemDetail.setTitle("접근 권한 없음");
        return problemDetail;
    }

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFoundException(final NotFoundException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());

        problemDetail.setTitle("데이터 없음");
        return problemDetail;
    }

    @ExceptionHandler(ConflictException.class)
    ProblemDetail handleConflictException(final ConflictException e) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());

        problemDetail.setTitle("데이터 충돌");
        return problemDetail;
    }

    /**
     * Internal Server Error 5xx :
     * 예외처리가 제대로 되지 않았거나 코드 자체의 문제인 경우일 확률 높음 코드를 고치거나 해당 예외처리 핸들러를 추가해줘야 함
     */
    @ExceptionHandler(Exception.class)
    ProblemDetail handleInternalError(final Exception e) {
        log.error("Uncaught {} - {}", e.getClass().getSimpleName(), e.getMessage());
        e.printStackTrace();
        ProblemDetail problemDetail = ProblemDetail
                .forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        return problemDetail;
    }


}


