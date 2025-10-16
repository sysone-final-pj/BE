package com.monito.domains.member.dto.response;

import com.monito.domains.member.domain.Role;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class MemberResponseDTO {
    private Long id;
    private String username;
    private Role role;
    private String email;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
