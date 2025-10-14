package com.monito.domains.member.domain;

import com.monito.global.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@Data
public class Member extends BaseEntity {
    private Long id;
    private String username;
    private String password;
    private Role role;
    private String email;
}
