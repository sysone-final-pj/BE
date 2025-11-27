package com.monito.global.security.userdetails;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.domain.Role;
import java.util.Collection;
import java.util.Collections;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
/**
 작성자: 백승준
 */
@Getter
public class CustomUserDetails implements UserDetails {

    private final Member member;

    public CustomUserDetails(Member user) {
        this.member = user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + member.getRole()));
    }

    @Override
    public String getPassword() {
        return member.getPassword();
    }

    @Override
    public String getUsername() {
        return member.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return member.getIsDeleted() == 0;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return member.getIsDeleted() == 0;
    }

    // Member 객체의 추가 정보에 접근할 수 있는 메서드들
    public Long getId() {
        return member.getId();
    }

    public String getEmail() {
        return member.getEmail();
    }


    public Role getRole() {
        return member.getRole();
    }
}
