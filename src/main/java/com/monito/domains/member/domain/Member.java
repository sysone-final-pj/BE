package com.monito.domains.member.domain;

import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import com.monito.global.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.security.crypto.password.PasswordEncoder;

@Entity
@Table(name = "members")
@SuperBuilder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "member_seq")
    @SequenceGenerator(
            name = "member_seq",
            sequenceName = "MEMBER_SEQ",
            allocationSize = 1
    )
    private Long id;


    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false, unique = false, length = 100)
    private String email;

    public void updateInfo(MemberUpdateRequestDTO dto, PasswordEncoder passwordEncoder){
        if(dto.getEmail() != null) this.email = dto.getEmail();
        if(dto.getPassword() != null) this.password = passwordEncoder.encode(dto.getPassword());
        if(dto.getRole() != null) this.role = Role.valueOf(dto.getRole().toUpperCase());
    }
}
