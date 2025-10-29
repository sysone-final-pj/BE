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
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "members")
@SuperBuilder
@SQLRestriction("is_deleted = 0")
@Getter
@NoArgsConstructor
@ToString
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
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(length = 100)
    private String companyName;

    @Column(length = 25)
    private String position;

    @Column(length = 20)
    private String mobileNumber;

    @Column(length = 20)
    private String officePhone;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 255)
    private String note;

    public void updateInfo(String email, String password, String name, String companyName,
                           String position, String mobileNumber, String officePhone,
                           String note, String role) {
        if (email != null) this.email = email;
        if (password != null) this.password = password;
        if (name != null) this.name = name;
        if (companyName != null) this.companyName = companyName;
        if (position != null) this.position = position;
        if (mobileNumber != null) this.mobileNumber = mobileNumber;
        if (officePhone != null) this.officePhone = officePhone;
        if (note != null) this.note = note;
        if (role != null) this.role = Role.valueOf(role.toUpperCase());
    }
}
