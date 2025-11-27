package com.monito.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 작성자: 백승준
 */
@Data
public class RefreshTokenRequestDTO {
    @NotBlank(message = "리프레시 토큰은 필수입니다")
    private String refreshToken;
}
