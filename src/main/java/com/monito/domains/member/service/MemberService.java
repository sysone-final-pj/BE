package com.monito.domains.member.service;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberCreateRequestDTO;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import java.util.List;

public interface MemberService {
    Member createMember(MemberCreateRequestDTO memberCreateRequestDTO);
    Member getMemberById(Long id);
    List<Member> getAllMembers();
    void updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO);
    void deleteMember(Long id);
}
