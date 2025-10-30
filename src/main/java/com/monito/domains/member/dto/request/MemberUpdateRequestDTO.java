package com.monito.domains.member.dto.request;

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

    @Size(max = 50, message = "이름은 50자 이하여야 합니다")
    private String name;

    @Size(max = 100, message = "회사 명은 100자 이하여야 합니다")
    private String companyName;

    @Size(max = 25, message = "직함은 25자 이하여야 합니다")
    private String position;

    @Size(max = 20, message = "전화번호는 20자 이하여야 합니다")
    private String mobileNumber;

    @Size(max = 20, message = "사무실 전화번호는 20자 이하여야 합니다")
    private String officePhone;

    @Size(max = 255, message = "기타 사항은 255자 이하여야 합니다")
    private String note;

    @Size(max = 10, message = "역할은 10자 이하여야 합니다")
    private String role;
}
