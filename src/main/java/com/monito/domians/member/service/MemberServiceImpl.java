package com.monito.domians.member.service;

import com.monito.domians.member.domain.Member;
import com.monito.domians.member.dto.request.MemberCreateRequestDTO;
import com.monito.domians.member.dto.request.MemberUpdateRequestDTO;
import com.monito.domians.member.repository.MemberRepository;
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
    public void createMember(MemberCreateRequestDTO memberCreateRequestDTO) {
        // account_id 중복 체크
        if (memberRepository.existsByUsername(memberCreateRequestDTO.getUsername())) {
            throw new BadRequestException(ExceptionMessage.DUPLICATE_ACCOUNT_ID);
        }

        // 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(memberCreateRequestDTO.getPassword());
        memberCreateRequestDTO.setPassword(encodedPassword);

        log.info("Role to be inserted: {}", memberCreateRequestDTO.getRole().name());

        int result = memberRepository.insertMember(memberCreateRequestDTO);
        if (result == 0) {
            throw new BadRequestException(ExceptionMessage.MEMBER_CREATE_FAILED);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Member getMemberById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));
    }

    @Override
    public List<Member> getAllMembers() {
        return memberRepository.findAll();
    }

    @Override
    public void updateMember(Long id, MemberUpdateRequestDTO memberUpdateRequestDTO) {
        // 사용자 존재 확인
        memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

        // 비밀번호가 있으면 암호화
        if (memberUpdateRequestDTO.getPassword() != null) {
            String encodedPassword = passwordEncoder.encode(memberUpdateRequestDTO.getPassword());
            memberUpdateRequestDTO.setPassword(encodedPassword);
        }

        int result = memberRepository.updateMember(id, memberUpdateRequestDTO);
        if (result == 0) {
            throw new BadRequestException(ExceptionMessage.MEMBER_UPDATE_FAILED);
        }
    }

    @Override
    public void deleteMember(Long id) {
        // 사용자 존재 확인
        memberRepository.findById(id)
                .orElseThrow(() -> new BadRequestException(ExceptionMessage.MEMBER_NOT_FOUND));

        int result = memberRepository.deleteMember(id);
        if (result == 0) {
            throw new BadRequestException(ExceptionMessage.MEMBER_DELETE_FAILED);
        }
    }
}
