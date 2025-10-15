package com.monito.domains.member.service;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberCreateRequestDTO;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import com.monito.domains.member.repository.MemberRepository;
import com.monito.global.exception.BadRequestException;
import com.monito.global.exception.ExceptionMessage;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class MemberServiceImpl implements MemberService{

    private final MemberRepository memberRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Override
    public Member createMember(MemberCreateRequestDTO memberCreateRequestDTO) {
        // account_id 중복 체크
        if (memberRepository.existsByUsername(memberCreateRequestDTO.getUsername())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_ACCOUNT_ID);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(memberCreateRequestDTO.getPassword());

        return memberRepository.save(memberCreateRequestDTO.toEntity(encodedPassword));
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public void updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO) {
        // 사용자 존재 확인
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

        member.updateInfo(memberUpdateRequestDTO, passwordEncoder);
    }

    @Override
    public void deleteMember(Long id) {
        Member member = memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

        member.markAsDeleted();
    }
}
