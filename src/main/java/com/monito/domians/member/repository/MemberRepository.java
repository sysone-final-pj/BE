package com.monito.domians.member.repository;

import com.monito.domians.member.domain.Member;
import com.monito.domians.member.dto.request.MemberCreateRequestDTO;
import com.monito.domians.member.dto.request.MemberUpdateRequestDTO;
import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;

public interface MemberRepository {
    int insertMember(MemberCreateRequestDTO memberCreateRequestDTO);
    void save(Member member);
    Optional<Member> findByUsername(String username);
    Optional<Member> findById(Long id);
    List<Member> findAll();
    boolean existsByUsername(String username);
    int updateMember(@Param("id") Long id, @Param("dto") MemberUpdateRequestDTO memberUpdateRequestDTO);
    int deleteMember(Long id);
}
