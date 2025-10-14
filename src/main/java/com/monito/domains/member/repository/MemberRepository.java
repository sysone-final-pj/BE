package com.monito.domains.member.repository;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface MemberRepository {
    Long save(Member member);
    Optional<Member> findByUsername(String username);
    Optional<Member> findById(Long id);
    List<Member> findAll();
    boolean existsByUsername(String username);
    int updateMember(@Param("id") Long id, @Param("dto") MemberUpdateRequestDTO memberUpdateRequestDTO);
    int deleteMember(Long id);
}
