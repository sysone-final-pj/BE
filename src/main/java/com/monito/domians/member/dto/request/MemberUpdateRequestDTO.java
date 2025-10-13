package com.monito.domians.member.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MemberUpdateRequestDTO {
    @Size(max = 255, message = "이메일은 255자 이하여야 합니다")
    @Email(message = "유효한 이메일 형식이어야 합니다")
    private String email;

    @Size(min = 6, max = 255, message = "비밀번호는 6자 이상 255자 이하여야 합니다")
    private String password;

    @Size(max = 10, message = "역할은 10자 이하여야 합니다")
    private String role;
}
