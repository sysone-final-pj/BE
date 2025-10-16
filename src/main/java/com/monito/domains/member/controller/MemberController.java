package com.monito.domains.member.controller;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberCreateRequestDTO;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import com.monito.domains.member.service.MemberService;
import com.monito.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 사용자 등록
     */
    @PostMapping
    public ApiResponse<Member> createMember(@Valid @RequestBody MemberCreateRequestDTO memberCreateRequestDTO) {
        return ApiResponse.created(memberService.createMember(memberCreateRequestDTO), "사용자가 성공적으로 등록되었습니다.");
    }

    /**
     * 사용자 조회 (ID로)
     */
    @GetMapping("/{id}")
    public ApiResponse<Member> getMemberById(@PathVariable Long id) {
        Member member = memberService.getMemberById(id);
        return ApiResponse.ok(member, "사용자 조회 성공");
    }

    /**
     * 모든 사용자 조회
     */
    @GetMapping
    public ApiResponse<List<Member>> getAllMembers() {
        List<Member> members = memberService.getAllMembers();
        return ApiResponse.ok(members, "사용자 목록 조회 성공");
    }

    /**
     * 사용자 수정
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateMember(
            @PathVariable Long id,
            @Valid @RequestBody MemberUpdateRequestDTO memberUpdateRequestDTO) {
        memberService.updateMember(id, memberUpdateRequestDTO);
        return ApiResponse.ok("사용자 정보가 성공적으로 수정되었습니다.");
    }

    /**
     * 사용자 삭제 (논리 삭제)
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteMember(@PathVariable Long id) {
        memberService.deleteMember(id);
        return ApiResponse.ok("사용자가 성공적으로 삭제되었습니다.");
    }
}
