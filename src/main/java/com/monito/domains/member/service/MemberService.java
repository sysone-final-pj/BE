package com.monito.domains.member.service;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberCreateRequestDTO;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import java.util.List;
/**
 작성자: 백승준
 */
public interface MemberService {
    Member createMember(MemberCreateRequestDTO memberCreateRequestDTO);
    Member getMemberById(Long id);
    List<Member> getAllMembers(String keyword);
    void updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO);
    void deleteMember(Long id);
    void existsByUsername(String username);
}
