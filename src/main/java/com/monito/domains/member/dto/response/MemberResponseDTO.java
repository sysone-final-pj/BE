package com.monito.domains.member.dto.response;

import com.monito.domains.member.domain.Role;
import java.sql.Timestamp;
import lombok.Data;

@Data
public class MemberResponseDTO {
    private Long id;
    private String username;
    private Role role;
    private String email;
    private Timestamp createdAt;
    private Timestamp updatedAt;
}
