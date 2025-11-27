package com.monito.domains.member.controller;

import com.monito.domains.member.domain.Member;
import com.monito.domains.member.dto.request.MemberCreateRequestDTO;
import com.monito.domains.member.dto.request.MemberUpdateRequestDTO;
import com.monito.domains.member.service.MemberService;
import com.monito.global.common.response.ApiResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
/**
 작성자: 백승준
 */
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    /**
     * 사용자 등록
     */
    @PreAuthorize("hasRole('ADMIN')")
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
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ApiResponse<List<Member>> getAllMembers(@RequestParam(required = false) String keyword) {
        List<Member> members = memberService.getAllMembers(keyword);
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

    /**
     * 사용자 username 찾기
     */
    @GetMapping("/check-username")
    public ApiResponse<Void> validateUsername(@RequestParam String username) {
        memberService.existsByUsername(username);
        return ApiResponse.ok("사용 가능한 아이디입니다.");
    }
}
