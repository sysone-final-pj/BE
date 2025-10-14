package com.monito.domians.auth.controller;

import com.monito.global.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class SecurityTestController {

    @GetMapping("/public")
    public ApiResponse<String> publicEndpoint() {
        return ApiResponse.ok("✅ 누구나 접근 가능 (public)");
    }

    @GetMapping("/user")
    public ApiResponse<String> userEndpoint() {
        return ApiResponse.ok("🔐 인증된 사용자 접근 성공 (user)");
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin")
    public ApiResponse<String> adminEndpoint() {
        return ApiResponse.ok("🛡️ 관리자 권한 접근 성공 (admin)");
    }

    @GetMapping("/me")
    public ApiResponse<String> me(@org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails user) {
        return ApiResponse.ok("👤 현재 사용자: " + user.getUsername());
    }

}
