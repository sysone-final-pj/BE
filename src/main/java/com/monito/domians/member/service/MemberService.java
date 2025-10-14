package com.monito.domians.member.service;

import com.monito.domians.member.domain.Member;
import com.monito.domians.member.dto.request.MemberCreateRequestDTO;
import com.monito.domians.member.dto.request.MemberUpdateRequestDTO;
import java.util.List;

public interface MemberService {
    void createMember(MemberCreateRequestDTO memberCreateRequestDTO);
    Member getMemberById(Long id);
    List<Member> getAllMembers();
    void updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO);
    void deleteMember(Long id);
}
