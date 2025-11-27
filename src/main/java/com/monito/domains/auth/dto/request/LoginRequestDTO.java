package com.monito.domains.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
/**
 작성자: 백승준
 */
@Data
public class LoginRequestDTO {
    @NotBlank(message = "계정 ID는 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    private String password;
}
